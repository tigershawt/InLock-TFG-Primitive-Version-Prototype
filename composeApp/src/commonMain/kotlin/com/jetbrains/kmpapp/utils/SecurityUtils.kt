package com.jetbrains.kmpapp.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.jetbrains.kmpapp.data.FirebaseService
import com.jetbrains.kmpapp.data.UserData
import com.jetbrains.kmpapp.data.UserRole
import com.jetbrains.kmpapp.screens.login.LoginScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object SecurityUtils : KoinComponent {

    private val firebaseService: FirebaseService by inject()
    private val _currentUserData = MutableStateFlow<UserData?>(null)
    val currentUserData: StateFlow<UserData?> = _currentUserData.asStateFlow()
    private var isInitialized = false

    fun isAuthenticated(): Boolean {
        return firebaseService.isUserSignedIn()
    }

    suspend fun hasRequiredRole(requiredRole: UserRole): Boolean {
        if (!isAuthenticated()) return false

        val userData = getCurrentUserData() ?: return false

        return when (requiredRole) {
            UserRole.USER -> true
            UserRole.MANUFACTURER -> userData.role == UserRole.MANUFACTURER || userData.role == UserRole.ADMIN
            UserRole.ADMIN -> userData.role == UserRole.ADMIN
        }
    }

    suspend fun refreshUserData(): UserData? {
        val userId = firebaseService.getCurrentUserId() ?: return null

        _currentUserData.update { null }

        val userData = firebaseService.getUserData(userId).getOrNull()

        if (userData != null) {
            _currentUserData.update { userData }
        }

        return userData
    }

    suspend fun getCurrentUserData(): UserData? {
        if (!isInitialized && _currentUserData.value == null) {
            isInitialized = true
            refreshUserData()
        }

        if (_currentUserData.value != null) {
            return _currentUserData.value
        }

        val userId = firebaseService.getCurrentUserId() ?: return null
        val userData = firebaseService.getUserData(userId).getOrNull()

        if (userData != null) {
            _currentUserData.update { userData }
        }

        return userData
    }

    suspend fun getCurrentUserRole(): UserRole? {
        val cachedData = getCurrentUserData()

        if (cachedData != null) {
            return cachedData.role
        }

        try {
            val firebaseRole = firebaseService.getCurrentUserRole().getOrNull()

            if (firebaseRole != null && _currentUserData.value == null) {
                refreshUserData()
            }

            return firebaseRole
        } catch (e: Exception) {
            return null
        }
    }

    fun isCurrentUserManufacturer(): Boolean {
        return firebaseService.isManufacturer()
    }

    @Composable
    fun RequireRole(
        requiredRole: UserRole,
        redirectTo: Screen = LoginScreen,
        content: @Composable () -> Unit
    ) {
        val navigator = LocalNavigator.currentOrThrow
        val userData by currentUserData.collectAsState()

        LaunchedEffect(Unit) {
            val hasPermission = hasRequiredRole(requiredRole)
            if (!hasPermission) {
                navigator.replace(redirectTo)
            }
        }

        if (userData != null) {
            val userRole = userData?.role
            when {
                userRole == requiredRole ||
                        (requiredRole == UserRole.USER) ||
                        (requiredRole == UserRole.MANUFACTURER && userRole == UserRole.ADMIN) -> {
                    content()
                }
            }
        }
    }

    fun clearUserData() {
        _currentUserData.update { null }
        isInitialized = false
    }
}