package com.jetbrains.kmpapp.data.repository

import com.jetbrains.kmpapp.data.FirebaseService
import com.jetbrains.kmpapp.mvi.Repository
import com.jetbrains.kmpapp.screens.qrcode.QRCodeService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class QRCodeRequest {
    data class GenerateQRCode(val content: String) : QRCodeRequest()
    object StartQRScan : QRCodeRequest()
    object StopQRScan : QRCodeRequest()
    object GetCurrentUserId : QRCodeRequest()
}

sealed class QRCodeResponse {
    data class QRCodeGenerated(val qrCodeData: ByteArray) : QRCodeResponse()
    data class ScannedResult(val userId: String) : QRCodeResponse()
    data class Error(val message: String) : QRCodeResponse()
    data class UserId(val userId: String?) : QRCodeResponse()
    object ScanStopped : QRCodeResponse()
}

class QRCodeRepository(
    private val firebaseService: FirebaseService,
    private val qrCodeService: QRCodeService
) : Repository<QRCodeRequest, QRCodeResponse> {
    
    override suspend fun execute(request: QRCodeRequest): Flow<QRCodeResponse> = flow {
        try {
            when (request) {
                is QRCodeRequest.GenerateQRCode -> {
                    val qrCodeData = qrCodeService.generateQRCode(request.content)
                    emit(QRCodeResponse.QRCodeGenerated(qrCodeData))
                }
                is QRCodeRequest.StartQRScan -> {
                    qrCodeService.startQRCodeScan().collect { result ->
                        result.onSuccess { scannedContent ->
                            emit(QRCodeResponse.ScannedResult(scannedContent))
                        }.onFailure { error ->
                            emit(QRCodeResponse.Error(error.message ?: "Failed to scan QR code"))
                        }
                    }
                }
                is QRCodeRequest.StopQRScan -> {
                    qrCodeService.stopQRCodeScan()
                    emit(QRCodeResponse.ScanStopped)
                }
                is QRCodeRequest.GetCurrentUserId -> {
                    val userId = firebaseService.getCurrentUserId()
                    emit(QRCodeResponse.UserId(userId))
                }
            }
        } catch (e: Exception) {
            emit(QRCodeResponse.Error(e.message ?: "An unexpected error occurred"))
        }
    }
}