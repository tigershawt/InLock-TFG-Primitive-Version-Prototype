package com.jetbrains.kmpapp.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TransferNfcServiceIOS : TransferNfcService {
    private val _tagData = MutableStateFlow<String?>(null)
    override val tagData: StateFlow<String?> = _tagData
    
    private val _isTransferScanActive = MutableStateFlow(false)
    override val isTransferScanActive: StateFlow<Boolean> = _isTransferScanActive

    override fun isNfcSupported(): Boolean = false

    override fun isNfcEnabled(): Boolean = false

    override fun startTransferScan(): Result<Unit> {
        return Result.failure(Exception("NFC scanning not supported on iOS yet"))
    }

    override fun stopTransferScan() {
        _isTransferScanActive.value = false
    }
    
    override fun extractRfidIdFromTagData(tagData: String): String {
        return ""
    }
}

actual fun createTransferNfcService(): TransferNfcService = TransferNfcServiceIOS()