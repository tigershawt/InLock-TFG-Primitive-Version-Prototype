package com.jetbrains.kmpapp.screens.qrcode

import android.annotation.SuppressLint
import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QRCodeAnalyzer(private val context: Context) {
    private var cameraExecutor: ExecutorService? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var preview: Preview? = null
    private var qrCodeDetected = false

    @SuppressLint("UnsafeOptInUsageError")
    fun startScanning(
        previewView: androidx.camera.view.PreviewView,
        onQrCodeDetected: (Barcode) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            qrCodeDetected = false
            cameraExecutor = Executors.newSingleThreadExecutor()
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                try {
                    cameraProvider = cameraProviderFuture.get()

                    preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                    imageAnalysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis?.setAnalyzer(cameraExecutor!!) { imageProxy ->
                        if (!qrCodeDetected) {
                            processImageProxy(imageProxy, onQrCodeDetected)
                        } else {
                            imageProxy.close()
                        }
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    cameraProvider?.unbindAll()

                    if (context is LifecycleOwner) {
                        cameraProvider?.bindToLifecycle(
                            context as LifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } else {
                        throw IllegalStateException("Context is not a LifecycleOwner")
                    }
                } catch (e: Exception) {
                    onError(e)
                }
            }, ContextCompat.getMainExecutor(context))

        } catch (e: Exception) {
            onError(e)
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(
        imageProxy: ImageProxy,
        onQrCodeDetected: (Barcode) -> Unit
    ) {
        val mediaImage = imageProxy.image

        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            val scanner = BarcodeScanning.getClient()

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        if (barcode.format == Barcode.FORMAT_QR_CODE) {
                            qrCodeDetected = true
                            onQrCodeDetected(barcode)
                            break
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    fun stopScanning() {
        try {
            cameraProvider?.unbindAll()
            cameraExecutor?.shutdown()
            cameraExecutor = null
            imageAnalysis = null
            preview = null
            qrCodeDetected = false
        } catch (e: Exception) {
            
        }
    }
}
