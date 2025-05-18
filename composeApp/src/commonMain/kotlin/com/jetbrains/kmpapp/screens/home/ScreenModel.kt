package com.jetbrains.kmpapp.screens.home

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.jetbrains.kmpapp.data.FirebaseService
import com.jetbrains.kmpapp.data.UserData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val isSignedOut: Boolean = false,
    val isAnonymous: Boolean = false,
    val userData: UserData? = null,
    val error: String = ""
)

class HomeScreenModel(
    private val firebaseService: FirebaseService
) : ScreenModel {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
    }

    fun signOut() {
        screenModelScope.launch {
            firebaseService.signOut()
            _uiState.update { it.copy(isSignedOut = true) }
        }
    }

    private fun loadUserData() {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val isAnonymous = firebaseService.isCurrentUserAnonymous()
            _uiState.update { it.copy(isAnonymous = isAnonymous) }

            if (!isAnonymous) {
                val userId = firebaseService.getCurrentUserId()
                if (userId != null) {
                    firebaseService.getUserData(userId)
                        .onSuccess { userData ->
                            _uiState.update {
                                it.copy(isLoading = false, userData = userData)
                            }
                        }
                        .onFailure { error ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = error.message ?: "Failed to load user data"
                                )
                            }
                        }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}