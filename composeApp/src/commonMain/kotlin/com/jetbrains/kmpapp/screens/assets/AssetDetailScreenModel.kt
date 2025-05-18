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

data class AssetDetailUiState(
    val isLoading: Boolean = false,
    val asset: Product? = null,
    val error: String = "",
    val actionError: String = "",
    val ownershipHistory: List<OwnershipRecord> = emptyList(),
    val isUserOwner: Boolean = false,
    val isTransferring: Boolean = false,
    val userBalance: Int = 0
)

class AssetDetailScreenModel(
    private val firebaseService: FirebaseService,
    private val productService: ProductService,
    private val blockchainService: BlockchainService
) : ScreenModel {

    private val _uiState = MutableStateFlow(AssetDetailUiState(isLoading = true))
    val uiState: StateFlow<AssetDetailUiState> = _uiState.asStateFlow()

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

                        _uiState.update {
                            it.copy(
                                asset = product,
                                isUserOwner = product.currentOwner == currentUserId
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


            blockchainService.getUserBalance(userId)
                .onSuccess { balance ->
                    _uiState.update {
                        it.copy(userBalance = balance)
                    }
                }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(isLoading = false)
            }
        }
    }


    fun transferAsset() {
        _uiState.update {
            it.copy(
                actionError = "Transfer feature coming soon in a future update"
            )
        }
    }

    fun clearActionError() {
        _uiState.update { it.copy(actionError = "") }
    }
}