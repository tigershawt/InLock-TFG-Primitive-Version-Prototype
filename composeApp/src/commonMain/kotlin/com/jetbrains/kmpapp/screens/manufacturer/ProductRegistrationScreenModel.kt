package com.jetbrains.kmpapp.screens.manufacturer

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.jetbrains.kmpapp.data.FirebaseService
import com.jetbrains.kmpapp.data.NfcService
import com.jetbrains.kmpapp.data.Product
import com.jetbrains.kmpapp.data.ProductService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class ProductRegistrationUiState(
    val isLoading: Boolean = false,
    val error: String = "",
    val success: String = "",
    val isManufacturer: Boolean = false,
    val isNfcScanning: Boolean = false,
    val scannedRfidId: String = "",
    val productProperties: Map<String, String> = mapOf(),
    val imageBytes: ByteArray? = null
) {
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProductRegistrationUiState) return false

        if (isLoading != other.isLoading) return false
        if (error != other.error) return false
        if (success != other.success) return false
        if (isManufacturer != other.isManufacturer) return false
        if (isNfcScanning != other.isNfcScanning) return false
        if (scannedRfidId != other.scannedRfidId) return false
        if (productProperties != other.productProperties) return false
        if (imageBytes != null) {
            if (other.imageBytes == null) return false
            if (!imageBytes.contentEquals(other.imageBytes)) return false
        } else if (other.imageBytes != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isLoading.hashCode()
        result = 31 * result + error.hashCode()
        result = 31 * result + success.hashCode()
        result = 31 * result + isManufacturer.hashCode()
        result = 31 * result + isNfcScanning.hashCode()
        result = 31 * result + scannedRfidId.hashCode()
        result = 31 * result + productProperties.hashCode()
        result = 31 * result + (imageBytes?.contentHashCode() ?: 0)
        return result
    }
}

class ProductRegistrationScreenModel(
    private val firebaseService: FirebaseService,
    private val productService: ProductService,
    private val nfcService: NfcService
) : ScreenModel {

    private val _uiState = MutableStateFlow(ProductRegistrationUiState())
    val uiState: StateFlow<ProductRegistrationUiState> = _uiState.asStateFlow()
    private val TAG = "ProductRegistrationScreenModel"

    init {
        checkManufacturerAccess()
    }

    private fun checkManufacturerAccess() {
        val isManufacturer = firebaseService.isManufacturer()
        _uiState.update { it.copy(isManufacturer = isManufacturer) }

        if (!isManufacturer) {
            _uiState.update { it.copy(error = "Unauthorized. Manufacturer access required.") }
        }
    }

    fun updateProductProperty(key: String, value: String) {
        val updatedProperties = _uiState.value.productProperties.toMutableMap()

        if (value.isEmpty()) {
            
            updatedProperties.remove(key)
        } else {
            
            updatedProperties[key] = value
        }

        _uiState.update { it.copy(productProperties = updatedProperties) }
    }

    fun setImageBytes(bytes: ByteArray) {
        _uiState.update { it.copy(imageBytes = bytes) }
    }

    fun createTemplate(name: String, description: String, category: String) {
        screenModelScope.launch {
            
            if (name.isEmpty() || description.isEmpty() || category.isEmpty()) {
                _uiState.update { it.copy(error = "Please fill in all required fields") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = "", success = "") }

            try {
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

                
                val userData = firebaseService.getUserData(currentUserId).getOrNull()

                if (userData == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to get user data"
                        )
                    }
                    return@launch
                }

                
                val template = Product(
                    name = name,
                    description = description,
                    category = category,
                    manufacturer = currentUserId,
                    manufacturerName = userData.displayName,
                    properties = _uiState.value.productProperties,
                    createdAt = Clock.System.now().toEpochMilliseconds(),
                    currentOwner = currentUserId,
                    isTemplate = true
                )

                
                productService.createProductTemplate(template)
                    .onSuccess { templateId ->
                        
                        val imageBytes = _uiState.value.imageBytes
                        if (imageBytes != null) {
                            productService.uploadProductImage(templateId, imageBytes)
                                .onSuccess { imageUrl ->
                                    _uiState.update {
                                        it.copy(
                                            isLoading = false,
                                            success = "Template created successfully with image",
                                            productProperties = mapOf(),
                                            imageBytes = null
                                        )
                                    }
                                }
                                .onFailure { error ->
                                    _uiState.update {
                                        it.copy(
                                            isLoading = false,
                                            success = "Template created but image upload failed: ${error.message}",
                                            productProperties = mapOf(),
                                            imageBytes = null
                                        )
                                    }
                                }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    success = "Template created successfully",
                                    productProperties = mapOf()
                                )
                            }
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Failed to create template"
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

    override fun onDispose() {
        super.onDispose()
        if (_uiState.value.isNfcScanning) {
            nfcService.stopNfcScan()
        }
    }
}