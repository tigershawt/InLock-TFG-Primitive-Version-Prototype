package com.jetbrains.kmpapp.screens.login

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.jetbrains.kmpapp.data.FirebaseService
import com.jetbrains.kmpapp.data.UserData
import com.jetbrains.kmpapp.data.UserRole
import com.jetbrains.kmpapp.utils.SecurityUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: String = "",
    val isRegistrationMode: Boolean = false,
    val userData: UserData? = null
)

class LoginScreenModel(
    private val firebaseService: FirebaseService
) : ScreenModel {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val TAG = "LoginScreenModel"

    init {
        checkCurrentUser()
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Please fill in all fields") }
            return
        }

        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = "") }

            firebaseService.signIn(email, password)
                .onSuccess { userId ->
                    firebaseService.getUserData(userId)
                        .onSuccess { userData ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isAuthenticated = true,
                                    userData = userData
                                )
                            }
                        }
                        .onFailure { error ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isAuthenticated = true
                                )
                            }
                        }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "Sign in failed")
                    }
                }
        }
    }

    fun signUp(email: String, password: String, displayName: String, username: String) {
        if (email.isBlank() || password.isBlank() || displayName.isBlank() || username.isBlank()) {
            _uiState.update { it.copy(error = "Please fill in all fields") }
            return
        }

        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = "") }

            firebaseService.signUp(email, password, displayName, username)
                .onSuccess { userId ->
                    val userData = UserData(
                        id = userId,
                        email = email,
                        displayName = displayName,
                        username = username,
                        role = UserRole.USER
                    )

                    SecurityUtils.getCurrentUserData()

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            userData = userData
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "Sign up failed")
                    }
                }
        }
    }

    fun signInAnonymously() {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = "") }

            firebaseService.signInAnonymously()
                .onSuccess { userId ->
                    _uiState.update {
                        it.copy(isLoading = false, isAuthenticated = true)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "Anonymous sign in failed")
                    }
                }
        }
    }

    fun toggleRegistrationMode() {
        _uiState.update { it.copy(isRegistrationMode = !it.isRegistrationMode, error = "") }
    }

    fun isUserSignedIn(): Boolean {
        return firebaseService.isUserSignedIn()
    }

    fun checkCurrentUser() {
        screenModelScope.launch {
            if (firebaseService.isUserSignedIn()) {
                val userId = firebaseService.getCurrentUserId()
                if (userId != null) {
                    firebaseService.getUserData(userId)
                        .onSuccess { userData ->
                            _uiState.update {
                                it.copy(isAuthenticated = true, userData = userData)
                            }
                        }
                        .onFailure { error ->
                            _uiState.update {
                                it.copy(isAuthenticated = true)
                            }
                        }
                } else {
                    _uiState.update { it.copy(isAuthenticated = true) }
                }
            }
        }
    }

    override fun onDispose() {
        super.onDispose()
    }
}