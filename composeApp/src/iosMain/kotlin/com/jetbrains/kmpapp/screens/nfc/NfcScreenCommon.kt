package com.jetbrains.kmpapp.screens.nfc

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen

actual class NfcScreenCommon : Screen {
    @Composable
    actual override fun Content() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "NFC functionality is not available on iOS in this version",
                style = MaterialTheme.typography.h6,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}