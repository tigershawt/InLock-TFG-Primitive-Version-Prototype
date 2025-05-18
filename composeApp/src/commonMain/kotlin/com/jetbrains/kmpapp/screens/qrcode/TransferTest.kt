package com.jetbrains.kmpapp.screens.qrcode

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.jetbrains.kmpapp.ui.theme.InLockBlue
import com.jetbrains.kmpapp.ui.theme.InLockTheme

class TransferTest : Screen {
    @Composable
    override fun Content() {
        InLockTheme {
            var testRecipientId by remember { mutableStateOf("") }
            
            Navigator(TestSelectionScreen)
        }
    }
}

object TestSelectionScreen : Screen {
    @Composable
    override fun Content() {
        InLockTheme {
            val navigator = LocalNavigator.currentOrThrow
            val testRecipientId = "test_recipient_123"
            
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Transfer Implementation Test") },
                        backgroundColor = InLockBlue,
                        contentColor = Color.White
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Transfer Implementation Test Suite",
                        style = MaterialTheme.typography.h5,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        "This test suite allows you to validate and compare the different implementations of the Transfer screen.",
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Text("Test with fixed test data (mock mode):", fontWeight = FontWeight.Bold)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(onClick = { navigator.push(TransferPatternSelector(testRecipientId)) }) {
                        Text("Launch Transfer Screen")
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Text("Test with QR code scanner:", fontWeight = FontWeight.Bold)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(onClick = { navigator.push(QRCodeScreen()) }) {
                        Text("Launch QR Code Scanner")
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Text(
                        "Implementation Details:",
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        "Original Implementation: Uses traditional ScreenModel with UI state flow.",
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        "MVI Implementation: Uses MVI architecture with intents, state, and side effects.",
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}