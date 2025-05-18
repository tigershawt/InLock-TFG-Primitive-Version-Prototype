package com.jetbrains.kmpapp.screens.assets

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.jetbrains.kmpapp.data.BlockchainService
import com.jetbrains.kmpapp.data.FirebaseService
import com.jetbrains.kmpapp.data.OwnershipRecord
import com.jetbrains.kmpapp.data.Product
import com.jetbrains.kmpapp.data.ProductService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EnhancedAssetDetailUiState(
    val isLoading: Boolean = true,
    val asset: Product? = null,
    val error: String = "",
    val actionError: String = "",
    val ownershipHistory: List<OwnershipRecord> = emptyList(),
    val isUserOwner: Boolean = false,
    val isStolen: Boolean = false,
    val isVerifying: Boolean = false
)

class EnhancedAssetDetailScreenModel(
    private val firebaseService: FirebaseService,
    private val productService: ProductService,
    private val blockchainService: BlockchainService
) : ScreenModel {

    private val _uiState = MutableStateFlow(EnhancedAssetDetailUiState())
    val uiState: StateFlow<EnhancedAssetDetailUiState> = _uiState.asStateFlow()

    fun loadAssetDetails(assetId: String) {
        screenModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = "") }

                val currentUserId = firebaseService.getCurrentUserId()
                if (currentUserId == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "User not signed in"
                        )
                    }
                    return@launch
                }

                productService.getProduct(assetId)
                    .onSuccess { product ->
                        if (product == null) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Asset not found"
                                )
                            }
                            return@onSuccess
                        }

                        val isStolen = product.properties.containsKey("status") &&
                                product.properties["status"] == "stolen"

                        _uiState.update {
                            it.copy(
                                asset = product,
                                isUserOwner = product.currentOwner == currentUserId,
                                isStolen = isStolen
                            )
                        }

                        if (product.isRegisteredOnBlockchain) {
                            loadAssetBlockchainData(assetId, currentUserId)
                        } else {
                            _uiState.update { it.copy(isLoading = false) }
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Failed to load asset details"
                            )
                        }
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

    private suspend fun loadAssetBlockchainData(assetId: String, userId: String) {
        try {
            blockchainService.getAssetHistory(assetId)
                .onSuccess { history ->
                    _uiState.update {
                        it.copy(
                            ownershipHistory = history,
                            isLoading = false
                        )
                    }
                }
                .onFailure { _ ->
                    _uiState.update {
                        it.copy(isLoading = false)
                    }
                }

            blockchainService.verifyOwnership(assetId, userId)
                .onSuccess { isOwner ->
                    _uiState.update {
                        it.copy(isUserOwner = isOwner)
                    }
                }
                .onFailure { _ -> }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(isLoading = false)
            }
        }
    }

    fun reportAssetStolen() {
        screenModelScope.launch {
            try {
                val currentAsset = _uiState.value.asset ?: return@launch

                _uiState.update { it.copy(isVerifying = true) }

                productService.reportAssetStolen(currentAsset.id)
                    .onSuccess {
                        val updatedAsset = currentAsset.copy(
                            properties = currentAsset.properties + mapOf("status" to "stolen")
                        )

                        _uiState.update {
                            it.copy(
                                isVerifying = false,
                                asset = updatedAsset,
                                isStolen = true
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isVerifying = false,
                                actionError = "Failed to report asset as stolen: ${error.message}"
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isVerifying = false,
                        actionError = e.message ?: "Failed to report asset as stolen"
                    )
                }
            }
        }
    }

    fun verifyAssetAuthenticity() {
        screenModelScope.launch {
            try {
                val currentAsset = _uiState.value.asset ?: return@launch

                if (!currentAsset.isRegisteredOnBlockchain) {
                    _uiState.update {
                        it.copy(actionError = "This asset is not registered on the blockchain")
                    }
                    return@launch
                }

                _uiState.update { it.copy(isVerifying = true) }

                val history = blockchainService.getAssetHistory(currentAsset.id)
                    .getOrNull() ?: emptyList()

                if (history.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isVerifying = false,
                            actionError = "No blockchain history found for this asset"
                        )
                    }
                    return@launch
                }

                val lastRecord = history.last()
                val blockchainOwnerId = lastRecord.user_id

                if (blockchainOwnerId == currentAsset.currentOwner) {
                    _uiState.update {
                        it.copy(
                            isVerifying = false,
                            ownershipHistory = history
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isVerifying = false,
                            actionError = "Warning: Asset ownership records don't match blockchain!"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isVerifying = false,
                        actionError = e.message ?: "Failed to verify authenticity"
                    )
                }
            }
        }
    }

    fun clearActionError() {
        _uiState.update { it.copy(actionError = "") }
    }
}