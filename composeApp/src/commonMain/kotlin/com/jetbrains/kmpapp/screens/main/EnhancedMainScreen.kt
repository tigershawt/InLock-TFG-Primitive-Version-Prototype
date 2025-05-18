package com.jetbrains.kmpapp.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.jetbrains.kmpapp.data.FirebaseService
import com.jetbrains.kmpapp.data.UserRole
import com.jetbrains.kmpapp.screens.admin.AdminScreen
import com.jetbrains.kmpapp.screens.assets.EnhancedAssetsScreen
import com.jetbrains.kmpapp.screens.manufacturer.ManufacturerDashboardScreenCommon
import com.jetbrains.kmpapp.screens.nfc.NfcScreenCommon
import com.jetbrains.kmpapp.screens.profile.ProfileScreen
import com.jetbrains.kmpapp.screens.qrcode.QRCodeScreen
import com.jetbrains.kmpapp.ui.components.EnhancedBottomNavDestination
import com.jetbrains.kmpapp.ui.components.EnhancedBottomNavigation
import com.jetbrains.kmpapp.ui.theme.InLockTheme
import com.jetbrains.kmpapp.utils.SecurityUtils
import com.jetbrains.kmpapp.utils.getCurrentTimeMillis
import kotlin.jvm.JvmStatic
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class EnhancedMainScreen(
    initialDestination: EnhancedBottomNavDestination = EnhancedBottomNavDestination.ASSETS,
    private val screenKey: String = "main_${getCurrentTimeMillis()}"
) : Screen, KoinComponent {

    companion object {
        private const val serialVersionUID = 1L

        @JvmStatic
        var _currentDestination = EnhancedBottomNavDestination.ASSETS
        
        @JvmStatic
        val currentDestination = mutableStateOf(_currentDestination)
        
        @JvmStatic
        var isPostLogout = false
        
        @JvmStatic
        var userRole: UserRole? = null
        
        @JvmStatic
        var uiRecreationKey = getCurrentTimeMillis()
        
        @JvmStatic
        var shouldClearBottomNav = false
        
        @JvmStatic
        fun setDestination(destination: EnhancedBottomNavDestination) {
            _currentDestination = destination
            currentDestination.value = destination
        }
        
        @JvmStatic
        fun resetNavigation() {
            _currentDestination = EnhancedBottomNavDestination.ASSETS
            currentDestination.value = EnhancedBottomNavDestination.ASSETS
            userRole = null
            isPostLogout = true
        }
        
        @JvmStatic
        fun forceRecreateAllUI() {
            uiRecreationKey = getCurrentTimeMillis()
            shouldClearBottomNav = true
            isPostLogout = true
        }
        
        @JvmStatic
        fun clearPostLogoutFlag() {
            isPostLogout = false
            shouldClearBottomNav = false
        }
    }

    private val firebaseService: FirebaseService by inject()

    private val _currentDestinationUpdated = mutableStateOf(false)

    override val key: String = screenKey
    
    init {
        setCurrentDestination(initialDestination)
    }
    
    fun setCurrentDestination(destination: EnhancedBottomNavDestination) {
        EnhancedMainScreen._currentDestination = destination
        EnhancedMainScreen.currentDestination.value = destination
        _currentDestinationUpdated.value = !_currentDestinationUpdated.value
    }

    @Composable
    override fun Content() {
        InLockTheme {
            val coroutineScope = rememberCoroutineScope()
            
            val refreshKey = remember { mutableStateOf(if (isPostLogout) 1 else 0) }
            
            var currentDestination by remember { mutableStateOf(EnhancedMainScreen._currentDestination) }
            
            val globalDestination by EnhancedMainScreen.currentDestination
            LaunchedEffect(Unit) {
                if (isPostLogout) {
                    clearPostLogoutFlag()
                    refreshKey.value = refreshKey.value + 1
                }
            }
            
            LaunchedEffect(globalDestination) {
                if (currentDestination != globalDestination) {
                    currentDestination = globalDestination
                }
            }
            
            var currentUserRole by remember { mutableStateOf<UserRole?>(null) }
            val mainNavigator = LocalNavigator.currentOrThrow

            LaunchedEffect(refreshKey.value) {
                try {
                    val userRole = SecurityUtils.getCurrentUserRole()
                    currentUserRole = userRole
                    EnhancedMainScreen.userRole = userRole

                    val isManufacturer = firebaseService.isManufacturer()
                    val isAdmin = firebaseService.isAdmin()

                    if ((isManufacturer || isAdmin) && userRole == null) {
                        val refreshedData = SecurityUtils.refreshUserData()
                        currentUserRole = refreshedData?.role
                        EnhancedMainScreen.userRole = refreshedData?.role
                    }
                } catch (e: Exception) {
                    currentUserRole = UserRole.USER
                    EnhancedMainScreen.userRole = UserRole.USER
                }
            }

            val isNavigationVisible = com.jetbrains.kmpapp.ui.components.NavigationManager.isNavigationVisible
            
            LaunchedEffect(Unit) {
                if (!shouldClearBottomNav) {
                    com.jetbrains.kmpapp.ui.components.NavigationManager.showBottomNavigation()
                }
            }
            
            Scaffold(
                bottomBar = {
                    if (isNavigationVisible) {
                        EnhancedBottomNavigation(
                            currentDestination = currentDestination,
                            onDestinationSelected = { destination ->
                                currentDestination = destination
                                setCurrentDestination(destination)
                            }
                        )
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = padding.calculateBottomPadding())
                ) {
                    when (currentDestination) {
                        EnhancedBottomNavDestination.ASSETS -> {
                            Navigator(EnhancedAssetsScreen())
                        }
                        EnhancedBottomNavDestination.SCANNER -> {
                            Navigator(NfcScreenCommon())
                        }
                        EnhancedBottomNavDestination.PROFILE -> {
                            Navigator(ProfileScreen())
                        }
                        EnhancedBottomNavDestination.TRANSFER -> {
                            Navigator(QRCodeScreen())
                        }
                        EnhancedBottomNavDestination.ADMIN -> {
                            if (firebaseService.isAdmin() || currentUserRole == UserRole.ADMIN) {
                                Navigator(AdminScreen())
                            } else {
                                coroutineScope.launch {
                                    currentDestination = EnhancedBottomNavDestination.PROFILE
                                    setCurrentDestination(EnhancedBottomNavDestination.PROFILE)
                                }
                                Navigator(ProfileScreen())
                            }
                        }
                        EnhancedBottomNavDestination.MANUFACTURER -> {
                            if (firebaseService.isManufacturer() || currentUserRole == UserRole.MANUFACTURER || currentUserRole == UserRole.ADMIN) {
                                Navigator(ManufacturerDashboardScreenCommon())
                            } else {
                                coroutineScope.launch {
                                    currentDestination = EnhancedBottomNavDestination.PROFILE
                                    setCurrentDestination(EnhancedBottomNavDestination.PROFILE)
                                }
                                Navigator(ProfileScreen())
                            }
                        }
                    }
                }
            }
        }
    }
}