package com.jetbrains.kmpapp.screens.assets

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.jetbrains.kmpapp.data.BlockchainService
import com.jetbrains.kmpapp.data.FirebaseService
import com.jetbrains.kmpapp.data.Product
import com.jetbrains.kmpapp.data.ProductService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class AssetsUiState(
    val isLoading: Boolean = false,
    val assets: List<Product> = emptyList(),
    val error: String = "",
    val isFetchingFromBlockchain: Boolean = false,
    val blockhainAssetIds: List<String> = emptyList(),
    val recoveryAttempted: Boolean = false,
    val userBalance: Int = 0
)

class AssetsScreenModel(
    private val firebaseService: FirebaseService,
    private val productService: ProductService,
    private val blockchainService: BlockchainService
) : ScreenModel {


    fun getUserBalance() {
        screenModelScope.launch {
            try {
                val userId = firebaseService.getCurrentUserId() ?: return@launch

                blockchainService.getUserBalance(userId)
                    .onSuccess { balance ->
                        _uiState.update { it.copy(userBalance = balance) }
                    }
            } catch (e: Exception) {
            }
        }
    }

    private val _uiState = MutableStateFlow(AssetsUiState(isLoading = true))
    val uiState: StateFlow<AssetsUiState> = _uiState.asStateFlow()

    init {
        loadUserAssets()
        getUserBalance()
    }

    fun loadUserAssets() {
        screenModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = "") }

                val userId = firebaseService.getCurrentUserId()
                if (userId == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "User not signed in"
                        )
                    }
                    return@launch
                }

                loadBlockchainAssetIds(userId, false)

                try {
                    withTimeoutOrNull(5000) {
                        productService.syncAllBlockchainProducts(userId)
                    }

                    val productsResult = withTimeoutOrNull(8000) {
                        productService.getUserProducts(userId)
                    } ?: Result.failure(Exception("Timeout fetching products from Firebase"))

                    productsResult.onSuccess { products ->
                        if (products.isNotEmpty()) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    assets = products
                                )
                            }

                        } else {
                            loadFromBlockchain(userId)
                        }
                    }.onFailure { _ ->
                        loadFromBlockchain(userId)
                    }
                } catch (e: Exception) {
                    loadFromBlockchain(userId)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "An unexpected error occurred"
                    )
                }
            }
        }
    }

    private fun loadFromBlockchain(userId: String) {
        screenModelScope.launch {
            try {
                _uiState.update { it.copy(
                    isLoading = false,
                    isFetchingFromBlockchain = true,
                    error = "No assets found in Firebase, checking blockchain..."
                ) }

                loadBlockchainAssetIds(userId, true)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isFetchingFromBlockchain = false,
                        error = "Error checking blockchain: ${e.message}"
                    )
                }
            }
        }
    }

    private fun loadBlockchainAssetIds(userId: String, shouldSync: Boolean) {
        screenModelScope.launch {
            try {
                blockchainService.getUserAssets(userId)
                    .onSuccess { assetIds ->
                        _uiState.update { it.copy(blockhainAssetIds = assetIds) }

                        if (assetIds.isNotEmpty() && shouldSync) {
                            syncAssetsFromBlockchain(userId, assetIds)
                        } else if (assetIds.isEmpty() && shouldSync) {
                            _uiState.update {
                                it.copy(
                                    isFetchingFromBlockchain = false,
                                    error = "No assets found in blockchain either"
                                )
                            }
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isFetchingFromBlockchain = false,
                                error = "Error checking blockchain: ${error.message}"
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isFetchingFromBlockchain = false,
                        error = "Exception checking blockchain: ${e.message}"
                    )
                }
            }
        }
    }

    private suspend fun syncAssetsFromBlockchain(userId: String, assetIds: List<String>) {
        try {
            val syncedProducts = mutableListOf<Product>()
            var syncCount = 0

            for (assetId in assetIds) {
                try {
                    val nodeData = withTimeoutOrNull(5000) {
                        blockchainService.getAssetNodeData(assetId).getOrNull()
                    }

                    val history = withTimeoutOrNull(5000) {
                        blockchainService.getAssetHistory(assetId).getOrNull()
                    } ?: emptyList()

                    if (history.isEmpty()) {
                        continue
                    }

                    val lastRecord = history.lastOrNull()
                    val isOwner = lastRecord?.user_id == userId
                    if (!isOwner) {
                        continue
                    }

                    val ownershipResult = withTimeoutOrNull(5000) {
                        blockchainService.verifyOwnership(assetId, userId)
                    }
                    if (ownershipResult?.getOrDefault(false) != true) {
                        continue
                    }

                    val syncResult = withTimeoutOrNull(10000) {
                        productService.forceProductSync(assetId)
                    }

                    if (syncResult?.isSuccess == true) {
                        delay(500)

                        val product = withTimeoutOrNull(5000) {
                            productService.getProduct(assetId).getOrNull()
                        }

                        if (product != null) {
                            if (product.currentOwner != userId) {
                                try {
                                    val updatedProduct = product.copy(currentOwner = userId)
                                    productService.updateProduct(updatedProduct)
                                } catch (e: Exception) {
                                }
                            }

                            syncedProducts.add(product)
                            syncCount++
                        }
                    }
                } catch (e: Exception) {
                }
            }

            if (syncedProducts.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        isFetchingFromBlockchain = false,
                        assets = syncedProducts,
                        error = if (syncCount < assetIds.size)
                            "Recovered $syncCount of ${assetIds.size} assets from blockchain"
                        else
                            "",
                        recoveryAttempted = true
                    )
                }


                delay(1000)
                refreshFromFirebase(userId)
            } else {
                _uiState.update {
                    it.copy(
                        isFetchingFromBlockchain = false,
                        error = "Failed to recover any assets from blockchain",
                        recoveryAttempted = true
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isFetchingFromBlockchain = false,
                    error = "Error syncing from blockchain: ${e.message}",
                    recoveryAttempted = true
                )
            }
        }
    }

    private suspend fun refreshFromFirebase(userId: String) {
        try {
            val productsResult = withTimeoutOrNull(5000) {
                productService.getUserProducts(userId)
            }

            productsResult?.onSuccess { products ->
                if (products.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            assets = products,
                            error = ""
                        )
                    }

                }
            }
        } catch (e: Exception) {
        }
    }

    fun checkBlockchainAssets() {
        screenModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = "") }

                val currentUserId = firebaseService.getCurrentUserId() ?: return@launch

                blockchainService.getUserAssets(currentUserId)
                    .onSuccess { assets ->
                        if (assets.isEmpty()) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "No assets found on blockchain"
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    blockhainAssetIds = assets,
                                    error = "Found ${assets.size} assets on blockchain: ${assets.joinToString()}"
                                )
                            }
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Blockchain error: ${error.message}"
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    fun syncFromBlockchain(assetId: String) {
        screenModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = "") }

                productService.forceProductSync(assetId)
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Sync successful for asset $assetId. Try refreshing now."
                            )
                        }

                        delay(1000)
                        loadUserAssets()
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Sync failed for asset $assetId: ${error.message}"
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    fun syncAllFromBlockchain() {
        screenModelScope.launch {
            try {
                val userId = firebaseService.getCurrentUserId() ?: return@launch
                _uiState.update { it.copy(isLoading = true, error = "Checking blockchain for assets...") }

                val syncResult = withTimeoutOrNull(8000) {
                    productService.syncAllBlockchainProducts(userId)
                }

                if (syncResult != null && syncResult.getOrDefault(0) > 0) {
                        delay(1000)
                    val productsResult = productService.getUserProducts(userId)
                    productsResult.onSuccess { products ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isFetchingFromBlockchain = false,
                                assets = products,
                                error = "Synced ${syncResult.getOrDefault(0)} assets from blockchain"
                            )
                        }


                        return@launch
                    }
                }

                val assetIds = _uiState.value.blockhainAssetIds

                if (assetIds.isEmpty()) {
                    blockchainService.getUserAssets(userId)
                        .onSuccess { blockchainAssets ->
                            if (blockchainAssets.isNotEmpty()) {
                                _uiState.update {
                                    it.copy(
                                        isFetchingFromBlockchain = true,
                                        blockhainAssetIds = blockchainAssets,
                                        isLoading = false
                                    )
                                }
                                syncAssetsFromBlockchain(userId, blockchainAssets)
                            } else {
                                _uiState.update {
                                    it.copy(
                                        error = "No assets found on blockchain",
                                        isLoading = false
                                    )
                                }
                            }
                        }
                        .onFailure { error ->
                            _uiState.update {
                                it.copy(
                                    error = "Failed to check blockchain assets: ${error.message}",
                                    isLoading = false
                                )
                            }
                        }
                } else {
                    _uiState.update { it.copy(isFetchingFromBlockchain = true, isLoading = false) }
                    syncAssetsFromBlockchain(userId, assetIds)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isFetchingFromBlockchain = false,
                        error = "Error syncing from blockchain: ${e.message}"
                    )
                }
            }
        }
    }

    fun retryAssetLoading() {
        val recoveryAttempted = _uiState.value.recoveryAttempted

        if (recoveryAttempted) {
            loadUserAssets()
        } else {
            screenModelScope.launch {
                val userId = firebaseService.getCurrentUserId() ?: return@launch
                loadFromBlockchain(userId)
            }
        }
    }
}