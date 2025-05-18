package com.jetbrains.kmpapp.screens.qrcode

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.jetbrains.kmpapp.screens.main.EnhancedMainScreen
import com.jetbrains.kmpapp.ui.components.EnhancedBottomNavDestination
import com.jetbrains.kmpapp.ui.icons.AppIcon
import com.jetbrains.kmpapp.ui.icons.AppIcons
import com.jetbrains.kmpapp.ui.theme.*
import kotlinx.coroutines.flow.collectLatest

class QRCodeScreenMVI : Screen {
    @Composable
    override fun Content() {
        InLockTheme {
            val navigator = LocalNavigator.currentOrThrow
            val model = getScreenModel<QRCodeModel>()
            val state by model.state.collectAsState()
            val scrollState = rememberScrollState()
            
            LaunchedEffect(Unit) {
                EnhancedMainScreen.currentDestination.value = 
                    EnhancedBottomNavDestination.TRANSFER
            }
            
            LaunchedEffect(Unit) {
                model.sideEffects.collectLatest { effect ->
                    when (effect) {
                        is QRCodeSideEffect.NavigateToTransfer -> {
                            navigator.push(TransferScreen(effect.userId))
                        }
                        is QRCodeSideEffect.ShowErrorToast -> {
                        }
                    }
                }
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Share & Scan") },
                        backgroundColor = InLockBlue,
                        contentColor = Color.White
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, InLockBlue), RoundedCornerShape(8.dp))
                    ) {
                        TabButton(
                            text = "Share My QR",
                            isSelected = !state.isScanMode,
                            onClick = { model.processIntent(QRCodeIntent.SetScanMode(false)) },
                            modifier = Modifier.weight(1f)
                        )
                        TabButton(
                            text = "Scan QR",
                            isSelected = state.isScanMode,
                            onClick = { model.processIntent(QRCodeIntent.SetScanMode(true)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    if (state.isScanMode) {
                        ScanQRContent(
                            state = state,
                            onStartScan = { model.processIntent(QRCodeIntent.StartQRScan) },
                            onStopScan = { model.processIntent(QRCodeIntent.StopQRScan) },
                            onTransferToUser = { userId -> model.navigateToTransfer(userId) }
                        )
                    } else {
                        ShareQRContent(state = state)
                    }
                    
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }

    @Composable
    private fun TabButton(
        text: String,
        isSelected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Box(
            modifier = modifier
                .background(
                    if (isSelected) InLockBlue else Color.Transparent,
                    RoundedCornerShape(8.dp)
                )
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (isSelected) Color.White else InLockBlue,
                fontWeight = FontWeight.Bold
            )
        }
    }

    @Composable
    private fun ShareQRContent(state: QRCodeState) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Your QR Code",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Share this QR code with others to let them transfer assets to you",
                textAlign = TextAlign.Center,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .size(250.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(BorderStroke(4.dp, InLockBlue), RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.userQRCode != null) {
                    QRCodeImage(state.userQRCode)
                } else {
                    if (state.isLoading) {
                        CircularProgressIndicator(color = InLockBlue)
                    } else {
                        Text(
                            "QR Code could not be generated",
                            color = Color.Red,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.userId.isNotEmpty()) {
                Text(
                    "Your ID: ${state.userId}",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "Others can scan this QR code to transfer assets to you",
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
    
    @Composable
    private fun ScanQRContent(
        state: QRCodeState,
        onStartScan: () -> Unit,
        onStopScan: () -> Unit,
        onTransferToUser: (String) -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Scan QR Code",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Scan someone else's QR code to transfer assets to them",
                textAlign = TextAlign.Center,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .size(250.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        BorderStroke(4.dp, if (state.isScanning) InLockBlue else Color.Gray),
                        RoundedCornerShape(12.dp)
                    )
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                if (state.isScanning) {
                    QRScannerPreview()
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AppIcon(
                            icon = AppIcons.QrCode,
                            contentDescription = "QR Code",
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "Press the button below to start scanning",
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { if (state.isScanning) onStopScan() else onStartScan() },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (state.isScanning) Color.Red else InLockBlue
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    if (state.isScanning) "Cancel Scanning" else "Start Scanning",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = state.error.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    state.error,
                    color = MaterialTheme.colors.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }

            AnimatedVisibility(
                visible = state.scannedUserId.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "User ID Scanned",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.subtitle1
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            state.scannedUserId,
                            style = MaterialTheme.typography.body1
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { onTransferToUser(state.scannedUserId) },
                            colors = ButtonDefaults.buttonColors(backgroundColor = InLockBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text(
                                "Transfer Asset to This User",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}