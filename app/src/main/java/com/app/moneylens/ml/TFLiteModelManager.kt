package com.app.moneylens.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * TensorFlow Lite Model Manager for Rupiah Classifier
 * Model Specs:
 * - Input: 224x224 RGB images (float32, normalized 0-1)
 * - Output: 7 softmax probabilities (Rp 1000-100000)
 * - Threshold: 85% confidence for reliable detection
 */
class TFLiteModelManager(private val context: Context) {
    private var interpreter: Interpreter? = null
    private var inputShape: IntArray = intArrayOf()
    private var outputShape: IntArray = intArrayOf()
    private var labels: List<String> = emptyList()

    fun loadModel(modelPath: String): Boolean {
        return try {
            val file = File(context.filesDir.parent, "app_flutter/models/$modelPath")
            if (!file.exists()) {
                // Try loading from assets
                loadModelFromAssets(modelPath)
            } else {
                loadModelFromFile(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            loadModelFromAssets(modelPath)
        }
    }

    private fun loadModelFromAssets(modelPath: String): Boolean {
        return try {
            val assetFileDescriptor = context.assets.openFd(modelPath)
            val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = fileInputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val mappedByteBuffer = fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                startOffset,
                declaredLength
            )

            interpreter = Interpreter(mappedByteBuffer)

            // Get input and output info
            inputShape = interpreter!!.getInputTensor(0).shape()
            outputShape = interpreter!!.getOutputTensor(0).shape()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun loadModelFromFile(file: File): Boolean {
        return try {
            val fileInputStream = FileInputStream(file)
            val fileChannel = fileInputStream.channel
            val mappedByteBuffer = fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                0,
                fileChannel.size()
            )

            interpreter = Interpreter(mappedByteBuffer)

            // Get input and output info
            inputShape = interpreter!!.getInputTensor(0).shape()
            outputShape = interpreter!!.getOutputTensor(0).shape()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadLabels(labelPath: String): Boolean {
        return try {
            val inputStream = context.assets.open(labelPath)
            val content = inputStream.bufferedReader().use { it.readText() }
            labels = content.split("\n").filter { it.isNotEmpty() }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    data class DetectionResult(
        val label: String,
        val confidence: Float,
        val isConfident: Boolean // true jika confidence >= 85%
    )

    fun detectImage(bitmap: Bitmap): DetectionResult? {
        return try {
            if (interpreter == null || inputShape.isEmpty()) {
                return null
            }

            // Input shape format: [batch, height, width, channels] = [1, 224, 224, 3]
            if (inputShape.size < 4) {
                return null
            }

            val inputBatch = inputShape[0]
            val inputHeight = inputShape[1]
            val inputWidth = inputShape[2]
            val inputChannels = inputShape[3]

            // Validate expected dimensions for Rupiah classifier
            if (inputHeight != 224 || inputWidth != 224 || inputChannels != 3) {
                return null
            }

            // Resize bitmap to match model input
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)

            // Convert bitmap to ByteBuffer with normalization (pixel / 255.0f)
            val inputBuffer = ByteBuffer.allocateDirect(
                4 * inputBatch * inputHeight * inputWidth * inputChannels
            )
            inputBuffer.order(ByteOrder.nativeOrder())

            // Extract RGB pixels and normalize to 0-1 range
            val pixels = IntArray(inputWidth * inputHeight)
            resizedBitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)

            for (pixel in pixels) {
                // Extract RGB components and normalize
                val r = ((pixel shr 16) and 0xFF) / 255.0f
                val g = ((pixel shr 8) and 0xFF) / 255.0f
                val b = (pixel and 0xFF) / 255.0f

                inputBuffer.putFloat(r)
                inputBuffer.putFloat(g)
                inputBuffer.putFloat(b)
            }

            inputBuffer.rewind()

             // Prepare output buffer for 8 classes (Rp 1000-100000 + Non Rupiah)
             val outputSize = outputShape.getOrNull(1) ?: 8
             val outputBuffer = Array(1) { FloatArray(outputSize) }

            // Run inference
            interpreter!!.run(inputBuffer, outputBuffer)

            // Get results from softmax output
            val probabilities = outputBuffer[0]
            val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
            val confidenceScore = probabilities[maxIndex]
            val confidencePercent = confidenceScore * 100f
            val isConfident = confidencePercent >= CONFIDENCE_THRESHOLD

            DetectionResult(
                label = if (maxIndex < labels.size) labels[maxIndex] else "Unknown",
                confidence = confidencePercent,
                isConfident = isConfident
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun close() {
        interpreter?.close()
    }

    companion object {
        private const val CONFIDENCE_THRESHOLD = 85f
    }
}

