package com.jetbrains.kmpapp.data

import kotlinx.coroutines.flow.StateFlow

interface NfcService {

    val tagData: StateFlow<String?>

    val isScanning: StateFlow<Boolean>

    fun isNfcSupported(): Boolean

    fun isNfcEnabled(): Boolean

    fun startNfcScan(): Result<Unit>

    fun stopNfcScan()

    suspend fun readNfcTag(): Result<String>
}
expect fun createNfcService(): NfcService