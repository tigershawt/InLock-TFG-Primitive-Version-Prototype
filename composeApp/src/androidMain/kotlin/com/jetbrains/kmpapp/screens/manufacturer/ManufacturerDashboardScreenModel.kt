package com.jetbrains.kmpapp.screens.manufacturer

import cafe.adriel.voyager.core.model.screenModelScope
import com.jetbrains.kmpapp.data.FirebaseService
import com.jetbrains.kmpapp.data.NfcService
import com.jetbrains.kmpapp.data.Product
import com.jetbrains.kmpapp.data.ProductService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock

class ManufacturerDashboardScreenModel(
    private val firebaseService: FirebaseService,
    private val productService: ProductService,
    private val nfcService: NfcService
) : ManufacturerDashboardScreenModelBase {

    private val _uiState = MutableStateFlow(ManufacturerDashboardUiState())
    override val uiState: StateFlow<ManufacturerDashboardUiState> = _uiState.asStateFlow()

    init {
        screenModelScope.launch(Dispatchers.IO) {
            try {
                checkManufacturerAccess()
                if (_uiState.value.isManufacturer && _uiState.value.isFirstLoad) {
                    loadProductTemplates()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            error = "Failed to initialize: ${e.message}",
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    private suspend fun checkManufacturerAccess() {
        val isManufacturer = withContext(Dispatchers.IO) {
            try {
                firebaseService.isManufacturer()
            } catch (e: Exception) {
                false
            }
        }

        withContext(Dispatchers.Main) {
            _uiState.update { it.copy(isManufacturer = isManufacturer) }
            if (!isManufacturer) {
                _uiState.update { it.copy(error = "Unauthorized. Manufacturer access required.") }
            }
        }
    }

    override fun loadProductTemplates() {
        if (_uiState.value.isLoading) return

        screenModelScope.launch {
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        isLoading = true,
                        error = "",
                        success = "",
                        isFirstLoad = false,
                        activeTab = 0
                    )
                }
            }

            try {
                withContext(Dispatchers.IO) {
                    val currentUserId = firebaseService.getCurrentUserId()
                    if (currentUserId == null) {
                        withContext(Dispatchers.Main) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Not logged in"
                                )
                            }
                        }
                        return@withContext
                    }

                    val isManufacturer = firebaseService.isManufacturer()

                    val checkResult = withTimeoutOrNull(5000) {
                        productService.getManufacturerTemplates(currentUserId)
                    }

                    val initialTemplatesCount = checkResult?.getOrNull()?.size ?: 0

                    if (initialTemplatesCount == 0 && isManufacturer) {
                        createDummyTemplate()
                        kotlinx.coroutines.delay(2000)
                    }

                    val templatesResult = withTimeoutOrNull(8000) {
                        productService.getManufacturerTemplates(currentUserId)
                    } ?: Result.failure(Exception("Operation timed out"))

                    withContext(Dispatchers.Main) {
                        templatesResult.fold(
                            onSuccess = { templates ->
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        templates = templates,
                                        error = ""
                                    )
                                }
                            },
                            onFailure = { error ->
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        error = error.message ?: "Failed to load product templates"
                                    )
                                }
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "An unexpected error occurred"
                        )
                    }
                }
            }
        }
    }

    override fun loadInstantiatedProducts() {
        screenModelScope.launch {
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(isLoading = true, error = "", success = "") }
            }

            try {
                withContext(Dispatchers.IO) {
                    val currentUserId = firebaseService.getCurrentUserId()
                    if (currentUserId == null) {
                        withContext(Dispatchers.Main) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Not logged in"
                                )
                            }
                        }
                        return@withContext
                    }

                    val productsResult = withTimeoutOrNull(8000) {
                        productService.getManufacturerInstantiatedProducts(currentUserId)
                    } ?: Result.failure(Exception("Operation timed out"))

                    withContext(Dispatchers.Main) {
                        productsResult.fold(
                            onSuccess = { products ->
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        instantiatedProducts = products,
                                        error = ""
                                    )
                                }
                            },
                            onFailure = { error ->
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        error = error.message ?: "Failed to load products"
                                    )
                                }
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "An unexpected error occurred"
                        )
                    }
                }
            }
        }
    }

    override suspend fun createInstantiatedProduct(templateId: String, rfidTagId: String): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            val currentUserId = firebaseService.getCurrentUserId()
                ?: throw Exception("User not signed in")
            return@withContext productService.instantiateProduct(templateId, rfidTagId).getOrThrow()
        }
    }

    override fun createDummyTemplate() {
        screenModelScope.launch {
            try {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isLoading = true, error = "", success = "") }
                }

                withContext(Dispatchers.IO) {
                    val currentUserId = firebaseService.getCurrentUserId() ?: return@withContext

                    val userData = firebaseService.getUserData(currentUserId).getOrNull()
                        ?: throw Exception("User data not found")

                    val timestamp = Clock.System.now().toEpochMilliseconds()

                    val dummyTemplate = Product(
                        name = "Demo Product Template",
                        description = "This is a sample product template for demonstration purposes",
                        category = "Demo",
                        manufacturer = currentUserId,
                        manufacturerName = userData.displayName,
                        createdAt = timestamp,
                        currentOwner = currentUserId,
                        isTemplate = true,
                        properties = mapOf(
                            "material" to "Metal",
                            "color" to "Silver",
                            "weight" to "250g"
                        )
                    )

                    val result = productService.createProductTemplate(dummyTemplate)

                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(isLoading = false) }

                        result.onSuccess {
                            _uiState.update {
                                it.copy(
                                    success = "Template created successfully",
                                    activeTab = 0
                                )
                            }
                            loadProductTemplates()
                        }.onFailure { error ->
                            _uiState.update {
                                it.copy(error = "Failed to create dummy template: ${error.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Error creating template: ${e.message}"
                        )
                    }
                }
            }
        }
    }

    override fun registerProductOnBlockchain(productId: String) {
        if (_uiState.value.isLoading) return

        screenModelScope.launch {
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        isLoading = true,
                        error = "",
                        success = "",
                        processingProductId = productId
                    )
                }
            }

            try {
                withContext(Dispatchers.IO) {
                    val productResult = withTimeoutOrNull(5000) {
                        productService.getProduct(productId)
                    } ?: Result.failure(Exception("Timeout fetching product"))

                    withContext(Dispatchers.Main) {
                        productResult.fold(
                            onSuccess = { product ->
                                if (product == null) {
                                    _uiState.update {
                                        it.copy(
                                            isLoading = false,
                                            error = "Product not found",
                                            processingProductId = ""
                                        )
                                    }
                                } else {
                                    screenModelScope.launch {
                                        withContext(Dispatchers.IO) {
                                            val registrationResult = withTimeoutOrNull(10000) {
                                                productService.registerProductOnBlockchain(product)
                                            } ?: Result.failure(Exception("Blockchain registration timed out"))

                                            withContext(Dispatchers.Main) {
                                                registrationResult.fold(
                                                    onSuccess = {
                                                        
                                                        screenModelScope.launch(Dispatchers.IO) {
                                                            try {
                                                                productService.forceProductSync(productId)
                                                            } catch (e: Exception) {
                                                            }
                                                        }

                                                        _uiState.update {
                                                            it.copy(
                                                                isLoading = false,
                                                                error = "",
                                                                success = "Product registered on blockchain successfully",
                                                                processingProductId = ""
                                                            )
                                                        }
                                                        loadInstantiatedProducts()
                                                    },
                                                    onFailure = { error ->
                                                        _uiState.update {
                                                            it.copy(
                                                                isLoading = false,
                                                                error = error.message ?: "Failed to register product on blockchain",
                                                                processingProductId = ""
                                                            )
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            onFailure = { error ->
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        error = error.message ?: "Failed to get product details",
                                        processingProductId = ""
                                    )
                                }
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "An unexpected error occurred",
                            processingProductId = ""
                        )
                    }
                }
            }
        }
    }

    override fun startRfidScan(templateId: String) {
        screenModelScope.launch {
            try {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isRfidScanning = true,
                            error = "",
                            success = "",
                            selectedTemplateId = templateId,
                            scannedRfidId = ""
                        )
                    }
                }

                nfcService.startNfcScan()
                    .onSuccess {
                        nfcService.readNfcTag()
                            .onSuccess { tagData ->
                                val rfidId = extractRfidIdFromData(tagData)
                                withContext(Dispatchers.Main) {
                                    _uiState.update {
                                        it.copy(
                                            scannedRfidId = rfidId,
                                            isRfidScanning = false
                                        )
                                    }
                                }
                            }
                            .onFailure { error ->
                                withContext(Dispatchers.Main) {
                                    _uiState.update {
                                        it.copy(
                                            isRfidScanning = false,
                                            error = "Failed to read RFID tag: ${error.message}"
                                        )
                                    }
                                }
                            }
                    }
                    .onFailure { error ->
                        withContext(Dispatchers.Main) {
                            _uiState.update {
                                it.copy(
                                    isRfidScanning = false,
                                    error = "Failed to start NFC scan: ${error.message}"
                                )
                            }
                        }
                    }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isRfidScanning = false,
                            error = e.message ?: "An unexpected error occurred"
                        )
                    }
                }
            }
        }
    }

    override fun stopRfidScan() {
        nfcService.stopNfcScan()
        _uiState.update { it.copy(isRfidScanning = false) }
    }

    override fun clearRfidScan() {
        _uiState.update {
            it.copy(
                scannedRfidId = "",
                selectedTemplateId = "",
                error = ""
            )
        }
    }

    override fun processProductInstantiation(templateId: String, rfidTagId: String) {
        screenModelScope.launch {
            try {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isLoading = true,
                            error = "",
                            success = "",
                            scannedRfidId = rfidTagId
                        )
                    }
                }

                withContext(Dispatchers.IO) {
                    val instantiateResult = productService.instantiateProduct(templateId, rfidTagId)

                    instantiateResult.onSuccess { productId ->
                        val productResult = productService.getProduct(productId)
                        productResult.onSuccess { product ->
                            if (product != null) {
                                val blockchainResult = productService.registerProductOnBlockchain(product)
                                blockchainResult.onSuccess {
                                    
                                    try {
                                        productService.forceProductSync(productId)
                                    } catch (e: Exception) {
                                    }

                                    withContext(Dispatchers.Main) {
                                        _uiState.update {
                                            it.copy(
                                                isLoading = false,
                                                success = "Product created and registered on blockchain successfully",
                                                scannedRfidId = "",
                                                selectedTemplateId = ""
                                            )
                                        }
                                        loadProductTemplates()
                                    }
                                }.onFailure { error ->
                                    withContext(Dispatchers.Main) {
                                        _uiState.update {
                                            it.copy(
                                                isLoading = false,
                                                error = "Product created but blockchain registration failed: ${error.message}",
                                                scannedRfidId = "",
                                                selectedTemplateId = ""
                                            )
                                        }
                                    }
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    _uiState.update {
                                        it.copy(
                                            isLoading = false,
                                            error = "Product instantiation succeeded but product not found",
                                            scannedRfidId = "",
                                            selectedTemplateId = ""
                                        )
                                    }
                                }
                            }
                        }.onFailure { error ->
                            withContext(Dispatchers.Main) {
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        error = "Product created but failed to retrieve: ${error.message}",
                                        scannedRfidId = "",
                                        selectedTemplateId = ""
                                    )
                                }
                            }
                        }
                    }.onFailure { error ->
                        withContext(Dispatchers.Main) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Failed to instantiate product: ${error.message}",
                                    scannedRfidId = "",
                                    selectedTemplateId = ""
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Error instantiating product: ${e.message}",
                            scannedRfidId = "",
                            selectedTemplateId = ""
                        )
                    }
                }
            }
        }
    }

    private fun extractRfidIdFromData(tagData: String): String {
        val idMatch = Regex("Tag ID \\(hex\\): ([A-F0-9]+)").find(tagData)
        return idMatch?.groupValues?.get(1) ?: ""
    }

    override fun onDispose() {
        super.onDispose()
        if (_uiState.value.isRfidScanning) {
            nfcService.stopNfcScan()
        }
    }
}