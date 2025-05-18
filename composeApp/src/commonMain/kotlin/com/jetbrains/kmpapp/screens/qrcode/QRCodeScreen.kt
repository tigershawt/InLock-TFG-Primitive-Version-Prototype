package com.jetbrains.kmpapp.screens.qrcode

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import com.jetbrains.kmpapp.ui.components.EnhancedCollapsibleHeader
import com.jetbrains.kmpapp.ui.icons.AppIcon
import com.jetbrains.kmpapp.ui.icons.AppIcons
import com.jetbrains.kmpapp.ui.theme.*

class QRCodeScreen : Screen {
    @Composable
    override fun Content() {
        InLockTheme {
            val navigator = LocalNavigator.currentOrThrow
            val screenModel = getScreenModel<QRCodeScreenModel>()
            val uiState by screenModel.uiState.collectAsState()
            val scrollState = rememberScrollState()
            
            LaunchedEffect(Unit) {
                EnhancedMainScreen.currentDestination.value = 
                    EnhancedBottomNavDestination.TRANSFER
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
                            isSelected = !uiState.isScanMode,
                            onClick = { screenModel.setMode(false) },
                            modifier = Modifier.weight(1f)
                        )
                        TabButton(
                            text = "Scan QR",
                            isSelected = uiState.isScanMode,
                            onClick = { screenModel.setMode(true) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    if (uiState.isScanMode) {
                        ScanQRContent(
                            uiState = uiState,
                            onStartScan = { screenModel.startQRScan() },
                            onStopScan = { screenModel.stopQRScan() },
                            onTransferToUser = { userId ->
                                navigator.push(TransferPatternSelector(userId))
                            }
                        )
                    } else {
                        ShareQRContent(uiState = uiState)
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
    private fun ShareQRContent(uiState: QRCodeUiState) {
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
                if (uiState.userQRCode != null) {
                    QRCodeImage(uiState.userQRCode)
                } else {
                    if (uiState.isLoading) {
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

            if (uiState.userId.isNotEmpty()) {
                Text(
                    "Your ID: ${uiState.userId}",
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
        uiState: QRCodeUiState,
        onStartScan: () -> Unit,
        onStopScan: () -> Unit,
        onTransferToUser: (String) -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScanQRHeader()
            
            Spacer(modifier = Modifier.height(32.dp))
            
            QRScannerContainer(isScanning = uiState.isScanning)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            ScanControlButton(
                isScanning = uiState.isScanning,
                onStartScan = onStartScan,
                onStopScan = onStopScan
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ErrorMessage(error = uiState.error)
            
            ScannedResultCard(
                scannedUserId = uiState.scannedUserId,
                onTransferToUser = onTransferToUser
            )
        }
    }
    
    @Composable
    private fun ScanQRHeader() {
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
    }
    
    @Composable
    private fun QRScannerContainer(isScanning: Boolean) {
        Box(
            modifier = Modifier
                .size(250.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    BorderStroke(4.dp, if (isScanning) InLockBlue else Color.Gray),
                    RoundedCornerShape(12.dp)
                )
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            if (isScanning) {
                QRScannerPreview()
            } else {
                ScannerPlaceholder()
            }
        }
    }
    
    @Composable
    private fun ScannerPlaceholder() {
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
    
    @Composable
    private fun ScanControlButton(
        isScanning: Boolean,
        onStartScan: () -> Unit,
        onStopScan: () -> Unit
    ) {
        Button(
            onClick = { if (isScanning) onStopScan() else onStartScan() },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (isScanning) Color.Red else InLockBlue
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                if (isScanning) "Cancel Scanning" else "Start Scanning",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
    
    @Composable
    private fun ErrorMessage(error: String) {
        AnimatedVisibility(
            visible = error.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                error,
                color = MaterialTheme.colors.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
    
    @Composable
    private fun ScannedResultCard(
        scannedUserId: String,
        onTransferToUser: (String) -> Unit
    ) {
        AnimatedVisibility(
            visible = scannedUserId.isNotEmpty(),
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
                        scannedUserId,
                        style = MaterialTheme.typography.body1
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onTransferToUser(scannedUserId) },
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
