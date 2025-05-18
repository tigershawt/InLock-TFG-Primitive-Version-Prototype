package com.jetbrains.kmpapp.screens.qrcode

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.ui.icons.AppIcon
import com.jetbrains.kmpapp.ui.icons.AppIcons

@Composable
actual fun QRCodeImage(qrCodeData: ByteArray) {
    val bitmap = remember(qrCodeData) {
        runCatching {
            BitmapFactory.decodeByteArray(qrCodeData, 0, qrCodeData.size)
        }.getOrNull()
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "QR Code",
            modifier = Modifier.size(200.dp),
            contentScale = ContentScale.Fit
        )
    } else {
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            AppIcon(
                icon = AppIcons.QrCode,
                contentDescription = "QR Code",
                tint = Color.Black,
                modifier = Modifier.size(150.dp)
            )
        }
    }
}