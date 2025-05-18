package com.jetbrains.kmpapp.screens.nfc

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen

expect class NfcScreenCommon() : Screen {
    @Composable
    override fun Content()
}