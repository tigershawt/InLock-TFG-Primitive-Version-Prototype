
package com.jetbrains.kmpapp.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual fun createNfcService(): NfcService = NfcServiceStub()

class NfcServiceStub : NfcService {
    private val _tagData = MutableStateFlow<String?>(null)
    override val tagData: StateFlow<String?> = _tagData.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    override fun isNfcSupported(): Boolean {
        
        return false
    }

    override fun isNfcEnabled(): Boolean {
        
        return false
    }

    override fun startNfcScan(): Result<Unit> = runCatching {
        throw UnsupportedOperationException("NFC scanning is not implemented on iOS yet")
    }

    override fun stopNfcScan() {
        
    }

    override suspend fun readNfcTag(): Result<String> = runCatching {
        throw UnsupportedOperationException("NFC scanning is not implemented on iOS yet")
    }
}