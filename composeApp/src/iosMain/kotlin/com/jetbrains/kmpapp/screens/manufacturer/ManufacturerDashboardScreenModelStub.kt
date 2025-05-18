package com.jetbrains.kmpapp.screens.manufacturer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ManufacturerDashboardScreenModelStub : ManufacturerDashboardScreenModelBase {

    private val _uiState = MutableStateFlow(
        ManufacturerDashboardUiState(
            isLoading = false,
            error = "Manufacturer functionality is not yet available on iOS.",
            isManufacturer = false,
            templates = emptyList(),
            isFirstLoad = false
        )
    )

    override val uiState: StateFlow<ManufacturerDashboardUiState> = _uiState.asStateFlow()

    override fun loadProductTemplates() {
        
    }

    override fun loadInstantiatedProducts() {
        
    }

    override fun createDummyTemplate() {
        
    }

    override fun registerProductOnBlockchain(productId: String) {
        
    }

    override suspend fun createInstantiatedProduct(templateId: String, rfidTagId: String): Result<String> {
        return Result.failure(UnsupportedOperationException("Not implemented on iOS"))
    }

    override fun processProductInstantiation(templateId: String, rfidTagId: String) {
        
    }

    override fun startRfidScan(templateId: String) {
        
    }

    override fun stopRfidScan() {
        
    }

    override fun clearRfidScan() {
        
    }

    override fun onDispose() {
        
    }
}