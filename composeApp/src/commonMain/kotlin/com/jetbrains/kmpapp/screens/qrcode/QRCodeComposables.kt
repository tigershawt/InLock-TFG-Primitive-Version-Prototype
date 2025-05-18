package com.jetbrains.kmpapp.screens.qrcode

import androidx.compose.runtime.Composable

@Composable
expect fun QRCodeImage(qrCodeData: ByteArray)

@Composable
expect fun QRScannerPreview()