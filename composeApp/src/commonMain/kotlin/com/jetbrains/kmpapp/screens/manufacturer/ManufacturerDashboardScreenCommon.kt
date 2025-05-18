
package com.jetbrains.kmpapp.screens.manufacturer

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen

expect class ManufacturerDashboardScreenCommon() : Screen {
    @Composable
    override fun Content()
}

