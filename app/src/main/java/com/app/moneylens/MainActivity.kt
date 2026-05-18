package com.app.moneylens

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.app.moneylens.auth.SessionManager
import com.app.moneylens.camera.CameraManager
import com.app.moneylens.ml.TFLiteModelManager
import com.app.moneylens.tts.TextToSpeechManager
import com.app.moneylens.utils.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    // ─── Views ─────────────────────────────────────────────────────────────────
    private lateinit var previewView    : PreviewView
    private lateinit var flashButton    : Button
    private lateinit var statusText     : TextView
    private lateinit var confidenceText : TextView

    // ─── Managers ──────────────────────────────────────────────────────────────
    private var cameraManager  : CameraManager?        = null
    private var tfliteManager  : TFLiteModelManager?   = null
    private var ttsManager     : TextToSpeechManager?  = null
    private var sessionManager : SessionManager?       = null

    // ─── State ─────────────────────────────────────────────────────────────────
    private var isProcessing        = false
    private var lastAnnouncedLabel  = ""    // Label terakhir yang sudah di-TTS
    private var lastDetectionTime   = 0L

    // ─── Timing ────────────────────────────────────────────────────────────────
    private val DETECTION_DEBOUNCE_MS = 2000L   // Jeda minimum antar TTS (ms)

    // ─── Voting system ─────────────────────────────────────────────────────────
    private data class FramePrediction(
        val label      : String,
        val confidence : Float,
        val margin     : Float
    )

    private val predictionHistory = mutableListOf<FramePrediction>()
    private val MAX_HISTORY       = 5
    private val MIN_CONSENSUS     = 4   // 4 dari 5 frame harus sama

    // ───────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ───────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        previewView    = findViewById(R.id.previewView)
        flashButton    = findViewById(R.id.flashButton)
        statusText     = findViewById(R.id.statusText)
        confidenceText = findViewById(R.id.confidenceText)

        // Initialize SessionManager untuk update last active time
        sessionManager = SessionManager(this)
        sessionManager?.updateLastActive()

        if (PermissionManager.hasAllPermissions(this)) {
            initializeApp()
        } else {
            PermissionManager.requestPermissions(this)
        }

        flashButton.setOnClickListener {
            cameraManager?.let {
                val turningOn = !it.isTorchEnabled()
                it.toggleTorch(turningOn)
                val msg = if (turningOn) getString(R.string.flash_on)
                else          getString(R.string.flash_off)
                ttsManager?.speak(msg)
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager?.stopCamera()
        tfliteManager?.close()
        ttsManager?.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode : Int,
        permissions : Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeApp()
            } else {
                statusText.text = getString(R.string.camera_error)
                ttsManager?.speak("Izin kamera diperlukan untuk menggunakan aplikasi ini")
                Toast.makeText(this, getString(R.string.camera_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ───────────────────────────────────────────────────────────────────────────
    // Initialization
    // ───────────────────────────────────────────────────────────────────────────

    private fun initializeApp() {
        statusText.text = getString(R.string.initializing)

        ttsManager    = TextToSpeechManager(this)
        tfliteManager = TFLiteModelManager(this)

        val modelLoaded  = tfliteManager?.loadModel("rupiah_float32.tflite") ?: false
        val labelsLoaded = tfliteManager?.loadLabels("labels.txt")           ?: false

        if (!modelLoaded || !labelsLoaded) {
            statusText.text = getString(R.string.model_error)
            ttsManager?.speak(getString(R.string.model_error))
            Log.e(TAG, "Failed to load model or labels")
            return
        }

        cameraManager = CameraManager(this, previewView, this)
        val cameraOk  = cameraManager?.initialize { bitmap -> processFrame(bitmap) } ?: false

        if (!cameraOk) {
            statusText.text = getString(R.string.camera_error)
            ttsManager?.speak(getString(R.string.camera_error))
            Log.e(TAG, "Failed to initialize camera")
            return
        }

        statusText.text = "Siap mendeteksi uang"
        ttsManager?.speak("Aplikasi siap. Arahkan kamera ke uang untuk mendeteksi")
        Log.d(TAG, "App initialized successfully")
    }

    // ───────────────────────────────────────────────────────────────────────────
    // Frame processing
    // ───────────────────────────────────────────────────────────────────────────

    private fun processFrame(bitmap: Bitmap) {
        // Guard: jangan proses jika TTS sedang bicara atau frame sebelumnya belum selesai
        if (ttsManager?.isSpeaking == true) return
        if (isProcessing) return
        isProcessing = true

        lifecycleScope.launch {
            try {
                // Jalankan inferensi di IO thread agar tidak block UI
                val result = withContext(Dispatchers.IO) {
                    tfliteManager?.detectImage(bitmap)
                }

                if (result == null) {
                    updateStatus("Gagal memproses gambar")
                    return@launch
                }

                // ── Tambah ke voting buffer ───────────────────────────────────
                predictionHistory.add(
                    FramePrediction(result.label, result.confidence, result.margin)
                )
                if (predictionHistory.size > MAX_HISTORY) {
                    predictionHistory.removeAt(0)
                }

                // ── Update confidence text setiap frame ───────────────────────
                updateConfidenceText(result.confidence, result.margin)

                // ── Evaluasi voting ───────────────────────────────────────────
                val consensus   = getConsensusLabel()
                val currentTime = System.currentTimeMillis()

                when {
                    // Belum cukup frame untuk voting
                    consensus == null -> {
                        updateStatus("Memindai... (${predictionHistory.size}/$MAX_HISTORY frame)")
                    }

                    // Terdeteksi bukan uang
                    consensus == "Non Rupiah" -> {
                        updateStatus("Bukan uang, arahkan ke uang rupiah")
                        // Reset voting agar siap deteksi berikutnya
                        predictionHistory.clear()
                        Log.d(TAG, "Non Rupiah detected, voting reset")
                    }

                    // Confidence atau margin tidak cukup
                    !result.isConfident -> {
                        updateStatus(
                            "Kurang jelas (conf: ${"%.1f".format(result.confidence)}% " +
                                    "| margin: ${"%.2f".format(result.margin)})"
                        )
                    }

                    // ✅ Deteksi berhasil
                    else -> {
                        val labelChanged  = consensus != lastAnnouncedLabel
                        val timeoutPassed = currentTime - lastDetectionTime > DETECTION_DEBOUNCE_MS

                        if (labelChanged || timeoutPassed) {
                            // Announce via TTS
                            ttsManager?.speak("Terdeteksi $consensus")

                            // Update state
                            lastAnnouncedLabel = consensus
                            lastDetectionTime  = currentTime

                            // Update UI
                            updateStatus("Terdeteksi: $consensus")

                            // Reset voting setelah deteksi berhasil
                            predictionHistory.clear()

                            Log.d(TAG,
                                "✅ Detected: $consensus | " +
                                        "conf: ${"%.1f".format(result.confidence)}% | " +
                                        "margin: ${"%.3f".format(result.margin)}"
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing frame", e)
                updateStatus("Error: ${e.message}")
            } finally {
                isProcessing = false
            }
        }
    }

    // ───────────────────────────────────────────────────────────────────────────
    // Voting
    // ───────────────────────────────────────────────────────────────────────────

    /**
     * Cek apakah ada konsensus dari voting buffer.
     * Konsensus = label yang muncul >= MIN_CONSENSUS kali dari MAX_HISTORY frame.
     *
     * @return label konsensus, atau null jika belum ada
     */
    private fun getConsensusLabel(): String? {
        if (predictionHistory.size < MIN_CONSENSUS) return null

        val labelCounts = mutableMapOf<String, Int>()
        for (pred in predictionHistory) {
            labelCounts[pred.label] = (labelCounts[pred.label] ?: 0) + 1
        }

        // Cari label dengan jumlah >= MIN_CONSENSUS
        return labelCounts.entries
            .firstOrNull { it.value >= MIN_CONSENSUS }
            ?.key
    }

    // ───────────────────────────────────────────────────────────────────────────
    // UI helpers
    // ───────────────────────────────────────────────────────────────────────────

    private fun updateStatus(text: String) {
        runOnUiThread { statusText.text = text }
    }

    private fun updateConfidenceText(confidence: Float, margin: Float) {
        runOnUiThread {
            confidenceText.text = "Conf: ${"%.1f".format(confidence)}% | " +
                    "Margin: ${"%.2f".format(margin)}"
        }
    }

    // ───────────────────────────────────────────────────────────────────────────
    // Companion
    // ───────────────────────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "MainActivity"
    }
}