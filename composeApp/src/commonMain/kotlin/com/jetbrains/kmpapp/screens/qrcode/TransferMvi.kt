package com.jetbrains.kmpapp.screens.qrcode

import com.jetbrains.kmpapp.data.Product
import com.jetbrains.kmpapp.mvi.MviIntent
import com.jetbrains.kmpapp.mvi.MviState
import com.jetbrains.kmpapp.mvi.SideEffect

data class TransferState(
    val isLoading: Boolean = false,
    val error: String = "",
    val recipientId: String = "",
    val recipientName: String = "",
    val userProducts: List<Product> = emptyList(),
    val selectedProduct: Product? = null,
    val isRfidScanning: Boolean = false,
    val scannedRfidId: String = "",
    val isRfidVerified: Boolean = false,
    val isTransferring: Boolean = false,
    val transferProgress: Float = 0f,
    val transferCompleted: Boolean = false,
    val confirmationCode: String = "",
    val userEnteredCode: String = "",
    val showConfirmationCodeUI: Boolean = false,
    val transferPermissionExpiryTime: Long = 0,
    val hasPhysicalVerification: Boolean = false,
    val physicalVerificationTimeRemaining: String = ""
) : MviState

sealed class TransferIntent : MviIntent {
    data class SetRecipientId(val recipientId: String) : TransferIntent()
    object LoadUserProducts : TransferIntent()
    data class SelectProduct(val productId: String) : TransferIntent()
    object ClearSelectedProduct : TransferIntent()
    object StartRfidScan : TransferIntent()
    object StopRfidScan : TransferIntent()
    data class RfidTagScanned(val tagData: String) : TransferIntent()
    data class ProcessConfirmationCode(val enteredCode: String) : TransferIntent()
    object ConfirmTransfer : TransferIntent()
    object ResetError : TransferIntent()
}

sealed class TransferSideEffect : SideEffect {
    object ShowNfcNotSupportedError : TransferSideEffect()
    object ShowNfcDisabledError : TransferSideEffect()
    data class ShowToast(val message: String) : TransferSideEffect()
    object NavigateBack : TransferSideEffect()
}

sealed class TransferRequest {
    data class GetRecipientData(val recipientId: String) : TransferRequest()
    object GetUserProducts : TransferRequest()
    data class GetProduct(val productId: String) : TransferRequest()
    data class VerifyOwnership(val productId: String, val userId: String) : TransferRequest()
    data class TransferOwnership(val productId: String, val recipientId: String) : TransferRequest()
    data class SyncProduct(val productId: String) : TransferRequest()
    data class ProcessNfcTag(val tagData: String) : TransferRequest()
}

sealed class TransferResponse {
    data class RecipientData(val name: String) : TransferResponse()
    data class UserProducts(val products: List<Product>) : TransferResponse()
    data class ProductData(val product: Product) : TransferResponse()
    data class OwnershipVerified(val isVerified: Boolean) : TransferResponse()
    data class TransferCompleted(val success: Boolean) : TransferResponse()
    data class Error(val message: String) : TransferResponse()
    data class TagData(val rfidId: String) : TransferResponse()
}