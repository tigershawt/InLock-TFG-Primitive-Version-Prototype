package com.jetbrains.kmpapp.screens.nfc

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.jetbrains.kmpapp.data.OwnershipRecord
import com.jetbrains.kmpapp.ui.icons.AppIcon
import com.jetbrains.kmpapp.ui.icons.AppIcons
import com.jetbrains.kmpapp.ui.theme.InLockBlue
import com.jetbrains.kmpapp.ui.theme.InLockDarkBlue
import com.jetbrains.kmpapp.ui.theme.InLockTheme
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class NfcScreen : Screen {
    @Composable
    override fun Content() {
        InLockTheme {
            val navigator = LocalNavigator.currentOrThrow
            val screenModel = getScreenModel<NfcScreenModel>()
            val uiState by screenModel.uiState.collectAsState()
            val verificationResult by screenModel.verificationResult.collectAsState()
            val scrollState = rememberScrollState()

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("NFC Scanner") },
                        navigationIcon = {
                            IconButton(onClick = { navigator.pop() }) {
                                AppIcon(
                                    icon = AppIcons.Category,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        backgroundColor = InLockBlue,
                        contentColor = Color.White
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .verticalScroll(scrollState)
                    ) {
                        AnimatedVisibility(
                            visible = uiState.error.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 24.dp),
                                elevation = 4.dp,
                                backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = uiState.error,
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colors.error
                                )
                            }
                        }

                        if (verificationResult != null) {
                            VerificationResultView(
                                verificationResult = verificationResult!!,
                                onScanAgain = { screenModel.resetScan() }
                            )
                        } else if (uiState.isScanning) {
                            NfcScanningView(
                                onCancelScan = { screenModel.stopNfcScan() }
                            )
                        } else {
                            NfcScanButton(
                                isScanning = uiState.isScanning,
                                enabled = uiState.isNfcSupported && uiState.isNfcEnabled,
                                onScanClick = {
                                    if (uiState.isScanning) {
                                        screenModel.stopNfcScan()
                                    } else {
                                        screenModel.startNfcScan()
                                    }
                                }
                            )
                        }

                        AnimatedVisibility(
                            visible = uiState.blockchainResponse.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                elevation = 4.dp,
                                backgroundColor = InLockBlue.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = uiState.blockchainResponse,
                                    modifier = Modifier.padding(16.dp),
                                    color = InLockBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun NfcScanButton(
        isScanning: Boolean,
        enabled: Boolean,
        onScanClick: () -> Unit
    ) {
        val infiniteTransition = rememberInfiniteTransition()
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                if (isScanning) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .border(4.dp, InLockBlue.copy(alpha = 0.5f), CircleShape)
                    )
                }

                Button(
                    onClick = onScanClick,
                    enabled = enabled,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = InLockBlue,
                        disabledBackgroundColor = Color.Gray.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isScanning) "SCANNING..." else "TAP TO SCAN",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        if (isScanning) {
                            Spacer(modifier = Modifier.height(8.dp))
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isScanning)
                    "Hold your phone near an NFC tag"
                else
                    "Press the button to start scanning for NFC tags",
                textAlign = TextAlign.Center,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Scan any product tag to verify its authenticity on the blockchain",
                textAlign = TextAlign.Center,
                color = Color.Gray,
                style = MaterialTheme.typography.caption
            )
        }
    }

    @Composable
    private fun NfcScanningView(onCancelScan: () -> Unit) {
        val infiniteTransition = rememberInfiniteTransition()
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .border(4.dp, InLockBlue.copy(alpha = 0.5f), CircleShape)
                )

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(InLockBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SCANNING...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Hold your phone near the NFC tag",
                textAlign = TextAlign.Center,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onCancelScan,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.error
                ),
                modifier = Modifier.fillMaxWidth(0.8f),
                shape = RoundedCornerShape(16.dp)
            ) {
                AppIcon(
                    icon = AppIcons.ErrorOutline,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cancel Scanning",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    @Composable
    private fun VerificationResultView(
        verificationResult: VerificationResult,
        onScanAgain: () -> Unit
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Card(
                backgroundColor = if (verificationResult.isAuthentic) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(if (verificationResult.isAuthentic) Color(0xFF4CAF50) else Color.Red)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppIcon(
                            icon = if (verificationResult.isAuthentic) AppIcons.VerifiedUser else AppIcons.ErrorOutline,
                            contentDescription = "Verification Status",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (verificationResult.isAuthentic) "Authentic Product" else "Not Authentic",
                        style = MaterialTheme.typography.h5,
                        fontWeight = FontWeight.Bold,
                        color = if (verificationResult.isAuthentic) Color(0xFF4CAF50) else Color.Red
                    )

                    if (verificationResult.isAuthentic) {
                        Text(
                            text = "This product is verified on the blockchain",
                            textAlign = TextAlign.Center,
                            color = Color(0xFF4CAF50)
                        )
                    } else {
                        Text(
                            text = "This product could not be verified",
                            textAlign = TextAlign.Center,
                            color = Color.Red
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (verificationResult.isAuthentic) {
                Card(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Product Details",
                            style = MaterialTheme.typography.h6,
                            fontWeight = FontWeight.Bold,
                            color = InLockBlue
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        DetailItem(
                            label = "Product Name",
                            value = verificationResult.productName
                        )

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        DetailItem(
                            label = "Category",
                            value = verificationResult.category
                        )

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        DetailItem(
                            label = "Asset ID",
                            value = verificationResult.assetId
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Ownership Details",
                            style = MaterialTheme.typography.h6,
                            fontWeight = FontWeight.Bold,
                            color = InLockBlue
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        DetailItem(
                            label = "Current Owner",
                            value = verificationResult.currentOwner
                        )

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        DetailItem(
                            label = "Manufacturer",
                            value = verificationResult.manufacturer
                        )

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        DetailItem(
                            label = "Registration Date",
                            value = verificationResult.registrationDate
                        )

                        if (verificationResult.previousOwners > 0) {
                            Divider(modifier = Modifier.padding(vertical = 12.dp))

                            DetailItem(
                                label = "Previous Owners",
                                value = verificationResult.previousOwners.toString()
                            )

                            Divider(modifier = Modifier.padding(vertical = 12.dp))

                            DetailItem(
                                label = "Last Transfer",
                                value = verificationResult.lastTransferDate
                            )
                        }
                    }
                }

                if (verificationResult.properties.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Properties",
                                style = MaterialTheme.typography.h6,
                                fontWeight = FontWeight.Bold,
                                color = InLockBlue
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            verificationResult.properties.forEach { (key, value) ->
                                if (key != "status" || value == "stolen") {
                                    DetailItem(
                                        label = key.capitalize(),
                                        value = value
                                    )

                                    if (key != verificationResult.properties.keys.last()) {
                                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                if (verificationResult.ownershipHistory.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Ownership History",
                                style = MaterialTheme.typography.h6,
                                fontWeight = FontWeight.Bold,
                                color = InLockBlue
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            verificationResult.ownershipHistory.forEachIndexed { index, record ->
                                OwnershipHistoryItem(
                                    record = record,
                                    isLast = index == verificationResult.ownershipHistory.size - 1
                                )

                                if (index < verificationResult.ownershipHistory.size - 1) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onScanAgain,
                colors = ButtonDefaults.buttonColors(backgroundColor = InLockBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                AppIcon(
                    icon = AppIcons.Nfc,
                    contentDescription = null,
                    tint = Color.White
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Scan Another Product",
                    color = Color.White
                )
            }
        }
    }

    @Composable
    private fun DetailItem(
        label: String,
        value: String
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.body2,
                color = Color.Gray
            )

            Text(
                text = value,
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.Medium
            )
        }
    }

    @Composable
    private fun OwnershipHistoryItem(
        record: OwnershipRecord,
        isLast: Boolean
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isLast) InLockBlue else Color.Gray.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                AppIcon(
                    icon = when (record.action) {
                        "register" -> AppIcons.CheckCircle
                        "transfer" -> AppIcons.Category
                        else -> AppIcons.Category
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (record.action) {
                        "register" -> "Initial Registration"
                        "transfer" -> "Transfer of Ownership"
                        else -> record.action.capitalize()
                    },
                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                    color = if (isLast) InLockDarkBlue else Color.Black
                )

                Text(
                    text = "Owner ID: ${record.user_id}",
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray
                )

                Text(
                    text = "Date: ${formatTimestamp((record.timestamp * 1000).toLong())}",
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray
                )
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        try {
            val instant = Instant.fromEpochMilliseconds(timestamp)
            val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            
            val hour = dateTime.hour.toString().padStart(2, '0')
            val minute = dateTime.minute.toString().padStart(2, '0')
            return "${dateTime.date} at $hour:$minute"
        } catch (e: Exception) {
            return "Unknown Date and Time"
        }
    }

    private fun String.capitalize(): String {
        return if (this.isNotEmpty()) {
            this[0].uppercase() + this.substring(1)
        } else {
            this
        }
    }
}