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
import com.app.moneylens.camera.CameraManager
import com.app.moneylens.ml.TFLiteModelManager
import com.app.moneylens.tts.TextToSpeechManager
import com.app.moneylens.utils.PermissionManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var flashButton: Button
    private lateinit var statusText: TextView
    private lateinit var confidenceText: TextView
    
    private var cameraManager: CameraManager? = null
    private var tfliteManager: TFLiteModelManager? = null
    private var ttsManager: TextToSpeechManager? = null
    
    private var isProcessing = false
    private var lastDetectedLabel = ""
    private var lastConfidence = 0f
    private var lastIsConfident = false
    private var lastDetectionTime = 0L
    private val detectionDebounceMs = 1500L  // Wait 1.5 seconds between detections
    
    // Voting system: track last 5 predictions to filter noise
    private data class FramePrediction(val label: String, val confidence: Float)
    private val predictionHistory = mutableListOf<FramePrediction>()
    private val MAX_HISTORY = 5
    private val MIN_CONSENSUS = 4  // 4 out of 5 must match

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        previewView = findViewById(R.id.previewView)
        flashButton = findViewById(R.id.flashButton)
        statusText = findViewById(R.id.statusText)
        confidenceText = findViewById(R.id.confidenceText)

        // Check and request permissions
        if (PermissionManager.hasAllPermissions(this)) {
            initializeApp()
        } else {
            PermissionManager.requestPermissions(this)
        }

        // Flash button listener
        flashButton.setOnClickListener {
            cameraManager?.let {
                val isOn = it.isTorchEnabled()
                it.toggleTorch(!isOn)
                val message = if (!isOn) getString(R.string.flash_on) else getString(R.string.flash_off)
                ttsManager?.speak(message)
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initializeApp() {
        statusText.text = getString(R.string.initializing)

        // Initialize TTS
        ttsManager = TextToSpeechManager(this)

        // Initialize TensorFlow Lite Model
        tfliteManager = TFLiteModelManager(this)
        
        // Load model and labels
        val modelLoaded = tfliteManager?.loadModel("rupiah_float32.tflite") ?: false
        val labelsLoaded = tfliteManager?.loadLabels("labels.txt") ?: false

        if (!modelLoaded || !labelsLoaded) {
            statusText.text = getString(R.string.model_error)
            ttsManager?.speak(getString(R.string.model_error))
            Log.e(TAG, "Failed to load model or labels")
            return
        }

        // Initialize Camera
        cameraManager = CameraManager(this, previewView, this)
        val cameraInitialized = cameraManager?.initialize { bitmap ->
            processFrame(bitmap)
        } ?: false

        if (!cameraInitialized) {
            statusText.text = getString(R.string.camera_error)
            ttsManager?.speak(getString(R.string.camera_error))
            Log.e(TAG, "Failed to initialize camera")
        }

        statusText.text = "Siap mendeteksi uang"
        ttsManager?.speak("Aplikasi siap. Arahkan kamera ke uang untuk mendeteksi")
    }

    private fun processFrame(bitmap: Bitmap) {
        // Don't process if TTS is still speaking (prevent spam detection)
        if (ttsManager?.isSpeaking == true) return
        if (isProcessing) return

        isProcessing = true
        lifecycleScope.launch {
            try {
                val result = tfliteManager?.detectImage(bitmap)

                if (result != null) {
                    lastDetectedLabel = result.label
                    lastConfidence = result.confidence
                    lastIsConfident = result.isConfident

                    // Add to prediction history for voting system
                    predictionHistory.add(FramePrediction(result.label, result.confidence))
                    if (predictionHistory.size > MAX_HISTORY) {
                        predictionHistory.removeAt(0)
                    }

                    // Update UI
                    statusText.text = if (result.isConfident) {
                        getString(R.string.detected)
                    } else {
                        getString(R.string.detecting)
                    }

                    confidenceText.text = String.format(
                        "Confidence: %.1f%%",
                        result.confidence
                    )

                    // Voting system: check if we have consensus
                    val consensus = getConsensusLabel()
                    
                    // Handle TTS output based on voting system
                    if (consensus != null && result.isConfident && consensus != "Non Rupiah") {
                        val currentTime = System.currentTimeMillis()
                        
                        // Check if label changed or enough time passed
                        if (lastDetectedLabel != consensus || 
                            (currentTime - lastDetectionTime > detectionDebounceMs)) {
                            
                            ttsManager?.speak("Terdeteksi: $consensus")
                            lastDetectionTime = currentTime
                            lastDetectedLabel = consensus
                            Log.d(TAG, "Detected via Voting: $consensus, Confidence: ${result.confidence}%")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing frame", e)
            } finally {
                isProcessing = false
            }
        }
    }

    /**
     * Check if we have consensus (4 out of 5 predictions match)
     * Returns the consensus label or null if no consensus
     */
    private fun getConsensusLabel(): String? {
        if (predictionHistory.size < MIN_CONSENSUS) {
            return null
        }

        // Count occurrences of each label
        val labelCounts = mutableMapOf<String, Int>()
        for (prediction in predictionHistory) {
            labelCounts[prediction.label] = labelCounts.getOrDefault(prediction.label, 0) + 1
        }

        // Find label with consensus (4+)
        for ((label, count) in labelCounts) {
            if (count >= MIN_CONSENSUS) {
                return label
            }
        }

        return null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeApp()
            } else {
                statusText.text = getString(R.string.camera_error)
                ttsManager?.speak("Izin kamera diperlukan untuk menggunakan aplikasi ini")
                Toast.makeText(this, getString(R.string.camera_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager?.stopCamera()
        tfliteManager?.close()
        ttsManager?.shutdown()
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

