
package com.jetbrains.kmpapp.screens.manufacturer

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.jetbrains.kmpapp.ui.theme.InLockBlue
import com.jetbrains.kmpapp.ui.theme.InLockTheme

actual class ManufacturerDashboardScreenCommon : Screen {
    @Composable
    actual override fun Content() {
        InLockTheme {
            val navigator = LocalNavigator.currentOrThrow

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Manufacturer Dashboard") },
                        navigationIcon = {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Manufacturer Dashboard",
                            style = MaterialTheme.typography.h5,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Text(
                            text = "This feature is not available on iOS in this version.",
                            style = MaterialTheme.typography.body1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        Button(
                            onClick = { navigator.pop() },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = InLockBlue
                            )
                        ) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }
    }
}