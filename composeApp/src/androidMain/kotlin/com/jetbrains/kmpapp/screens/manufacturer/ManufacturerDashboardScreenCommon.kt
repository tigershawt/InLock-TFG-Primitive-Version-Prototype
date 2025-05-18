package com.jetbrains.kmpapp.screens.manufacturer

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen

actual class ManufacturerDashboardScreenCommon : Screen {
    private val manufacturerDashboardScreen = ManufacturerDashboardScreen()

    @Composable
    actual override fun Content() {
        manufacturerDashboardScreen.Content()
    }
}