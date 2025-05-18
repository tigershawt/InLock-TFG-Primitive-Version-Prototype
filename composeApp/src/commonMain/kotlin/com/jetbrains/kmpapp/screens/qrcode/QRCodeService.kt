package com.jetbrains.kmpapp.screens.qrcode

import kotlinx.coroutines.flow.Flow

interface QRCodeService {
    suspend fun generateQRCode(content: String): ByteArray

    suspend fun startQRCodeScan(): Flow<Result<String>>

    suspend fun stopQRCodeScan()
}

expect fun createQRCodeService(): QRCodeService