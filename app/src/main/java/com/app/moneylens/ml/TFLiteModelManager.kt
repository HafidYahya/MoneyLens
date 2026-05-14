package com.app.moneylens.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * TensorFlow Lite Model Manager for Rupiah Classifier
 *
 * Model Specs:
 * - Input  : [1, 224, 224, 3] float32, normalized 0.0–1.0 (RGB)
 * - Output : [1, 8] softmax probabilities (Rp 1000–100000 + Non Rupiah)
 *
 * Preprocessing pipeline (identical to Python training):
 *   1. Center crop → square
 *   2. Resize to 224×224 (bilinear)
 *   3. Force ARGB_8888
 *   4. Extract RGB, normalize / 255.0f
 *   5. Rewind buffer → run inference
 */
class TFLiteModelManager(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var inputShape: IntArray      = intArrayOf()
    private var outputShape: IntArray     = intArrayOf()
    private var labels: List<String>      = emptyList()

    // ─── Data class ────────────────────────────────────────────────────────────

    data class DetectionResult(
        val label      : String,
        val confidence : Float,   // 0–100%
        val margin     : Float,   // top1 prob – top2 prob (0.0–1.0)
        val allProbs   : FloatArray,
        val isConfident: Boolean  // true jika confidence ≥ threshold AND margin ≥ margin threshold
    )

    // ─── Model loading ─────────────────────────────────────────────────────────

    fun loadModel(modelPath: String): Boolean {
        return try {
            val file = File(context.filesDir.parent, "app_flutter/models/$modelPath")
            if (file.exists()) loadModelFromFile(file) else loadModelFromAssets(modelPath)
        } catch (e: Exception) {
            Log.e(TAG, "loadModel error, fallback to assets", e)
            loadModelFromAssets(modelPath)
        }
    }

    private fun loadModelFromAssets(modelPath: String): Boolean {
        return try {
            val afd           = context.assets.openFd(modelPath)
            val channel       = FileInputStream(afd.fileDescriptor).channel
            val mappedBuffer  = channel.map(
                FileChannel.MapMode.READ_ONLY,
                afd.startOffset,
                afd.declaredLength
            )

            val options = Interpreter.Options().apply {
                numThreads = 4          // Gunakan 4 thread CPU
                useNNAPI   = true       // Aktifkan NNAPI accelerator jika tersedia
            }

            interpreter  = Interpreter(mappedBuffer, options)
            inputShape   = interpreter!!.getInputTensor(0).shape()
            outputShape  = interpreter!!.getOutputTensor(0).shape()

            Log.d(TAG, "Model loaded from assets: $modelPath")
            Log.d(TAG, "Input shape  : ${inputShape.toList()}")
            Log.d(TAG, "Output shape : ${outputShape.toList()}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "loadModelFromAssets failed", e)
            false
        }
    }

    private fun loadModelFromFile(file: File): Boolean {
        return try {
            val channel      = FileInputStream(file).channel
            val mappedBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())

            val options = Interpreter.Options().apply {
                numThreads = 4
                useNNAPI   = true
            }

            interpreter  = Interpreter(mappedBuffer, options)
            inputShape   = interpreter!!.getInputTensor(0).shape()
            outputShape  = interpreter!!.getOutputTensor(0).shape()

            Log.d(TAG, "Model loaded from file: ${file.path}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "loadModelFromFile failed", e)
            false
        }
    }

    fun loadLabels(labelPath: String): Boolean {
        return try {
            val text = context.assets.open(labelPath).bufferedReader().use { it.readText() }
            labels   = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            Log.d(TAG, "Labels loaded (${labels.size}): $labels")
            true
        } catch (e: Exception) {
            Log.e(TAG, "loadLabels failed", e)
            false
        }
    }

    // ─── Inference ─────────────────────────────────────────────────────────────

    fun detectImage(bitmap: Bitmap): DetectionResult? {
        return try {
            if (interpreter == null || inputShape.size < 4) {
                Log.w(TAG, "Interpreter not ready")
                return null
            }

            val inputHeight   = inputShape[1]   // 224
            val inputWidth    = inputShape[2]   // 224
            val inputChannels = inputShape[3]   // 3

            if (inputHeight != 224 || inputWidth != 224 || inputChannels != 3) {
                Log.e(TAG, "Unexpected input shape: ${inputShape.toList()}")
                return null
            }

            // ── Step 1: Center crop → square ──────────────────────────────────
            val squareSize = minOf(bitmap.width, bitmap.height)
            val cropX      = (bitmap.width  - squareSize) / 2
            val cropY      = (bitmap.height - squareSize) / 2
            val cropped    = Bitmap.createBitmap(bitmap, cropX, cropY, squareSize, squareSize)

            // ── Step 2: Resize ke 224×224 (bilinear = true) ───────────────────
            val resized = Bitmap.createScaledBitmap(cropped, inputWidth, inputHeight, true)

            // ── Step 3: Pastikan format ARGB_8888 ─────────────────────────────
            val argb = if (resized.config == Bitmap.Config.ARGB_8888) {
                resized
            } else {
                resized.copy(Bitmap.Config.ARGB_8888, false)
            }

            // ── Step 4: Isi ByteBuffer (R, G, B per pixel, normalized 0–1) ────
            val bufferSize  = 4 * inputHeight * inputWidth * inputChannels
            val inputBuffer = ByteBuffer.allocateDirect(bufferSize)
            inputBuffer.order(ByteOrder.nativeOrder())

            val pixels = IntArray(inputWidth * inputHeight)
            argb.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)

            for (pixel in pixels) {
                inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)   // R
                inputBuffer.putFloat(((pixel shr 8)  and 0xFF) / 255.0f)   // G
                inputBuffer.putFloat((pixel           and 0xFF) / 255.0f)   // B
            }

            // ── Step 5: Rewind SETELAH mengisi buffer ─────────────────────────
            inputBuffer.rewind()

            // ── Step 6: Run inference ─────────────────────────────────────────
            val outputSize   = outputShape.getOrNull(1) ?: labels.size
            val outputBuffer = Array(1) { FloatArray(outputSize) }
            interpreter!!.run(inputBuffer, outputBuffer)

            // ── Step 7: Parse hasil ───────────────────────────────────────────
            val probs    = outputBuffer[0]
            val maxIndex = probs.indices.maxByOrNull { probs[it] } ?: 0
            val confidence = probs[maxIndex] * 100f

            // Hitung margin: selisih top-1 dan top-2
            val sorted = probs.sortedDescending()
            val margin = if (sorted.size >= 2) sorted[0] - sorted[1] else 1.0f

            val label = if (maxIndex < labels.size) labels[maxIndex] else "Unknown"
            val isConfident = confidence >= CONFIDENCE_THRESHOLD &&
                    margin     >= MARGIN_THRESHOLD

            Log.d(TAG, "Result → label: $label | conf: ${"%.1f".format(confidence)}% " +
                    "| margin: ${"%.3f".format(margin)} | confident: $isConfident")

            DetectionResult(
                label       = label,
                confidence  = confidence,
                margin      = margin,
                allProbs    = probs,
                isConfident = isConfident
            )

        } catch (e: Exception) {
            Log.e(TAG, "detectImage error", e)
            null
        }
    }

    // ─── Cleanup ───────────────────────────────────────────────────────────────

    fun close() {
        interpreter?.close()
        interpreter = null
        Log.d(TAG, "Interpreter closed")
    }

    // ─── Constants ─────────────────────────────────────────────────────────────

    companion object {
        private const val TAG                  = "TFLiteModelManager"
        const        val CONFIDENCE_THRESHOLD  = 85f     // Minimum confidence (%)
        private      val MARGIN_THRESHOLD      = 0.15f   // Minimum top1–top2 gap
    }
}