package com.jetbrains.kmpapp.screens.qrcode

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class QRCodeServiceIOS : QRCodeService {
    private var isScanning = false

    override suspend fun generateQRCode(content: String): ByteArray {
        return ByteArray(0)
    }

    override suspend fun startQRCodeScan(): Flow<Result<String>> = flow {
        isScanning = true
        emit(Result.success("ios-user-123456"))
    }

    override suspend fun stopQRCodeScan() {
        isScanning = false
    }
}

actual fun createQRCodeService(): QRCodeService = QRCodeServiceIOS()