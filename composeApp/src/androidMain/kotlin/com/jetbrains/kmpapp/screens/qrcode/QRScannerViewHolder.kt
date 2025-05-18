package com.jetbrains.kmpapp.screens.qrcode

import androidx.camera.view.PreviewView

object QRScannerViewHolder {
    @Volatile
    private var previewView: PreviewView? = null

    @Synchronized
    fun setPreviewView(view: PreviewView) {
        previewView = view
    }

    @Synchronized
    fun getPreviewView(): PreviewView? {
        return previewView
    }

    @Synchronized
    fun clearPreviewView() {
        previewView = null
    }
}