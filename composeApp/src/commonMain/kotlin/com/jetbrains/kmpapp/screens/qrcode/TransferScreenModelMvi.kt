package com.jetbrains.kmpapp.screens.qrcode

import com.jetbrains.kmpapp.data.PhysicalVerificationManager
import com.jetbrains.kmpapp.data.Product
import com.jetbrains.kmpapp.data.TransferNfcService
import com.jetbrains.kmpapp.data.TransferPermissionManager
import com.jetbrains.kmpapp.data.repository.TransferRepository
import com.jetbrains.kmpapp.mvi.MviModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class TransferScreenModelMvi(
    private val transferRepository: TransferRepository,
    private val nfcService: TransferNfcService
) : MviModel<TransferState, TransferIntent, TransferSideEffect>() {

    private val transferPermissionManager = TransferPermissionManager.getInstance()
    private val physicalVerificationManager = PhysicalVerificationManager.getInstance()
    private var currentConfirmationCode: String = ""
    private var tagCollectorActive = false

    init {
        setupTagDataCollector()
    }

    override fun initialState(): TransferState = TransferState()

    override suspend fun handleIntent(intent: TransferIntent) {
        when (intent) {
            is TransferIntent.SetRecipientId -> handleSetRecipientId(intent.recipientId)
            is TransferIntent.LoadUserProducts -> handleLoadUserProducts()
            is TransferIntent.SelectProduct -> handleSelectProduct(intent.productId)
            is TransferIntent.ClearSelectedProduct -> handleClearSelectedProduct()
            is TransferIntent.StartRfidScan -> handleStartRfidScan()
            is TransferIntent.StopRfidScan -> handleStopRfidScan()
            is TransferIntent.RfidTagScanned -> handleRfidTagScanned(intent.tagData)
            is TransferIntent.ProcessConfirmationCode -> handleProcessConfirmationCode(intent.enteredCode)
            is TransferIntent.ConfirmTransfer -> handleConfirmTransfer()
            is TransferIntent.ResetError -> handleResetError()
        }
    }

    private fun setupTagDataCollector() {
        if (tagCollectorActive) return

        tagCollectorActive = true
        launchInScope {
            try {
                nfcService.tagData.collectLatest { tagData ->
                    tagData?.let { data ->
                        updateState { it.copy(isRfidScanning = true) }
                        processIntent(TransferIntent.RfidTagScanned(data))
                    }
                }
            } catch (e: Exception) {
                tagCollectorActive = false
                setupTagDataCollector()
            }
        }
    }

    private suspend fun handleSetRecipientId(recipientId: String) {
        updateState { it.copy(recipientId = recipientId) }

        transferRepository.execute(TransferRequest.GetRecipientData(recipientId))
            .catch { error ->
                updateState { it.copy(error = error.message ?: "Failed to get recipient data") }
            }
            .collect { response ->
                when (response) {
                    is TransferResponse.RecipientData -> {
                        updateState { it.copy(recipientName = response.name) }
                    }
                    is TransferResponse.Error -> {
                        updateState { it.copy(error = response.message) }
                    }
                    else -> {}
                }
            }

        processIntent(TransferIntent.LoadUserProducts)
    }

    private suspend fun handleLoadUserProducts() {
        updateState { it.copy(isLoading = true, error = "") }
        
        transferRepository.execute(TransferRequest.GetUserProducts)
            .catch { error ->
                updateState { 
                    it.copy(
                        isLoading = false, 
                        error = error.message ?: "Failed to load user products"
                    )
                }
            }
            .collect { response ->
                when (response) {
                    is TransferResponse.UserProducts -> {
                        updateState { 
                            it.copy(
                                isLoading = false, 
                                userProducts = response.products,
                                error = if (response.products.isEmpty()) "No assets found to transfer" else ""
                            )
                        }
                    }
                    is TransferResponse.Error -> {
                        updateState { it.copy(isLoading = false, error = response.message) }
                    }
                    else -> {}
                }
            }
    }

    private suspend fun handleSelectProduct(productId: String) {
        val matchingProducts = state.value.userProducts.filter { p -> 
            p.id == productId 
        }
        
        if (matchingProducts.isEmpty()) {
            updateState { it.copy(error = "Selected product not found") }
            return
        }
        
        val product = matchingProducts.first()
        
        val hasCodeVerification = transferPermissionManager.hasValidCodeVerification(productId)
        val hasPhysicalVerification = physicalVerificationManager.hasRecentPhysicalVerification(productId)
        val hasFullVerification = hasCodeVerification && hasPhysicalVerification
        
        val expiryTime = if (hasFullVerification) {
            transferPermissionManager.getTransferPermissionExpiry(productId)
        } else 0L
        
        val physicalTimeRemaining = transferPermissionManager.getPhysicalVerificationTimeRemaining(productId)
        
        if (hasFullVerification) {
            updateState {
                it.copy(
                    selectedProduct = product, 
                    isRfidVerified = true,
                    transferPermissionExpiryTime = expiryTime,
                    hasPhysicalVerification = true,
                    physicalVerificationTimeRemaining = physicalTimeRemaining,
                    error = ""
                )
            }
        } else {
            generateConfirmationCode()
            
            val errorMessage = when {
                !hasPhysicalVerification -> 
                    "This asset needs physical verification. Please scan it using NFC Verification first."
                !hasCodeVerification -> 
                    "Please enter the confirmation code to verify your transfer intent."
                else -> ""
            }
            
            updateState {
                it.copy(
                    selectedProduct = product, 
                    isRfidVerified = false,
                    showConfirmationCodeUI = true,
                    hasPhysicalVerification = hasPhysicalVerification,
                    physicalVerificationTimeRemaining = physicalTimeRemaining,
                    error = errorMessage
                )
            }
        }
    }

    private fun generateConfirmationCode() {
        currentConfirmationCode = transferPermissionManager.generateConfirmationCode()
        
        updateState { 
            it.copy(
                confirmationCode = currentConfirmationCode,
                userEnteredCode = "",
                showConfirmationCodeUI = true
            )
        }
    }

    private fun handleClearSelectedProduct() {
        updateState { it.copy(selectedProduct = null) }
    }

    private fun handleStartRfidScan() {
        if (!checkNfcCapabilities()) return
        
        generateConfirmationCode()
        
        updateState {
            it.copy(showConfirmationCodeUI = true, error = "")
        }
    }

    private fun checkNfcCapabilities(): Boolean {
        if (!nfcService.isNfcSupported()) {
            updateState { it.copy(error = "NFC is not supported on this device") }
            launchInScope {
                emitSideEffect(TransferSideEffect.ShowNfcNotSupportedError)
            }
            return false
        }

        if (!nfcService.isNfcEnabled()) {
            updateState { it.copy(error = "NFC is not enabled. Please enable it in your device settings.") }
            launchInScope {
                emitSideEffect(TransferSideEffect.ShowNfcDisabledError)
            }
            return false
        }

        return true
    }

    private fun handleStopRfidScan() {
        updateState { it.copy(showConfirmationCodeUI = false) }
    }

    private suspend fun handleRfidTagScanned(tagData: String) {
        transferRepository.execute(TransferRequest.ProcessNfcTag(tagData))
            .catch { error ->
                updateState { 
                    it.copy(
                        isRfidScanning = false, 
                        error = error.message ?: "Error processing NFC tag"
                    )
                }
            }
            .collect { response ->
                when (response) {
                    is TransferResponse.TagData -> {
                        val rfidId = response.rfidId
                        updateState { 
                            it.copy(
                                scannedRfidId = rfidId, 
                                isRfidScanning = false
                            )
                        }
                        
                        nfcService.stopTransferScan()
                        
                        if (state.value.selectedProduct == null) {
                            handleInitialRfidScan(rfidId)
                        } else {
                            verifyRfidForSelectedProduct(rfidId)
                        }
                    }
                    
                    is TransferResponse.Error -> {
                        updateState { 
                            it.copy(
                                isRfidScanning = false, 
                                error = response.message
                            )
                        }
                    }
                    
                    else -> {}
                }
            }
    }

    private suspend fun handleInitialRfidScan(rfidId: String) {
        val matchingProducts = state.value.userProducts.filter { p -> 
            p.id == rfidId 
        }
        
        if (matchingProducts.isNotEmpty()) {
            selectVerifiedProduct(matchingProducts.first())
            return
        }
        
        transferRepository.execute(TransferRequest.GetProduct(rfidId))
            .catch { error ->
                updateState { it.copy(error = error.message ?: "Error fetching product details") }
            }
            .collect { response ->
                when (response) {
                    is TransferResponse.ProductData -> {
                        selectVerifiedProduct(response.product)
                    }
                    
                    is TransferResponse.Error -> {
                        syncAndCheckProduct(rfidId)
                    }
                    
                    else -> {}
                }
            }
    }

    private suspend fun syncAndCheckProduct(rfidId: String) {
        transferRepository.execute(TransferRequest.SyncProduct(rfidId))
            .catch { error ->
                updateState { it.copy(error = error.message ?: "Error syncing product") }
            }
            .collect { response ->
                when (response) {
                    is TransferResponse.ProductData -> {
                        selectVerifiedProduct(response.product)
                    }
                    
                    is TransferResponse.Error -> {
                        updateState { it.copy(error = "Asset not found or not owned by you") }
                    }
                    
                    else -> {}
                }
            }
    }

    private suspend fun verifyRfidForSelectedProduct(rfidId: String) {
        val selectedProduct = state.value.selectedProduct
        
        if (selectedProduct == null) {
            updateState { it.copy(error = "No product selected") }
            return
        }
        
        if (selectedProduct.id != rfidId) {
            updateState { it.copy(error = "The scanned tag doesn't match the selected asset") }
            return
        }
        
        launchInScope {
            try {
                physicalVerificationManager.recordPhysicalVerification(rfidId)
                updateState { 
                    it.copy(
                        isRfidVerified = true, 
                        hasPhysicalVerification = true,
                        physicalVerificationTimeRemaining = transferPermissionManager.getPhysicalVerificationTimeRemaining(rfidId),
                        error = ""
                    )
                }
            } catch (e: Exception) {
                updateState { it.copy(error = e.message ?: "Error verifying ownership") }
            }
        }
    }

    private fun selectVerifiedProduct(product: Product) {
        updateState {
            it.copy(
                selectedProduct = product,
                isRfidVerified = true,
                hasPhysicalVerification = true,
                physicalVerificationTimeRemaining = transferPermissionManager.getPhysicalVerificationTimeRemaining(product.id),
                error = ""
            )
        }
        
        physicalVerificationManager.recordPhysicalVerification(product.id)
    }

    private fun handleProcessConfirmationCode(enteredCode: String) {
        updateState { it.copy(userEnteredCode = enteredCode) }
        
        if (enteredCode.equals(currentConfirmationCode, ignoreCase = true)) {
            val productId = state.value.selectedProduct?.id ?: return
            transferPermissionManager.markAssetVerified(productId)
            
            val expiryTime = transferPermissionManager.getTransferPermissionExpiry(productId)
            
            updateState { 
                it.copy(
                    isRfidVerified = true,
                    showConfirmationCodeUI = false,
                    transferPermissionExpiryTime = expiryTime,
                    error = ""
                )
            }
        } else {
            if (enteredCode.length == currentConfirmationCode.length) {
                updateState { it.copy(error = "Incorrect confirmation code. Please try again.") }
            }
        }
    }

    private suspend fun handleConfirmTransfer() {
        val state = this.state.value
        
        if (state.selectedProduct == null) {
            updateState { it.copy(error = "No product selected") }
            return
        }
        
        if (!state.isRfidVerified) {
            updateState { it.copy(error = "Please verify the asset with confirmation code first") }
            return
        }
        
        val productId = state.selectedProduct.id
        if (!physicalVerificationManager.hasRecentPhysicalVerification(productId)) {
            updateState { 
                it.copy(
                    error = "Physical verification expired or missing. Please use NFC Verification screen to physically verify this asset.",
                    hasPhysicalVerification = false
                ) 
            }
            return
        }
        
        startTransferAnimation()
        
        transferRepository.execute(TransferRequest.TransferOwnership(productId, state.recipientId))
            .catch { error ->
                updateState { 
                    it.copy(
                        isTransferring = false, 
                        error = error.message ?: "Transfer failed" 
                    )
                }
            }
            .collect { response ->
                when (response) {
                    is TransferResponse.TransferCompleted -> {
                        completeTransferAnimation()
                        updateState { 
                            it.copy(
                                isTransferring = false,
                                transferCompleted = true,
                                error = ""
                            )
                        }
                        processIntent(TransferIntent.LoadUserProducts)
                    }
                    
                    is TransferResponse.Error -> {
                        updateState { 
                            it.copy(
                                isTransferring = false,
                                error = response.message
                            )
                        }
                    }
                    
                    else -> {}
                }
            }
    }

    private fun startTransferAnimation() {
        updateState { it.copy(isTransferring = true, transferProgress = 0f, error = "") }
        
        launchInScope {
            for (i in 1..5) {
                kotlinx.coroutines.delay(200)
                updateState { it.copy(transferProgress = i / 10f) }
            }
        }
    }

    private suspend fun completeTransferAnimation() {
        for (i in 6..10) {
            kotlinx.coroutines.delay(100)
            updateState { it.copy(transferProgress = i / 10f) }
        }
    }

    private fun handleResetError() {
        updateState { it.copy(error = "") }
    }

    override fun onDispose() {
        super.onDispose()
        
        updateState {
            it.copy(
                showConfirmationCodeUI = false, 
                userEnteredCode = "",
                confirmationCode = ""
            )
        }
        
        currentConfirmationCode = ""
    }
}