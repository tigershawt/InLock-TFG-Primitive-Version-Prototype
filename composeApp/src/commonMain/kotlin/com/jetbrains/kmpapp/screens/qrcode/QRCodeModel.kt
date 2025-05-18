package com.jetbrains.kmpapp.screens.qrcode

import com.jetbrains.kmpapp.data.repository.QRCodeRepository
import com.jetbrains.kmpapp.data.repository.QRCodeRequest
import com.jetbrains.kmpapp.data.repository.QRCodeResponse
import com.jetbrains.kmpapp.mvi.MviModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect

class QRCodeModel(
    private val qrCodeRepository: QRCodeRepository
) : MviModel<QRCodeState, QRCodeIntent, QRCodeSideEffect>() {

    init {
        processIntent(QRCodeIntent.LoadUserData)
    }

    override fun initialState(): QRCodeState = QRCodeState()

    override suspend fun handleIntent(intent: QRCodeIntent) {
        when (intent) {
            is QRCodeIntent.LoadUserData -> loadUserData()
            is QRCodeIntent.SetScanMode -> setScanMode(intent.scanMode)
            is QRCodeIntent.StartQRScan -> startQRScan()
            is QRCodeIntent.StopQRScan -> stopQRScan()
            is QRCodeIntent.QRCodeScanned -> handleQRCodeScanned(intent.userId)
            is QRCodeIntent.SetError -> setError(intent.error)
            is QRCodeIntent.QRCodeGenerated -> setQRCodeData(intent.qrCodeData)
        }
    }

    private suspend fun loadUserData() {
        qrCodeRepository.execute(QRCodeRequest.GetCurrentUserId)
            .catch { e ->
                updateState {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "An unexpected error occurred"
                    )
                }
            }
            .collect { response ->
                when (response) {
                    is QRCodeResponse.UserId -> {
                        if (response.userId == null) {
                            updateState {
                                it.copy(isLoading = false, error = "User not signed in")
                            }
                        } else {
                            updateState { it.copy(userId = response.userId) }
                            generateQRCode(response.userId)
                        }
                    }
                    is QRCodeResponse.Error -> {
                        updateState {
                            it.copy(
                                isLoading = false,
                                error = response.message
                            )
                        }
                    }
                    else -> {}
                }
            }
    }

    private suspend fun generateQRCode(content: String) {
        updateState { it.copy(isLoading = true) }
        
        qrCodeRepository.execute(QRCodeRequest.GenerateQRCode(content))
            .catch { e ->
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Failed to generate QR code: ${e.message}"
                    )
                }
                emitSideEffect(QRCodeSideEffect.ShowErrorToast)
            }
            .collect { response ->
                when (response) {
                    is QRCodeResponse.QRCodeGenerated -> {
                        updateState {
                            it.copy(
                                isLoading = false,
                                userQRCode = response.qrCodeData,
                                error = ""
                            )
                        }
                    }
                    is QRCodeResponse.Error -> {
                        updateState {
                            it.copy(
                                isLoading = false,
                                error = response.message
                            )
                        }
                        emitSideEffect(QRCodeSideEffect.ShowErrorToast)
                    }
                    else -> {}
                }
            }
    }

    private fun setScanMode(scanMode: Boolean) {
        if (!scanMode && state.value.isScanMode) {
            updateState {
                it.copy(
                    isScanMode = false,
                    isScanning = false,
                    scannedUserId = "",
                    error = ""
                )
            }
        } else {
            updateState { it.copy(isScanMode = scanMode, error = "") }
        }
    }

    private suspend fun startQRScan() {
        updateState {
            it.copy(
                isScanning = true,
                scannedUserId = "",
                error = ""
            )
        }

        qrCodeRepository.execute(QRCodeRequest.StartQRScan)
            .catch { e ->
                updateState {
                    it.copy(
                        isScanning = false,
                        error = e.message ?: "Failed to start QR scan"
                    )
                }
            }
            .collect { response ->
                when (response) {
                    is QRCodeResponse.ScannedResult -> {
                        processIntent(QRCodeIntent.QRCodeScanned(response.userId))
                    }
                    is QRCodeResponse.Error -> {
                        processIntent(QRCodeIntent.SetError(response.message))
                    }
                    else -> {}
                }
            }
    }

    private fun handleQRCodeScanned(userId: String) {
        updateState {
            it.copy(
                scannedUserId = userId,
                isScanning = false,
                error = ""
            )
        }
    }

    private suspend fun stopQRScan() {
        qrCodeRepository.execute(QRCodeRequest.StopQRScan)
            .catch { e ->
                updateState {
                    it.copy(
                        isScanning = false,
                        error = e.message ?: "Failed to stop QR scan"
                    )
                }
            }
            .collect { response ->
                when (response) {
                    is QRCodeResponse.ScanStopped -> {
                        updateState { it.copy(isScanning = false) }
                    }
                    is QRCodeResponse.Error -> {
                        updateState {
                            it.copy(
                                isScanning = false,
                                error = response.message
                            )
                        }
                    }
                    else -> {}
                }
            }
    }

    private fun setError(error: String) {
        updateState { it.copy(error = error, isScanning = false) }
    }

    private fun setQRCodeData(qrCodeData: ByteArray) {
        updateState { it.copy(userQRCode = qrCodeData, isLoading = false) }
    }

    fun navigateToTransfer(userId: String) {
        launchInScope {
            emitSideEffect(QRCodeSideEffect.NavigateToTransfer(userId))
        }
    }

    override fun onDispose() {
        super.onDispose()
        if (state.value.isScanning) {
            launchInScope {
                try {
                    qrCodeRepository.execute(QRCodeRequest.StopQRScan).collect()
                } catch (e: Exception) {
                }
            }
        }
    }
}