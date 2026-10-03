package com.mika.app.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class CameraManager(private val context: Context) {

    private var imageCapture: ImageCapture? = null
    private var isFrontCamera = true

    private val detectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()

    private val faceDetector = FaceDetection.getClient(detectorOptions)

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        useFront: Boolean,
        onFaceDetected: (smiling: Boolean, sleepy: Boolean) -> Unit
    ) {
        isFrontCamera = useFront
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = if (isFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageCapture
                )
            } catch (_: Exception) {}

        }, ContextCompat.getMainExecutor(context))
    }

    fun captureSingleFrameBase64(onCaptured: (String?) -> Unit) {
        val capture = imageCapture ?: run {
            onCaptured(null)
            return
        }

        capture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()

                    if (bitmap != null) {
                        val resized = Bitmap.createScaledBitmap(bitmap, 768, (768f * bitmap.height / bitmap.width).toInt(), true)
                        val stream = ByteArrayOutputStream()
                        resized.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                        val byteArray = stream.toByteArray()
                        val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
                        onCaptured(base64)
                    } else {
                        onCaptured(null)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    onCaptured(null)
                }
            }
        )
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}
