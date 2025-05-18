package com.jetbrains.kmpapp.screens.qrcode

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val preview = remember { Preview.Builder().build() }
    val previewView = remember {
        PreviewView(context).apply {
            this.scaleType = scaleType
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { ContextCompat.getMainExecutor(context) }

    DisposableEffect(key1 = cameraSelector) {
        val cameraProviderListener = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
                preview.setSurfaceProvider(previewView.surfaceProvider)
            } catch (e: Exception) {
                
            }
        }

        cameraProviderFuture.addListener(cameraProviderListener, executor)

        onDispose {
            try {
                cameraProviderFuture.get().unbindAll()
            } catch (e: Exception) {
                
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
    }
}