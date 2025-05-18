package com.jetbrains.kmpapp.data.repository

import com.jetbrains.kmpapp.data.FirebaseService
import com.jetbrains.kmpapp.data.Product
import com.jetbrains.kmpapp.data.ProductService
import com.jetbrains.kmpapp.data.TransferNfcService
import com.jetbrains.kmpapp.mvi.Repository
import com.jetbrains.kmpapp.screens.qrcode.TransferRequest
import com.jetbrains.kmpapp.screens.qrcode.TransferResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull

class TransferRepository(
    private val firebaseService: FirebaseService,
    private val productService: ProductService,
    private val nfcService: TransferNfcService
) : Repository<TransferRequest, TransferResponse> {

    override suspend fun execute(request: TransferRequest): Flow<TransferResponse> = flow {
        try {
            when (request) {
                is TransferRequest.GetRecipientData -> {
                    val recipientId = request.recipientId
                    firebaseService.getUserData(recipientId)
                        .onSuccess { userData -> 
                            if (userData != null) {
                                emit(TransferResponse.RecipientData(userData.displayName))
                            } else {
                                emit(TransferResponse.Error("Recipient not found"))
                            }
                        }
                        .onFailure { error ->
                            emit(TransferResponse.Error("Failed to get recipient data: ${error.message}"))
                        }
                }
                
                is TransferRequest.GetUserProducts -> {
                    val userId = firebaseService.getCurrentUserId()
                    if (userId == null) {
                        emit(TransferResponse.Error("User not signed in"))
                        return@flow
                    }

                    val directResult = withTimeoutOrNull(8000) {
                        productService.getUserProducts(userId)
                    }
                    
                    if (directResult != null && directResult.isSuccess && directResult.getOrNull()?.isNotEmpty() == true) {
                        val products = directResult.getOrNull() ?: emptyList()
                        emit(TransferResponse.UserProducts(products))
                        return@flow
                    }

                    withTimeoutOrNull(10000) { 
                        productService.syncAllBlockchainProducts(userId) 
                    }

                    val syncedResult = withTimeoutOrNull(5000) {
                        productService.getUserProducts(userId)
                    }
                    
                    if (syncedResult != null && syncedResult.isSuccess) {
                        val products = syncedResult.getOrNull() ?: emptyList()
                        emit(TransferResponse.UserProducts(products))
                    } else {
                        val blockchainAssetIds = withTimeoutOrNull(5000) {
                            productService.getBlockchainProducts(userId).getOrNull() ?: emptyList()
                        } ?: emptyList()
                        
                        if (blockchainAssetIds.isEmpty()) {
                            emit(TransferResponse.UserProducts(emptyList()))
                            return@flow
                        }
                        
                        val syncedProducts = blockchainAssetIds.mapNotNull { assetId ->
                            try {
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
                        
                        emit(TransferResponse.UserProducts(syncedProducts))
                    }
                }
                
                is TransferRequest.GetProduct -> {
                    val productId = request.productId
                    productService.getProduct(productId)
                        .onSuccess { product ->
                            if (product != null) {
                                emit(TransferResponse.ProductData(product))
                            } else {
                                emit(TransferResponse.Error("Product not found"))
                            }
                        }
                        .onFailure { error ->
                            emit(TransferResponse.Error("Failed to get product: ${error.message}"))
                        }
                }
                
                is TransferRequest.VerifyOwnership -> {
                    val productId = request.productId
                    val userId = request.userId
                    
                    productService.verifyProductOwnership(productId, userId)
                        .onSuccess { isOwner ->
                            emit(TransferResponse.OwnershipVerified(isOwner))
                        }
                        .onFailure { error ->
                            emit(TransferResponse.Error("Failed to verify ownership: ${error.message}"))
                        }
                }
                
                is TransferRequest.TransferOwnership -> {
                    val productId = request.productId
                    val recipientId = request.recipientId
                    
                    val result = withTimeoutOrNull(30000) {
                        productService.transferProductOwnership(productId, recipientId)
                    } ?: Result.failure(Exception("Transfer operation timed out"))
                    
                    result.onSuccess {
                        emit(TransferResponse.TransferCompleted(true))
                    }.onFailure { error ->
                        val errorMessage = when {
                            error.message?.contains("not the owner") == true -> 
                                "You are not the owner of this asset on the blockchain"
                            error.message?.contains("timeout") == true -> 
                                "Blockchain service may be unavailable"
                            else -> error.message ?: "Unknown error"
                        }
                        emit(TransferResponse.Error("Transfer failed: $errorMessage"))
                    }
                }
                
                is TransferRequest.SyncProduct -> {
                    val productId = request.productId
                    
                    productService.forceProductSync(productId)
                        .onSuccess {
                            productService.getProduct(productId)
                                .onSuccess { product ->
                                    if (product != null) {
                                        emit(TransferResponse.ProductData(product))
                                    } else {
                                        emit(TransferResponse.Error("Product not found after sync"))
                                    }
                                }
                                .onFailure { error ->
                                    emit(TransferResponse.Error("Failed to get product after sync: ${error.message}"))
                                }
                        }
                        .onFailure { error ->
                            emit(TransferResponse.Error("Failed to sync product: ${error.message}"))
                        }
                }
                
                is TransferRequest.ProcessNfcTag -> {
                    val tagData = request.tagData
                    val rfidId = nfcService.extractRfidIdFromTagData(tagData)
                    
                    if (rfidId.isEmpty()) {
                        emit(TransferResponse.Error("Could not identify the NFC tag"))
                    } else {
                        emit(TransferResponse.TagData(rfidId))
                    }
                }
            }
        } catch (e: Exception) {
            emit(TransferResponse.Error(e.message ?: "An unexpected error occurred"))
        }
    }
}