package com.jetbrains.kmpapp.screens.admin

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.jetbrains.kmpapp.data.FirebaseService
import com.jetbrains.kmpapp.data.UserData
import com.jetbrains.kmpapp.data.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminUiState(
    val isLoading: Boolean = false,
    val users: List<UserData> = emptyList(),
    val error: String = "",
    val isAdmin: Boolean = false
)

class AdminScreenModel(
    private val firebaseService: FirebaseService
) : ScreenModel {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        checkAdminAccess()
        loadUsers()
    }

    private fun checkAdminAccess() {
        screenModelScope.launch {
            firebaseService.getCurrentUserRole()
                .onSuccess { role ->
                    _uiState.update { it.copy(isAdmin = role == UserRole.ADMIN) }
                    if (role != UserRole.ADMIN) {
                        _uiState.update { it.copy(error = "Unauthorized. Admin access required.") }
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message ?: "Failed to check admin status") }
                }
        }
    }

    fun loadUsers() {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            firebaseService.getAllUsers()
                .onSuccess { userList ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            users = userList,
                            error = ""
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load users"
                        )
                    }
                }
        }
    }

    fun updateUserRole(userId: String, newRole: UserRole) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            firebaseService.updateUserRole(userId, newRole)
                .onSuccess {
                    loadUsers()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to update user role"
                        )
                    }
                }
        }
    }
}