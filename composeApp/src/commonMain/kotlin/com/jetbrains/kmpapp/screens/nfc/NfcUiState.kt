package com.jetbrains.kmpapp.screens.nfc

data class NfcUiState(
    val isScanning: Boolean = false,
    val isNfcSupported: Boolean = false,
    val isNfcEnabled: Boolean = false,
    val tagData: String = "",
    val error: String = "",
    val blockchainResponse: String = ""
)