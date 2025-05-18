package com.jetbrains.kmpapp.screens.qrcode

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.jetbrains.kmpapp.data.FirebaseService
import com.jetbrains.kmpapp.data.NfcService
import com.jetbrains.kmpapp.data.PhysicalVerificationManager
import com.jetbrains.kmpapp.data.Product
import com.jetbrains.kmpapp.data.ProductService
import com.jetbrains.kmpapp.data.TransferNfcService
import com.jetbrains.kmpapp.data.TransferPermissionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class TransferUiState(
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
)

class TransferScreenModel(
    private val firebaseService: FirebaseService,
    private val productService: ProductService,
    val nfcService: TransferNfcService
) : ScreenModel {
    private val transferPermissionManager = TransferPermissionManager.getInstance()
    private val physicalVerificationManager = PhysicalVerificationManager.getInstance()
    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()
    private var currentConfirmationCode: String = ""

    fun setRecipientId(recipientId: String) {
        _uiState.update { it.copy(recipientId = recipientId) }
        loadRecipientData(recipientId)
        loadUserProducts()
    }

    private fun loadRecipientData(recipientId: String) {
        screenModelScope.launch {
            firebaseService.getUserData(recipientId)
                .onSuccess { userData -> 
                    userData?.let { user ->
                        _uiState.update { it.copy(recipientName = user.displayName) }
                    }
                }
        }
    }

    private fun loadUserProducts() {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = "") }
            
            val userId = firebaseService.getCurrentUserId() ?: run {
                _uiState.update { it.copy(isLoading = false, error = "User not signed in") }
                return@launch
            }
            
            runCatching {
                fetchUserProductsWithTimeout(userId, 8000)?.let { result ->
                    if (result.isSuccess && result.getOrNull()?.isNotEmpty() == true) {
                        return@runCatching result
                    }
                }
                
                withTimeoutOrNull(10000) { 
                    productService.syncAllBlockchainProducts(userId) 
                }
                
                fetchUserProductsWithTimeout(userId, 5000) 
                    ?: Result.failure(Exception("Timeout fetching products after sync"))
            }.onSuccess { result ->
                handleSuccessfulProductFetch(result, userId)
            }.onFailure { error ->
                handleProductFetchFailure(error, userId)
            }
        }
    }
    
    private suspend fun fetchUserProductsWithTimeout(userId: String, timeoutMs: Long): Result<List<Product>>? {
        return withTimeoutOrNull(timeoutMs) {
            productService.getUserProducts(userId)
        }
    }
    
    private suspend fun handleSuccessfulProductFetch(result: Result<List<Product>>, userId: String) {
        result.getOrNull()?.let { products ->
            if (products.isNotEmpty()) {
                updateStateWithProducts(products)
                return
            }
            
            tryBlockchainProductRecovery(userId)
        }
    }
    
    private suspend fun tryBlockchainProductRecovery(userId: String) {
        try {
            val blockchainAssetIds = fetchBlockchainAssetIds(userId)
            
            if (blockchainAssetIds.isEmpty()) {
                updateStateWithProducts(emptyList())
                return
            }
            
            val syncedProducts = blockchainAssetIds.mapNotNull { assetId ->
                syncAndFetchSingleProduct(assetId)
            }
            
            updateStateWithProducts(syncedProducts)
        } catch (e: Exception) {
            updateStateWithProducts(emptyList())
        }
    }
    
    private suspend fun fetchBlockchainAssetIds(userId: String): List<String> {
        return withTimeoutOrNull(5000) {
            productService.getBlockchainProducts(userId).getOrNull() ?: emptyList()
        } ?: emptyList()
    }
    
    private suspend fun syncAndFetchSingleProduct(assetId: String): Product? {
        return try {
            var product: Product? = null
            productService.forceProductSync(assetId)
                .onSuccess {
                    productService.getProduct(assetId)
                        .onSuccess { fetchedProduct ->
                            product = fetchedProduct
                        }
                }
            product
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun handleProductFetchFailure(error: Throwable, userId: String) {
        try {
            val blockchainAssetIds = fetchBlockchainAssetIds(userId)
            
            if (blockchainAssetIds.isEmpty()) {
                updateStateWithError(error.message ?: "Failed to load your assets")
                return
            }
            
            val syncCount = withTimeoutOrNull(15000) {
                productService.syncAllBlockchainProducts(userId).getOrNull() ?: 0
            } ?: 0
            
            if (syncCount > 0) {
                val products = withTimeoutOrNull(5000) {
                    productService.getUserProducts(userId).getOrNull() ?: emptyList()
                } ?: emptyList()
                
                val errorMsg = if (products.isEmpty()) "Synced $syncCount assets but no products found" else ""
                _uiState.update { it.copy(isLoading = false, userProducts = products, error = errorMsg) }
            } else {
                updateStateWithError(error.message ?: "Failed to load your assets")
            }
        } catch (e: Exception) {
            updateStateWithError("Failed to load assets: ${e.message}")
        }
    }
    
    private fun updateStateWithProducts(products: List<Product>) {
        _uiState.update {
            it.copy(
                isLoading = false,
                userProducts = products,
                error = if (products.isEmpty()) "No assets found to transfer" else ""
            )
        }
    }
    
    private fun updateStateWithError(errorMessage: String) {
        _uiState.update { it.copy(isLoading = false, error = errorMessage) }
    }

    fun selectProduct(productId: String) {
        val product = _uiState.value.userProducts.find { it.id == productId }
        if (product != null) {
            val hasCodeVerification = transferPermissionManager.hasValidCodeVerification(productId)
            
            val hasPhysicalVerification = physicalVerificationManager.hasRecentPhysicalVerification(productId)
            
            val hasFullVerification = hasCodeVerification && hasPhysicalVerification
            
            val expiryTime = if (hasFullVerification) {
                transferPermissionManager.getTransferPermissionExpiry(productId)
            } else 0L
            
            val physicalTimeRemaining = transferPermissionManager.getPhysicalVerificationTimeRemaining(productId)
            
            if (hasFullVerification) {
                _uiState.update {
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
                
                _uiState.update {
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
        } else {
            _uiState.update {
                it.copy(error = "Selected product not found")
            }
        }
    }

    fun clearSelectedProduct() {
        _uiState.update { it.copy(selectedProduct = null) }
    }

    private var tagCollectorActive = false

    init {
        setupTagDataCollector()
    }

    private fun setupTagDataCollector() {
        if (tagCollectorActive) return

        tagCollectorActive = true
        screenModelScope.launch {
            try {
                nfcService.tagData.collect { tagData ->
                    tagData?.let { data ->
                        if (!_uiState.value.isRfidScanning) {
                            _uiState.update { it.copy(isRfidScanning = true) }
                        }

                        handleReceivedTagData(data)
                    }
                }
            } catch (e: Exception) {
                tagCollectorActive = false
                setupTagDataCollector()
            }
        }
    }

    private fun generateConfirmationCode() {
        currentConfirmationCode = transferPermissionManager.generateConfirmationCode()
        
        _uiState.update { 
            it.copy(
                confirmationCode = currentConfirmationCode,
                userEnteredCode = "",
                showConfirmationCodeUI = true
            )
        }
    }
    
    fun processConfirmationCode(enteredCode: String) {
        _uiState.update { it.copy(userEnteredCode = enteredCode) }
        
        if (enteredCode.equals(currentConfirmationCode, ignoreCase = true)) {
            val productId = _uiState.value.selectedProduct?.id ?: return
            transferPermissionManager.markAssetVerified(productId)
            
            val expiryTime = transferPermissionManager.getTransferPermissionExpiry(productId)
            
            _uiState.update { 
                it.copy(
                    isRfidVerified = true,
                    showConfirmationCodeUI = false,
                    transferPermissionExpiryTime = expiryTime
                )
            }
        } else {
            if (enteredCode.length == currentConfirmationCode.length) {
                _uiState.update { it.copy(error = "Incorrect confirmation code. Please try again.") }
            }
        }
    }
    
    fun startRfidScan() {
        generateConfirmationCode()
        
        _uiState.update {
            it.copy(showConfirmationCodeUI = true, error = "")
        }
    }

    private fun checkNfcCapabilities(): Boolean {
        if (!nfcService.isNfcSupported()) {
            updateStateWithScanError("NFC is not supported on this device")
            return false
        }

        if (!nfcService.isNfcEnabled()) {
            updateStateWithScanError("NFC is not enabled. Please enable it in your device settings.")
            return false
        }

        return true
    }

    private fun handleReceivedTagData(tagData: String) {
        val rfidId = nfcService.extractRfidIdFromTagData(tagData)

        if (rfidId.isEmpty()) {
            _uiState.update {
                it.copy(error = "Could not identify the NFC tag - please try again", isRfidScanning = false)
            }
            return
        }

        _uiState.update {
            it.copy(scannedRfidId = rfidId, isRfidScanning = false)
        }

        nfcService.stopTransferScan()

        screenModelScope.launch {
            if (_uiState.value.selectedProduct == null) {
                handleInitialRfidScan(rfidId)
            } else {
                verifyRfidForSelectedProduct(rfidId)
            }
        }
    }
    
    private fun handleScanStartFailure(error: Throwable) {
        val errorMessage = when (error.message) {
            "NFC is not enabled" -> "Please enable NFC in your device settings to use this feature."
            "NFC is not supported on this device" -> "Sorry, your device doesn't support NFC functionality."
            else -> "Failed to start NFC scan: ${error.message}"
        }
        
        updateStateWithScanError(errorMessage)
    }
    
    private fun updateStateWithScanError(errorMessage: String) {
        _uiState.update {
            it.copy(
                isRfidScanning = false,
                error = errorMessage
            )
        }
    }

    private fun handleInitialRfidScan(rfidId: String) {
        screenModelScope.launch {
            try {
                _uiState.update { it.copy(isRfidScanning = false) }
                
                _uiState.value.userProducts.find { it.id == rfidId }?.let { product ->
                    selectVerifiedProduct(product)
                    return@launch
                }

                val fetchResult = productService.getProduct(rfidId)
                
                if (fetchResult.isSuccess && fetchResult.getOrNull() != null) {
                    val fetchedProduct = fetchResult.getOrNull()!!
                    val currentUserId = firebaseService.getCurrentUserId() ?: return@launch

                    if (fetchedProduct.currentOwner == currentUserId) {
                        _uiState.update {
                            it.copy(
                                selectedProduct = fetchedProduct,
                                isRfidVerified = true,
                                error = ""
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(error = "This asset doesn't belong to you")
                        }
                    }
                    return@launch
                }

                try {
                    productService.forceProductSync(rfidId).fold(
                        onSuccess = {
                            productService.getProduct(rfidId).fold(
                                onSuccess = { product ->
                                    if (product != null) {
                                        val currentUserId = firebaseService.getCurrentUserId() ?: return@fold
                                        if (product.currentOwner == currentUserId) {
                                            selectVerifiedProduct(product)
                                            return@launch
                                        } else {
                                            _uiState.update { it.copy(error = "Asset found but you're not the owner") }
                                        }
                                    } else {
                                        _uiState.update { it.copy(error = "Asset not recognized. Please try again or select from the list.") }
                                    }
                                },
                                onFailure = {
                                    _uiState.update { it.copy(error = "Asset not recognized after sync") }
                                }
                            )
                        },
                        onFailure = {
                            _uiState.update { it.copy(error = "Asset not found on the blockchain") }
                        }
                    )
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = "Error syncing from blockchain: ${e.message}") }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Error processing scanned tag")
                }
            }
        }
    }

    private fun verifyRfidForSelectedProduct(rfidId: String) {
        screenModelScope.launch {
            _uiState.update { it.copy(isRfidScanning = false) }
            
            try {
                val selectedProduct = _uiState.value.selectedProduct
                
                when {
                    selectedProduct == null -> {
                        _uiState.update { it.copy(error = "No product selected") }
                        return@launch
                    }
                    selectedProduct.id != rfidId -> {
                        _uiState.update { it.copy(error = "The scanned tag doesn't match the selected asset") }
                        return@launch
                    }
                }
                
                verifyOwnership(rfidId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error verifying scanned tag") }
            }
        }
    }
    
    private suspend fun verifyOwnership(productId: String) {
        try {
            val currentUserId = firebaseService.getCurrentUserId() ?: return
            
            productService.verifyProductOwnership(productId, currentUserId).fold(
                onSuccess = { isOwner ->
                    if (isOwner) {
                        _uiState.update { it.copy(isRfidVerified = true, error = "") }
                    } else {
                        _uiState.update { 
                            it.copy(error = "Ownership verification failed. This asset may have been transferred to another user.")
                        }
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isRfidVerified = true, error = "") }
                }
            )
        } catch (e: Exception) {
            _uiState.update { it.copy(isRfidVerified = true, error = "") }
        }
    }

    fun stopRfidScan() {
        _uiState.update { it.copy(showConfirmationCodeUI = false) }
    }

    fun confirmTransfer() {
        screenModelScope.launch {
            val state = _uiState.value
            
            when {
                state.selectedProduct == null -> {
                    _uiState.update { it.copy(error = "No product selected") }
                    return@launch
                }
                !state.isRfidVerified -> {
                    _uiState.update { it.copy(error = "Please verify the asset with confirmation code first") }
                    return@launch
                }
            }
            
            val productId = state.selectedProduct.id
            if (!physicalVerificationManager.hasRecentPhysicalVerification(productId)) {
                _uiState.update { 
                    it.copy(
                        error = "Physical verification expired or missing. Please use NFC Verification screen to physically verify this asset.",
                        hasPhysicalVerification = false
                    ) 
                }
                return@launch
            }
            
            try {
                startTransferProcess()
                
                val currentUserId = firebaseService.getCurrentUserId() ?: run {
                    _uiState.update { it.copy(isTransferring = false, error = "User not signed in") }
                    return@launch
                }
                
                _uiState.update { it.copy(transferProgress = 0.5f) }
                
                executeTransfer(state.selectedProduct.id, state.recipientId)
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isTransferring = false, error = e.message ?: "Transfer failed") 
                }
            }
        }
    }
    
    private suspend fun startTransferProcess() {
        _uiState.update { 
            it.copy(isTransferring = true, transferProgress = 0f, error = "") 
        }
        
        for (i in 1..5) {
            kotlinx.coroutines.delay(200)
            _uiState.update { it.copy(transferProgress = i / 10f) }
        }
    }
    
    private suspend fun executeTransfer(productId: String, recipientId: String) {
        try {
            val currentUserId = firebaseService.getCurrentUserId() ?: run {
                _uiState.update { it.copy(isTransferring = false, error = "Current user ID not available") }
                return
            }

            _uiState.update { it.copy(transferProgress = 0.4f, error = "Verifying blockchain ownership...") }
            
            val transferResult = withTimeoutOrNull(30000) {
                productService.transferProductOwnership(productId, recipientId)
            } ?: Result.failure(Exception("Transfer operation timed out"))
            
            transferResult.fold(
                onSuccess = {
                    completeTransferAnimation()

                    _uiState.update {
                        it.copy(
                            isTransferring = false, 
                            transferCompleted = true, 
                            error = ""
                        )
                    }

                    loadUserProducts()
                },
                onFailure = { error ->
                    val errorMessage = when {
                        error.message?.contains("not the owner") == true -> 
                            "Transfer failed: You are not the owner of this asset on the blockchain"
                        error.message?.contains("timeout") == true -> 
                            "Transfer timed out: Blockchain service may be unavailable"
                        else -> "Transfer failed: ${error.message}"
                    }
                    
                    _uiState.update {
                        it.copy(
                            isTransferring = false, 
                            transferCompleted = false,
                            error = errorMessage
                        )
                    }
                }
            )
        } catch (e: Exception) {
            _uiState.update {
                it.copy(isTransferring = false, error = "Transfer error: ${e.message}")
            }
        }
    }
    
    private suspend fun verifyBlockchainTransferResult(productId: String, newOwnerId: String) {
        try {
            kotlinx.coroutines.delay(1500)
            
            val product = withTimeoutOrNull(3000) {
                productService.getProduct(productId).getOrNull()
            }
            
            if (product == null) {
                _uiState.update { 
                    it.copy(error = "Transfer completed, but could not verify blockchain status")
                }
                return
            }
            
            if (!product.isRegisteredOnBlockchain) {
                _uiState.update { 
                    it.copy(error = "Transfer completed successfully in local database only")
                }
                return
            }
            
            val verificationResult = withTimeoutOrNull(5000) {
                productService.verifyProductOwnership(productId, newOwnerId)
            }
            
            if (verificationResult == null) {
                _uiState.update { 
                    it.copy(error = "Transfer completed but blockchain verification timed out")
                }
                return
            }
            
            verificationResult.fold(
                onSuccess = { isVerified ->
                    if (isVerified) {
                        _uiState.update { it.copy(error = "") }
                    } else {
                        _uiState.update { 
                            it.copy(error = "Transfer completed but blockchain needs more time to update")
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(error = "Transfer completed but blockchain verification encountered an error")
                    }
                }
            )
        } catch (e: Exception) {
            _uiState.update { 
                it.copy(error = "Transfer completed successfully, blockchain status pending")
            }
        }
    }
    
    private suspend fun completeTransferAnimation() {
        for (i in 6..10) {
            kotlinx.coroutines.delay(100)
            _uiState.update { it.copy(transferProgress = i / 10f) }
        }
    }

    fun resetError() {
        _uiState.update { it.copy(error = "") }
    }
    
    private fun selectVerifiedProduct(product: Product) {
        _uiState.update {
            it.copy(
                selectedProduct = product,
                isRfidVerified = true,
                error = ""
            )
        }
    }

    override fun onDispose() {
        _uiState.update {
            it.copy(
                showConfirmationCodeUI = false, 
                userEnteredCode = "",
                confirmationCode = ""
            )
        }
        
        currentConfirmationCode = ""
    }
}