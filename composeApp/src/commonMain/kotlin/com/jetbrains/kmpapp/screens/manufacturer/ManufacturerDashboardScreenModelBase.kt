package com.jetbrains.kmpapp.screens.manufacturer

import cafe.adriel.voyager.core.model.ScreenModel
import com.jetbrains.kmpapp.data.Product
import kotlinx.coroutines.flow.StateFlow

interface ManufacturerDashboardScreenModelBase : ScreenModel {
    val uiState: StateFlow<ManufacturerDashboardUiState>

    fun loadProductTemplates()
    fun loadInstantiatedProducts()
    fun createDummyTemplate()
    fun registerProductOnBlockchain(productId: String)
    suspend fun createInstantiatedProduct(templateId: String, rfidTagId: String): Result<String>
    fun processProductInstantiation(templateId: String, rfidTagId: String)
    fun startRfidScan(templateId: String)
    fun stopRfidScan()
    fun clearRfidScan()
}

data class ManufacturerDashboardUiState(
    val isLoading: Boolean = false,
    val error: String = "",
    val success: String = "",
    val isManufacturer: Boolean = false,

    
    val templates: List<Product> = emptyList(),
    val selectedTemplateId: String = "",

    
    val instantiatedProducts: List<Product> = emptyList(),

    
    val isRfidScanning: Boolean = false,
    val scannedRfidId: String = "",

    
    val activeTab: Int = 0,
    val isFirstLoad: Boolean = true,
    val processingProductId: String = ""
)