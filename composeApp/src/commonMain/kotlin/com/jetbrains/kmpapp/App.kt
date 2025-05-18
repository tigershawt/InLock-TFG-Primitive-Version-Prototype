package com.jetbrains.kmpapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import com.jetbrains.kmpapp.data.FirebaseService
import com.jetbrains.kmpapp.di.initQrCodeModule
import com.jetbrains.kmpapp.screens.login.LoginScreen
import com.jetbrains.kmpapp.screens.main.EnhancedMainScreen
import com.jetbrains.kmpapp.ui.components.NavigationManager
import com.jetbrains.kmpapp.ui.components.NavigationStateProvider
import com.jetbrains.kmpapp.ui.theme.InLockBlue
import com.jetbrains.kmpapp.ui.theme.InLockTheme
import com.jetbrains.kmpapp.utils.getCurrentTimeMillis
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object AppState {
    var appResetCounter = 0
        private set

    var isPostLogout = false
        private set

    fun forceReset() {
        isPostLogout = true
        appResetCounter++
    }

    fun clearPostLogout() {
        isPostLogout = false
    }
}

class AppComponent : KoinComponent {
    private val firebaseService: FirebaseService by inject()

    init {
        initQrCodeModule()
    }

    @Composable
    fun App() {
        val resetCounter = remember { mutableStateOf(AppState.appResetCounter) }
        var isUserSignedIn by remember { mutableStateOf(false) }
        var isChecking by remember { mutableStateOf(true) }

        LaunchedEffect(AppState.appResetCounter) {
            if (resetCounter.value != AppState.appResetCounter) {
                resetCounter.value = AppState.appResetCounter

                NavigationManager.reset()
            }
        }

        LaunchedEffect(resetCounter.value) {
            if (AppState.isPostLogout) {
                EnhancedMainScreen.resetNavigation()
                NavigationManager.hideBottomNavigation()
                AppState.clearPostLogout()
            }

            isUserSignedIn = firebaseService.isUserSignedIn()

            if (isUserSignedIn) {
                NavigationManager.showBottomNavigation()
            } else {
                NavigationManager.hideBottomNavigation()
            }
            
            isChecking = false
        }

        val uiKey = "app_${resetCounter.value}_${getCurrentTimeMillis()}"

        NavigationStateProvider {
            InLockTheme {
                if (isChecking) {
                    SplashScreen()
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUserSignedIn) {
                            Navigator(
                                EnhancedMainScreen(
                                    screenKey = "main_${resetCounter.value}_${getCurrentTimeMillis()}"
                                )
                            )
                        } else {
                            LaunchedEffect(Unit) {
                                NavigationManager.hideBottomNavigation()
                            }
                            Navigator(LoginScreen)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SplashScreen() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "InLock",
                style = MaterialTheme.typography.h3,
                color = InLockBlue
            )
        }
    }
}

@Composable
fun App() {
    AppComponent().App()
}