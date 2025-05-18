package com.jetbrains.kmpapp.screens.nfc

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.jetbrains.kmpapp.data.BlockchainService
import com.jetbrains.kmpapp.data.FirebaseService
import com.jetbrains.kmpapp.data.NfcService
import com.jetbrains.kmpapp.data.OwnershipRecord
import com.jetbrains.kmpapp.data.PhysicalVerificationManager
import com.jetbrains.kmpapp.data.ProductService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime



data class VerificationResult(
    val isAuthentic: Boolean,
    val assetId: String,
    val productName: String,
    val category: String,
    val currentOwner: String,
    val manufacturer: String,
    val registrationDate: String,
    val previousOwners: Int = 0,
    val lastTransferDate: String = "",
    val ownershipHistory: List<OwnershipRecord> = emptyList(),
    val properties: Map<String, String> = mapOf()
)

class NfcScreenModel(
    private val nfcService: NfcService,
    private val blockchainService: BlockchainService,
    private val firebaseService: FirebaseService,
    private val productService: ProductService
) : ScreenModel {

    private val physicalVerificationManager = PhysicalVerificationManager.getInstance()

    private val _uiState = MutableStateFlow(NfcUiState())
    val uiState: StateFlow<NfcUiState> = _uiState.asStateFlow()

    
    private val _verificationResult = MutableStateFlow<VerificationResult?>(null)
    val verificationResult: StateFlow<VerificationResult?> = _verificationResult.asStateFlow()

    init {
        checkNfcStatus()
    }

    private fun checkNfcStatus() {
        val isSupported = nfcService.isNfcSupported()
        val isEnabled = if (isSupported) nfcService.isNfcEnabled() else false

        _uiState.update {
            it.copy(
                isNfcSupported = isSupported,
                isNfcEnabled = isEnabled,
                error = when {
                    !isSupported -> "NFC is not supported on this device"
                    !isEnabled -> "NFC is not enabled. Please enable it in your device settings."
                    else -> ""
                }
            )
        }
    }

    fun startNfcScan() {
        screenModelScope.launch {
            resetScanState()
            
            nfcService.startNfcScan()
                .onSuccess {
                    nfcService.tagData.collectLatest { tagData ->
                        tagData?.let {
                            _uiState.update { state -> state.copy(tagData = tagData, isScanning = false) }
                            processScannedTag(tagData)
                        }
                    }
                }
                .onFailure { error ->
                    val errorMessage = when (error.message) {
                        "NFC is not enabled" -> "Please enable NFC in your device settings to use this feature."
                        "NFC is not supported on this device" -> "Sorry, your device doesn't support NFC functionality."
                        else -> "Failed to start NFC scan: ${error.message}"
                    }
                    _uiState.update { it.copy(isScanning = false, error = errorMessage) }
                }
        }
    }
    
    private fun resetScanState() {
        _uiState.update { it.copy(isScanning = true, tagData = "", error = "", blockchainResponse = "") }
        _verificationResult.value = null
    }

    fun stopNfcScan() {
        nfcService.stopNfcScan()
        _uiState.update { it.copy(isScanning = false) }
    }

    private suspend fun processScannedTag(tagData: String) {
        try {
            val tagId = extractTagIdFromData(tagData).takeIf { it.isNotEmpty() } ?: run {
                _uiState.update { it.copy(error = "Could not extract tag ID from the scanned tag") }
                return
            }

            
            val assetId = tagId
            
            
            productService.getProduct(assetId).getOrNull()?.let { product ->
                
                verifyAssetOnBlockchain(product)
                return
            }
            
            
            if (verifyAssetExistsOnBlockchain(assetId)) {
                
                syncAssetFromBlockchain(assetId)
            } else {
                
                _verificationResult.value = createUnknownVerificationResult(assetId)
                _uiState.update { it.copy(blockchainResponse = "Product not found on blockchain") }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Error processing tag: ${e.message}") }
        }
    }
    
    private fun createUnknownVerificationResult(assetId: String) = VerificationResult(
        isAuthentic = false,
        assetId = assetId,
        productName = "Unknown Product",
        category = "Unknown",
        currentOwner = "Unknown",
        manufacturer = "Unknown",
        registrationDate = "N/A"
    )

    private suspend fun verifyAssetOnBlockchain(product: com.jetbrains.kmpapp.data.Product) {
        try {
            
            val historyResult = blockchainService.getAssetHistory(product.id)
            
            if (!historyResult.isSuccess) {
                handleFailedVerification(product, "Product exists but could not be verified on blockchain: ${historyResult.exceptionOrNull()?.message}")
                return
            }
            
            val history = historyResult.getOrNull() ?: emptyList()
            
            
            val currentOwnerName = if (product.currentOwner.isNotEmpty()) {
                firebaseService.getUserData(product.currentOwner)
                    .getOrNull()?.displayName ?: product.currentOwner
            } else "Unknown"
            
            
            val previousOwners = history.count { it.action == "transfer" }
            
            
            val lastTransferDate = if (previousOwners > 0) {
                history.lastOrNull { it.action == "transfer" }
                    ?.let { formatTimestamp((it.timestamp * 1000).toLong()) } ?: "N/A"
            } else "N/A"
            
            
            val result = VerificationResult(
                isAuthentic = true,
                assetId = product.id,
                productName = product.name,
                category = product.category,
                currentOwner = currentOwnerName,
                manufacturer = product.manufacturerName,
                registrationDate = formatTimestamp(product.createdAt),
                previousOwners = previousOwners,
                lastTransferDate = lastTransferDate,
                ownershipHistory = history,
                properties = product.properties
            )
            
            

            physicalVerificationManager.recordPhysicalVerification(product.id)
            
            _verificationResult.value = result
            _uiState.update { it.copy(blockchainResponse = "Asset verified on blockchain (Physical verification recorded for 1 hour)") }
            
        } catch (e: Exception) {
            handleFailedVerification(product, "Error verifying asset: ${e.message}")
        }
    }
    
    private fun handleFailedVerification(product: com.jetbrains.kmpapp.data.Product, errorMessage: String) {
        _verificationResult.value = VerificationResult(
            isAuthentic = false,
            assetId = product.id,
            productName = product.name,
            category = product.category,
            currentOwner = "Unknown", 
            manufacturer = product.manufacturerName,
            registrationDate = formatTimestamp(product.createdAt)
        )
        
        _uiState.update { it.copy(error = errorMessage) }
    }

    private suspend fun verifyAssetExistsOnBlockchain(assetId: String): Boolean {
        return try {
            blockchainService.getAssetHistory(assetId)
                .getOrNull()
                ?.let { history -> 
                    history.isNotEmpty() && history.any { it.action == "register" } 
                } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun syncAssetFromBlockchain(assetId: String) {
        try {
            
            productService.forceProductSync(assetId)
                .onSuccess {
                    
                    productService.getProduct(assetId).getOrNull()?.let { product ->
                        
                        verifyAssetOnBlockchain(product)
                        return
                    }
                }
            
            
            
            val historyResult = blockchainService.getAssetHistory(assetId)
            val nodeDataResult = blockchainService.getAssetNodeData(assetId)
            
            if (!historyResult.isSuccess || !nodeDataResult.isSuccess) {
                handleBlockchainVerificationFailure(assetId, "Failed to verify asset: couldn't retrieve blockchain data")
                return
            }
            
            val history = historyResult.getOrNull() ?: emptyList()
            val nodeData = nodeDataResult.getOrNull() ?: mapOf()
            
            
            if (!history.any { it.action == "register" }) {
                _verificationResult.value = VerificationResult(
                    isAuthentic = false,
                    assetId = assetId,
                    productName = nodeData["name"] ?: "Unknown Product",
                    category = nodeData["category"] ?: "Unknown",
                    currentOwner = "Unknown",
                    manufacturer = nodeData["manufacturer"] ?: "Unknown",
                    registrationDate = "Unknown"
                )
                _uiState.update { it.copy(error = "Asset exists on blockchain but appears to be invalid (no registration record)") }
                return
            }
            
            
            val lastRecord = history.maxByOrNull { it.timestamp }
            val productInfo = extractProductInfoFromNodeData(nodeData)
            val previousOwners = history.count { it.action == "transfer" }
            
            
            val currentOwner = lastRecord?.let {
                firebaseService.getUserData(it.user_id)
                    .getOrNull()?.displayName ?: it.user_id
            } ?: "Unknown"
            
            
            val lastTransferDate = history
                .filter { it.action == "transfer" }
                .maxByOrNull { it.timestamp }
                ?.let { formatTimestamp((it.timestamp * 1000).toLong()) } ?: "N/A"
            
            
            val result = VerificationResult(
                isAuthentic = true,
                assetId = assetId,
                productName = productInfo.name,
                category = productInfo.category,
                currentOwner = currentOwner,
                manufacturer = productInfo.manufacturer,
                registrationDate = productInfo.createdAt,
                previousOwners = previousOwners,
                lastTransferDate = lastTransferDate,
                ownershipHistory = history,
                properties = nodeData.filterNot { it.key in listOf("name", "description", "category", "manufacturer", "createdAt") }
            )
            

            physicalVerificationManager.recordPhysicalVerification(assetId)
            
            _verificationResult.value = result
            _uiState.update { it.copy(blockchainResponse = "Asset verified on blockchain (Physical verification recorded for 1 hour)") }
            
        } catch (e: Exception) {
            handleBlockchainVerificationFailure(assetId, "Error syncing from blockchain: ${e.message}")
        }
    }
    
    private fun extractProductInfoFromNodeData(nodeData: Map<String, String>): ProductInfo {
        return ProductInfo(
            name = nodeData["name"] ?: "Unknown Product",
            category = nodeData["category"] ?: "Unknown",
            manufacturer = nodeData["manufacturer"] ?: "Unknown",
            createdAt = nodeData["createdAt"]?.toLongOrNull()?.let { formatTimestamp(it) } ?: "Unknown"
        )
    }
    
    private data class ProductInfo(
        val name: String,
        val category: String,
        val manufacturer: String,
        val createdAt: String
    )
    
    private fun handleBlockchainVerificationFailure(assetId: String, errorMessage: String) {
        _verificationResult.value = createUnknownVerificationResult(assetId)
        _uiState.update { it.copy(error = errorMessage) }
    }

    fun getVerificationResult(tagId: String): VerificationResult? = _verificationResult.value

    fun resetScan() {
        _uiState.update { it.copy(tagData = "", error = "", blockchainResponse = "") }
        _verificationResult.value = null
    }

    private fun extractTagIdFromData(tagData: String): String {
        return Regex("Tag ID \\(hex\\): ([A-F0-9]+)")
            .find(tagData)
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()
    }

    private fun formatTimestamp(timestamp: Long): String {
        return try {
            val instant = Instant.fromEpochMilliseconds(timestamp)
            val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

            val hour = dateTime.hour.toString().padStart(2, '0')
            val minute = dateTime.minute.toString().padStart(2, '0')

            return "${dateTime.date} at $hour:$minute"
        } catch (e: Exception) {
            "Unknown Date and Time"
        }
    }

    override fun onDispose() {
        super.onDispose()
        
        
        stopNfcScan()
    }
}