package com.jetbrains.kmpapp.screens.qrcode

import com.jetbrains.kmpapp.mvi.MviIntent
import com.jetbrains.kmpapp.mvi.MviState
import com.jetbrains.kmpapp.mvi.SideEffect

data class QRCodeState(
    val isLoading: Boolean = true,
    val error: String = "",
    val userId: String = "",
    val userQRCode: ByteArray? = null,
    val isScanMode: Boolean = false,
    val isScanning: Boolean = false,
    val scannedUserId: String = ""
) : MviState {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is QRCodeState) return false

        if (isLoading != other.isLoading) return false
        if (error != other.error) return false
        if (userId != other.userId) return false
        if (userQRCode != null) {
            if (other.userQRCode == null) return false
            if (!userQRCode.contentEquals(other.userQRCode)) return false
        } else if (other.userQRCode != null) return false
        if (isScanMode != other.isScanMode) return false
        if (isScanning != other.isScanning) return false
        if (scannedUserId != other.scannedUserId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isLoading.hashCode()
        result = 31 * result + error.hashCode()
        result = 31 * result + userId.hashCode()
        result = 31 * result + (userQRCode?.contentHashCode() ?: 0)
        result = 31 * result + isScanMode.hashCode()
        result = 31 * result + isScanning.hashCode()
        result = 31 * result + scannedUserId.hashCode()
        return result
    }
}

sealed class QRCodeIntent : MviIntent {
    object LoadUserData : QRCodeIntent()
    data class SetScanMode(val scanMode: Boolean) : QRCodeIntent()
    object StartQRScan : QRCodeIntent()
    object StopQRScan : QRCodeIntent()
    data class QRCodeScanned(val userId: String) : QRCodeIntent()
    data class SetError(val error: String) : QRCodeIntent()
    data class QRCodeGenerated(val qrCodeData: ByteArray) : QRCodeIntent()
}

sealed class QRCodeSideEffect : SideEffect {
    data class NavigateToTransfer(val userId: String) : QRCodeSideEffect()
    object ShowErrorToast : QRCodeSideEffect()
}