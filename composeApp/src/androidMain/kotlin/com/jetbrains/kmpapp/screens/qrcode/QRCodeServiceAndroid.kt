package com.jetbrains.kmpapp.screens.qrcode

import android.graphics.Bitmap
import android.graphics.Color
import androidx.camera.view.PreviewView
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.ByteArrayOutputStream

actual fun createQRCodeService(): QRCodeService = QRCodeServiceAndroid()

class QRCodeServiceAndroid : QRCodeService {
    private var qrCodeAnalyzer: QRCodeAnalyzer? = null
    private var cameraPermissionHelper: CameraPermissionHelper? = null
    private var isScanActive = false

    override suspend fun generateQRCode(content: String): ByteArray {
        val size = 512
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H
        )

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()
        bitmap.recycle()

        return byteArray
    }

    override suspend fun startQRCodeScan(): Flow<Result<String>> = callbackFlow {
        if (isScanActive) {
            stopQRCodeScan()
        }

        isScanActive = true

        try {
            val activity = MainActivityHolder.getActivity()

            if (cameraPermissionHelper == null) {
                cameraPermissionHelper = CameraPermissionHelper(activity)
            }

            var previewView: PreviewView? = null
            var retryCount = 0

            while (previewView == null && retryCount < 10) {
                previewView = QRScannerViewHolder.getPreviewView()
                if (previewView == null) {
                    delay(200)
                    retryCount++
                }
            }

            if (previewView == null) {
                trySend(Result.failure(Exception("Camera preview not available")))
                close()
                return@callbackFlow
            }

            cameraPermissionHelper?.requestCameraPermission(
                onGranted = {
                    qrCodeAnalyzer = QRCodeAnalyzer(activity)
                    qrCodeAnalyzer?.startScanning(
                        previewView = previewView,
                        onQrCodeDetected = { barcode ->
                            val qrContent = barcode.displayValue ?: ""
                            if (qrContent.isNotEmpty()) {
                                trySend(Result.success(qrContent))
                            }
                        },
                        onError = { error ->
                            trySend(Result.failure(error))
                        }
                    )
                },
                onDenied = {
                    trySend(Result.failure(Exception("Camera permission denied")))
                    close()
                }
            )
        } catch (e: Exception) {
            trySend(Result.failure(e))
            close()
        }

        awaitClose {
            isScanActive = false
            qrCodeAnalyzer?.stopScanning()
            qrCodeAnalyzer = null
        }
    }

    override suspend fun stopQRCodeScan() {
        isScanActive = false
        qrCodeAnalyzer?.stopScanning()
        qrCodeAnalyzer = null
    }
}