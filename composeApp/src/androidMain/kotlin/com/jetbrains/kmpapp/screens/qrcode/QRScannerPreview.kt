package com.jetbrains.kmpapp.screens.qrcode

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.jetbrains.kmpapp.ui.theme.InLockBlue

@Composable
actual fun QRScannerPreview() {
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    id = android.R.id.content
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                QRScannerViewHolder.setPreviewView(previewView)
            }
        )

        LaunchedEffect(Unit) {
            QRScannerViewHolder.getPreviewView()
        }

        DisposableEffect(lifecycleOwner) {
            onDispose {
                QRScannerViewHolder.clearPreviewView()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = InLockBlue)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Scanning for QR Code...",
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}