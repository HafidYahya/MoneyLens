package com.app.moneylens.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors

class CameraManager(
    private val context: Context,
    private val previewView: PreviewView,
    private val lifecycleOwner: LifecycleOwner
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var onFrameAvailable: ((Bitmap) -> Unit)? = null
    private val executorService = Executors.newSingleThreadExecutor()
    private var isTorchOn = false
    private var boundCamera: Camera? = null  // Store reference to bound camera

    fun initialize(onFrameCallback: (Bitmap) -> Unit): Boolean {
        return try {
            onFrameAvailable = onFrameCallback

            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    cameraProvider = cameraProviderFuture.get()

                    // Preview use case
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    // Image analysis use case
                    imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(executorService, MyImageAnalyzer { bitmap ->
                                onFrameAvailable?.invoke(bitmap)
                            })
                        }

                    // Select back camera
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    // Unbind use cases before rebinding
                    cameraProvider?.unbindAll()

                    // Bind use cases to camera and store camera reference
                    boundCamera = cameraProvider?.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )
                } catch (exc: Exception) {
                    Log.e(TAG, "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(context))

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize camera", e)
            false
        }
    }

    fun toggleTorch(enable: Boolean) {
        try {
            // Use the stored camera reference to control torch
            if (boundCamera != null) {
                boundCamera?.cameraControl?.enableTorch(enable)
                isTorchOn = enable
                Log.d(TAG, "Torch: ${if (enable) "ON" else "OFF"}")
            } else {
                Log.w(TAG, "Camera not yet bound, cannot toggle torch")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle torch", e)
        }
    }

    fun isTorchEnabled(): Boolean = isTorchOn

    fun stopCamera() {
        cameraProvider?.unbindAll()
        executorService.shutdown()
    }

    companion object {
        private const val TAG = "CameraManager"
    }

    private class MyImageAnalyzer(private val onImageAvailable: (Bitmap) -> Unit) :
        ImageAnalysis.Analyzer {

        override fun analyze(image: ImageProxy) {
            try {
                val bitmap = image.toBitmap()
                onImageAvailable(bitmap)
            } catch (e: Exception) {
                Log.e("ImageAnalyzer", "Error converting image", e)
            } finally {
                image.close()
            }
        }

        private fun ImageProxy.toBitmap(): Bitmap {
            val nv21 = yuv420ToNv21(this)
            val yuvImage = android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, width, height, null)
            val out = java.io.ByteArrayOutputStream()
            yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 100, out)
            val imageBytes = out.toByteArray()
            return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        }

        private fun yuv420ToNv21(image: ImageProxy): ByteArray {
            val planes = image.planes
            val ySize = planes[0].buffer.remaining()
            val u8Size = planes[1].buffer.remaining()
            val v8Size = planes[2].buffer.remaining()
            val nv21 = ByteArray(ySize + u8Size + v8Size)

            planes[0].buffer.get(nv21, 0, ySize)
            val pixelStride = planes[1].pixelStride

            if (pixelStride == 1) {
                planes[1].buffer.get(nv21, ySize, u8Size)
                planes[2].buffer.get(nv21, ySize + u8Size, v8Size)
            } else {
                val uvBuffer = ByteArray(u8Size + v8Size)
                planes[1].buffer.get(uvBuffer, 0, u8Size)
                planes[2].buffer.get(uvBuffer, u8Size, v8Size)
                for (i in 0 until u8Size + v8Size) {
                    nv21[ySize + i] = uvBuffer[i]
                }
            }
            return nv21
        }
    }
}

