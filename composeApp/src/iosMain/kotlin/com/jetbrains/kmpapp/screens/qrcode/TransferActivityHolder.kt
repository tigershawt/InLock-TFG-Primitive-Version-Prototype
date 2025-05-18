package com.jetbrains.kmpapp.screens.qrcode



actual object TransferActivityHolder : TransferActivityHolderInterface {
    actual override fun processTransferIntent(intent: Any): Boolean {
        return false
    }
}