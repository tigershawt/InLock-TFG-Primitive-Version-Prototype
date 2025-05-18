
package com.jetbrains.kmpapp.screens.profile

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.jetbrains.kmpapp.AppState
import com.jetbrains.kmpapp.data.BlockchainService
import com.jetbrains.kmpapp.data.FirebaseService
import com.jetbrains.kmpapp.data.UserData
import com.jetbrains.kmpapp.data.UserRole
import com.jetbrains.kmpapp.screens.main.EnhancedMainScreen
import com.jetbrains.kmpapp.utils.SecurityUtils
import com.jetbrains.kmpapp.utils.getDeviceId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val isSignedOut: Boolean = false,
    val userData: UserData? = null,
    val error: String = "",
    val tokenBalance: Int = 0,
    val assetsCount: Int = 0,
    val isGuest: Boolean = false,
    val deviceId: String = "",
    val userId: String = ""
)

class ProfileScreenModel(
    private val firebaseService: FirebaseService,
    private val blockchainService: BlockchainService
) : ScreenModel {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val TAG = "ProfileScreenModel"

    fun loadUserProfile() {
        screenModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = "") }

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
                
                val deviceId = getDeviceId()
                
                val isAnonymous = firebaseService.isCurrentUserAnonymous()
                
                if (isAnonymous) {
                    val guestUserData = UserData(
                        id = userId,
                        email = "guest@inlock.app",
                        displayName = "Guest User",
                        username = "guest",
                        role = UserRole.USER
                    )
                    
                    _uiState.update {
                        it.copy(
                            userData = guestUserData,
                            isGuest = true,
                            deviceId = deviceId,
                            userId = userId,
                            isLoading = false
                        )
                    }
                    loadBlockchainData(userId)
                } else {
                    firebaseService.getUserData(userId)
                        .onSuccess { userData ->
                            _uiState.update {
                                it.copy(
                                    userData = userData,
                                    isGuest = false,
                                    deviceId = deviceId,
                                    userId = userId
                                )
                            }
                            loadBlockchainData(userId)
                        }
                        .onFailure { error ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = error.message ?: "Failed to load user data"
                                )
                            }
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

    private suspend fun loadBlockchainData(userId: String) {
        try {
            blockchainService.getUserBalance(userId)
                .onSuccess { balance ->
                    _uiState.update {
                        it.copy(tokenBalance = balance)
                    }
                }
                .onFailure { error ->
                }

            blockchainService.getUserAssets(userId)
                .onSuccess { assets ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            assetsCount = assets.size
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false
                        )
                    }
                }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false
                )
            }
        }
    }

    fun signOut() {
        screenModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                firebaseService.signOut()
                
                SecurityUtils.clearUserData()
                
                com.jetbrains.kmpapp.ui.components.NavigationManager.hideBottomNavigation()
                
                com.jetbrains.kmpapp.ui.components.hideBottomNav()
                
                AppState.forceReset()
                
                kotlinx.coroutines.delay(100)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSignedOut = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to sign out"
                    )
                }
            }
        }
    }
}