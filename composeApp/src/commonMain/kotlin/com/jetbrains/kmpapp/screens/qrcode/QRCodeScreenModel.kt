
package com.jetbrains.kmpapp.screens.qrcode

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.jetbrains.kmpapp.data.FirebaseService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QRCodeUiState(
    val isLoading: Boolean = true,
    val error: String = "",
    val userId: String = "",
    val userQRCode: ByteArray? = null,
    val isScanMode: Boolean = false,
    val isScanning: Boolean = false,
    val scannedUserId: String = ""
) {
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is QRCodeUiState) return false

        if (isLoading != other.isLoading) return false
        if (error != other.error) return false
        if (userId != other.userId) return false
        if (userQRCode != null) {
            if (other.userQRCode == null) return false
            if (!userQRCode.contentEquals(other.userQRCode)) return false
        } else if (other.userQRCode != null) return false
        if (isScanMode != other.isScanMode) return false
        if (isScanning != other.isScanning) return false
        if (scannedUserId != other.scannedUserId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isLoading.hashCode()
        result = 31 * result + error.hashCode()
        result = 31 * result + userId.hashCode()
        result = 31 * result + (userQRCode?.contentHashCode() ?: 0)
        result = 31 * result + isScanMode.hashCode()
        result = 31 * result + isScanning.hashCode()
        result = 31 * result + scannedUserId.hashCode()
        return result
    }
}

class QRCodeScreenModel(
    private val firebaseService: FirebaseService,
    private val qrCodeService: QRCodeService
) : ScreenModel {

    private val _uiState = MutableStateFlow(QRCodeUiState())
    val uiState: StateFlow<QRCodeUiState> = _uiState.asStateFlow()

    private val TAG = "QRCodeScreenModel"

    init {
        loadUserData()
    }

    private fun loadUserData() {
        screenModelScope.launch {
            try {
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

                _uiState.update { it.copy(userId = userId) }

                
                generateQRCode(userId)
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

    private fun generateQRCode(content: String) {
        screenModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val qrCodeData = qrCodeService.generateQRCode(content)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userQRCode = qrCodeData,
                        error = ""
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to generate QR code: ${e.message}"
                    )
                }
            }
        }
    }

    fun setMode(scanMode: Boolean) {
        
        if (!scanMode && _uiState.value.isScanMode) {
            _uiState.update {
                it.copy(
                    isScanMode = false,
                    isScanning = false,
                    scannedUserId = "",
                    error = ""
                )
            }
        } else {
            _uiState.update { it.copy(isScanMode = scanMode, error = "") }
        }
    }

    fun startQRScan() {
        screenModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        isScanning = true,
                        scannedUserId = "",
                        error = ""
                    )
                }

                
                qrCodeService.startQRCodeScan().collect { result ->
                    result.onSuccess { scannedContent ->
                        
                        _uiState.update {
                            it.copy(
                                scannedUserId = scannedContent,
                                isScanning = false,
                                error = ""
                            )
                        }
                    }.onFailure { error ->
                        _uiState.update {
                            it.copy(
                                error = error.message ?: "Failed to scan QR code",
                                isScanning = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        error = e.message ?: "Failed to start QR scan"
                    )
                }
            }
        }
    }

    fun stopQRScan() {
        screenModelScope.launch {
            try {
                qrCodeService.stopQRCodeScan()
                _uiState.update { it.copy(isScanning = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        error = e.message ?: "Failed to stop QR scan"
                    )
                }
            }
        }
    }

    override fun onDispose() {
        super.onDispose()
        if (_uiState.value.isScanning) {
            
            screenModelScope.launch {
                try {
                    qrCodeService.stopQRCodeScan()
                } catch (e: Exception) {
                    
                }
            }
        }
    }
}