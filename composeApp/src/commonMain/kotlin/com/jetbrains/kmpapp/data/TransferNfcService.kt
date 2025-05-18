package com.jetbrains.kmpapp.data

import kotlinx.coroutines.flow.StateFlow

interface TransferNfcService {
    val tagData: StateFlow<String?>
    val isTransferScanActive: StateFlow<Boolean>
    
    fun isNfcSupported(): Boolean
    fun isNfcEnabled(): Boolean
    fun startTransferScan(): Result<Unit>
    fun stopTransferScan()
    fun extractRfidIdFromTagData(tagData: String): String
}

expect fun createTransferNfcService(): TransferNfcService