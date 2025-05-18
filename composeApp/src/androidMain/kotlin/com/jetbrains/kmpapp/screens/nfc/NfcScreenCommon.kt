package com.jetbrains.kmpapp.screens.nfc

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen

actual class NfcScreenCommon : Screen {
    private val nfcScreen = EnhancedNfcScreen()

    @Composable
    actual override fun Content() {
        nfcScreen.Content()
    }
}