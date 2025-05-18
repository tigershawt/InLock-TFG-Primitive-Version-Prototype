package com.jetbrains.kmpapp.data

import android.app.Activity
import android.nfc.Tag


fun TransferNfcService.setTransferActivity(activity: Activity) {
    when (this) {
        is TransferNfcServiceAndroid -> this.setTransferActivity(activity)
        is TransferNfcServiceRedesigned -> this.setTransferActivity(activity)
        else -> throw IllegalStateException("Unknown TransferNfcService implementation")
    }
}

fun TransferNfcService.releaseActivity() {
    when (this) {
        is TransferNfcServiceAndroid -> this.releaseActivity()
        is TransferNfcServiceRedesigned -> this.releaseActivity()
        else -> { /* No-op for other implementations */ }
    }
}

fun TransferNfcService.processTransferTag(tag: Tag): String {
    return when (this) {
        is TransferNfcServiceAndroid -> this.processTransferTag(tag)
        is TransferNfcServiceRedesigned -> this.processTransferTag(tag)
        else -> throw IllegalStateException("Unknown TransferNfcService implementation")
    }
}