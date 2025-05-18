package com.jetbrains.kmpapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

object NavigationManager {
    private val _navigationVisibleState = mutableStateOf(false)
    val isNavigationVisible get() = _navigationVisibleState.value

    fun showBottomNavigation() {
        _navigationVisibleState.value = true
    }

    fun hideBottomNavigation() {
        _navigationVisibleState.value = false
    }

    fun reset() {
        _navigationVisibleState.value = false
    }
}

val LocalNavigationVisibility = compositionLocalOf { false }


@Composable
fun NavigationStateProvider(
    content: @Composable () -> Unit
) {
    val isNavigationVisible by remember { mutableStateOf(NavigationManager.isNavigationVisible) }

    CompositionLocalProvider(
        LocalNavigationVisibility provides isNavigationVisible
    ) {
        content()
    }
}