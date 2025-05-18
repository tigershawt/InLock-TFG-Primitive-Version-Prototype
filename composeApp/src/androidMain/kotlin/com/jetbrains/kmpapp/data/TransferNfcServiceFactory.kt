package com.jetbrains.kmpapp.data

import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual fun createTransferNfcService(): TransferNfcService {
    return object : TransferNfcService {
        override val tagData = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
        override val isTransferScanActive = kotlinx.coroutines.flow.MutableStateFlow(false)
        override fun isNfcSupported() = false
        override fun isNfcEnabled() = false
        override fun startTransferScan() = Result.failure<Unit>(IllegalStateException("Use Koin to get TransferNfcService"))
        override fun stopTransferScan() {}
        override fun extractRfidIdFromTagData(tagData: String) = ""
    }
}

class TransferNfcServiceFactory(private val context: Context) {
    fun create(): TransferNfcService {
        return TransferNfcServiceAndroid(context)
    }
}