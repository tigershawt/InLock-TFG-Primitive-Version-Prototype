package com.jetbrains.kmpapp.screens.qrcode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.jetbrains.kmpapp.ui.theme.InLockBlue
import com.jetbrains.kmpapp.ui.theme.InLockTheme

class TransferPatternSelector(private val recipientUserId: String) : Screen {
    
    @Composable
    override fun Content() {
        InLockTheme {
            val navigator = LocalNavigator.currentOrThrow
            
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Choose Implementation") },
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
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Choose an implementation to test",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))

                    ImplementationCard(
                        title = "Original Implementation",
                        description = "Uses traditional ScreenModel with UI state flow",
                        onClick = {
                            navigator.push(TransferScreen(recipientUserId))
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    ImplementationCard(
                        title = "MVI Pattern Implementation",
                        description = "Uses MVI architecture with intents, state, and side effects",
                        onClick = {
                            navigator.push(TransferScreen_MVI(recipientUserId))
                        }
                    )
                }
            }
        }
    }
    
    @Composable
    private fun ImplementationCard(
        title: String,
        description: String,
        onClick: () -> Unit
    ) {
        Card(
            elevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = InLockBlue
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    description,
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Launch", color = Color.White)
                }
            }
        }
    }
}