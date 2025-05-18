package com.jetbrains.kmpapp.screens.qrcode

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.jetbrains.kmpapp.data.Product
import com.jetbrains.kmpapp.ui.components.ActionButtonRow
import com.jetbrains.kmpapp.ui.components.BlockchainBadge
import com.jetbrains.kmpapp.ui.components.ButtonAction
import com.jetbrains.kmpapp.ui.components.ContentCard
import com.jetbrains.kmpapp.ui.components.EmptyStateContent
import com.jetbrains.kmpapp.ui.components.LoadingStateContent
import com.jetbrains.kmpapp.ui.components.ProductImageContainer
import com.jetbrains.kmpapp.ui.components.StatusBadgeStyle
import com.jetbrains.kmpapp.ui.icons.AppIcon
import com.jetbrains.kmpapp.ui.icons.AppIcons
import com.jetbrains.kmpapp.ui.theme.InLockBlue
import com.jetbrains.kmpapp.ui.theme.InLockTheme
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.launch

class TransferScreen(private val recipientUserId: String) : Screen {
    @Composable
    override fun Content() {
        InLockTheme {
            val navigator = LocalNavigator.currentOrThrow
            val screenModel = getScreenModel<TransferScreenModel>()
            val uiState by screenModel.uiState.collectAsState()
            val scrollState = rememberScrollState()
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                screenModel.setRecipientId(recipientUserId)
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Transfer Asset") },
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
                        .padding(padding)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        
                        RecipientInfoCard(
                            recipientId = recipientUserId,
                            recipientName = uiState.recipientName
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        
                        when {
                            uiState.transferCompleted -> {
                                TransferCompletedContent(
                                    onDone = { navigator.pop() }
                                )
                            }

                            uiState.isTransferring -> {
                                TransferProgressContent(
                                    progress = uiState.transferProgress
                                )
                            }

                            uiState.isRfidVerified && uiState.selectedProduct != null -> {
                                VerifiedProductContent(
                                    product = uiState.selectedProduct!!,
                                    recipientName = uiState.recipientName.ifEmpty { recipientUserId },
                                    expiryTime = uiState.transferPermissionExpiryTime,
                                    onConfirm = {
                                        screenModel.confirmTransfer()
                                    },
                                    onCancel = {
                                        screenModel.clearSelectedProduct()
                                    }
                                )
                            }
                            
                            uiState.showConfirmationCodeUI && uiState.selectedProduct != null -> {
                                com.jetbrains.kmpapp.screens.qrcode.ConfirmationCodeContent(
                                    product = uiState.selectedProduct!!,
                                    confirmationCode = uiState.confirmationCode,
                                    userEnteredCode = uiState.userEnteredCode,
                                    onCodeChanged = { code -> 
                                        screenModel.processConfirmationCode(code)
                                    },
                                    onSubmit = {
                                    },
                                    onCancel = {
                                        screenModel.stopRfidScan()
                                        screenModel.clearSelectedProduct()
                                    }
                                )
                            }

                            uiState.selectedProduct != null -> {
                                SelectedProductContent(
                                    product = uiState.selectedProduct!!,
                                    recipientName = uiState.recipientName.ifEmpty { recipientUserId },
                                    isRfidScanning = uiState.isRfidScanning,
                                    onStartRfidScan = {
                                        screenModel.startRfidScan()
                                    },
                                    onCancelRfidScan = {
                                        screenModel.stopRfidScan()
                                    },
                                    onCancel = {
                                        screenModel.clearSelectedProduct()
                                    }
                                )
                            }

                            uiState.isRfidScanning -> {
                                
                                if (uiState.error.isNotEmpty()) {
                                    Column {
                                        
                                        Card(
                                            backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.1f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 16.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Text(
                                                    "Error Scanning",
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colors.error
                                                )
                                                
                                                Spacer(modifier = Modifier.height(4.dp))
                                                
                                                Text(
                                                    uiState.error,
                                                    color = MaterialTheme.colors.error
                                                )
                                                
                                                Spacer(modifier = Modifier.height(8.dp))
                                                
                                                Button(
                                                    onClick = {
                                                        screenModel.resetError()
                                                        screenModel.startRfidScan()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        backgroundColor = InLockBlue
                                                    )
                                                ) {
                                                    Text("Try Again", color = Color.White)
                                                }
                                            }
                                        }
                                        
                                        
                                        RfidScanningContent(
                                            onCancel = {
                                                screenModel.stopRfidScan()
                                            }
                                        )
                                    }
                                } else {
                                    
                                    RfidScanningContent(
                                        onCancel = {
                                            screenModel.stopRfidScan()
                                        }
                                    )
                                }
                            }

                            uiState.isLoading -> {
                                LoadingContent()
                            }

                            uiState.userProducts.isEmpty() -> {
                                NoProductsContent()
                            }

                            else -> {
                                SelectProductContent(
                                    products = uiState.userProducts,
                                    onProductSelected = { productId ->
                                        screenModel.selectProduct(productId)
                                    },
                                    onScanRfid = {
                                        screenModel.startRfidScan()
                                    }
                                )
                            }
                        }
                    }

                    
                    AnimatedVisibility(
                        visible = uiState.error.isNotEmpty(),
                        enter = androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        Snackbar(
                            action = {
                                TextButton(
                                    onClick = { screenModel.resetError() }
                                ) {
                                    Text("Dismiss", color = Color.White)
                                }
                            },
                            backgroundColor = MaterialTheme.colors.error
                        ) {
                            Text(uiState.error, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RecipientInfoCard(
        recipientId: String,
        recipientName: String
    ) {
        ContentCard(
            title = "Recipient",
            cornerRadius = 16,
            elevation = 4
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(InLockBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    AppIcon(
                        icon = AppIcons.AdminPanelSettings,
                        contentDescription = "User",
                        tint = InLockBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (recipientName.isNotEmpty()) {
                        Text(
                            recipientName,
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "ID: $recipientId",
                            style = MaterialTheme.typography.caption,
                            color = Color.Gray
                        )
                    } else {
                        Text(
                            "User ID: $recipientId",
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Loading recipient information...",
                            style = MaterialTheme.typography.caption,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun LoadingContent() {
        com.jetbrains.kmpapp.ui.components.LoadingStateContent(
            message = "Loading assets...",
            modifier = Modifier.height(300.dp)
        )
    }

    @Composable
    private fun NoProductsContent() {
        com.jetbrains.kmpapp.ui.components.EmptyStateContent(
            title = "No Assets Available",
            message = "You don't have any assets to transfer",
            icon = AppIcons.Inventory,
            modifier = Modifier.height(300.dp)
        )
    }

    @Composable
    private fun SelectProductContent(
        products: List<Product>,
        onProductSelected: (String) -> Unit,
        onScanRfid: () -> Unit
    ) {
        Card(
            elevation = 4.dp,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "Select Asset to Transfer",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = InLockBlue
                )

                Spacer(modifier = Modifier.height(16.dp))

                
                OutlinedButton(
                    onClick = {
                        onScanRfid()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        AppIcon(
                            icon = AppIcons.Nfc,
                            contentDescription = null,
                            tint = InLockBlue
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text("Scan Physical Asset with NFC")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Or select from your assets:",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                
                products.forEach { product ->
                    ProductListItem(
                        product = product,
                        onClick = { onProductSelected(product.id) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    @Composable
    private fun ProductListItem(
        product: Product,
        onClick: () -> Unit
    ) {
        ContentCard(
            cornerRadius = 8,
            elevation = 2,
            onClick = onClick
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProductImageContainer(
                    product = product,
                    modifier = Modifier.size(60.dp),
                    cornerRadius = 8
                )

                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        product.name,
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        product.category,
                        style = MaterialTheme.typography.caption,
                        color = Color.Gray
                    )

                    if (product.isRegisteredOnBlockchain) {
                        BlockchainBadge(
                            registered = true,
                            style = StatusBadgeStyle.SUBTLE,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun RfidScanningContent(
        onCancel: () -> Unit
    ) {
        Card(
            elevation = 4.dp,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Scanning NFC Tag",  
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = InLockBlue
                )

                Spacer(modifier = Modifier.height(24.dp))
                
                
                val infiniteTransition = rememberInfiniteTransition()
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                
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

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Hold your phone near the NFC tag on the physical asset",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body1
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.Red
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Cancel Scan",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun SelectedProductContent(
        product: Product,
        recipientName: String,
        isRfidScanning: Boolean,
        onStartRfidScan: () -> Unit,
        onCancelRfidScan: () -> Unit,
        onCancel: () -> Unit
    ) {
        val uiState by getScreenModel<TransferScreenModel>().uiState.collectAsState()
        Card(
            elevation = 4.dp,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Verify Asset Ownership",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = InLockBlue
                )

                Spacer(modifier = Modifier.height(16.dp))

                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    if (product.imageUrl.isNotEmpty()) {
                        KamelImage(
                            resource = asyncPainterResource(product.imageUrl),
                            contentDescription = product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            onLoading = {
                                CircularProgressIndicator(strokeWidth = 2.dp)
                            },
                            onFailure = {
                                AppIcon(
                                    icon = AppIcons.BrokenImage,
                                    contentDescription = "No image",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        )
                    } else {
                        AppIcon(
                            icon = AppIcons.Inventory,
                            contentDescription = "No image",
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    
                    if (product.isRegisteredOnBlockchain) {
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .align(Alignment.TopEnd)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50)),
                            contentAlignment = Alignment.Center
                        ) {
                            AppIcon(
                                icon = AppIcons.VerifiedUser,
                                contentDescription = "Blockchain Verified",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    product.name,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    product.category,
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    product.description,
                    style = MaterialTheme.typography.body2
                )

                Spacer(modifier = Modifier.height(24.dp))


                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF9FBE7))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            "Code Verification Required",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF827717)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            "Please scan the physical asset's RFID tag to verify your ownership before transferring.",
                            color = Color(0xFF827717)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (uiState.hasPhysicalVerification) Color(0xFFE8F5E9) else Color(0xFFFFF9C4)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(
                            icon = if (uiState.hasPhysicalVerification) AppIcons.CheckCircle else AppIcons.Warning,
                            contentDescription = null,
                            tint = if (uiState.hasPhysicalVerification) Color(0xFF4CAF50) else Color(0xFFF57F17),
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                if (uiState.hasPhysicalVerification) "Physical Verification Complete" else "Physical Verification Required",
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.hasPhysicalVerification) Color(0xFF4CAF50) else Color(0xFFF57F17)
                            )
                            
                            if (uiState.hasPhysicalVerification) {
                                Text(
                                    "Asset physically verified. ${uiState.physicalVerificationTimeRemaining}",
                                    color = Color(0xFF4CAF50)
                                )
                            } else {
                                Text(
                                    "Please use the NFC Verification screen to physically verify this asset before transferring.",
                                    color = Color(0xFFF57F17)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                
                if (isRfidScanning) {
                    Button(
                        onClick = onCancelRfidScan,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Red
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Cancel Scanning",
                            color = Color.White,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    ActionButtonRow(
                        primaryAction = ButtonAction(
                            text = "Verify Intent",
                            onClick = onStartRfidScan, // This will now show the confirmation code UI
                            icon = AppIcons.VerifiedUser
                        ),
                        secondaryAction = ButtonAction(
                            text = "Cancel",
                            onClick = onCancel,
                            isPrimary = false
                        )
                    )
                }
            }
        }
    }

    @Composable
    private fun VerifiedProductContent(
        product: Product,
        recipientName: String,
        expiryTime: Long,
        onConfirm: () -> Unit,
        onCancel: () -> Unit
    ) {
        val uiState by getScreenModel<TransferScreenModel>().uiState.collectAsState()
        Card(
            elevation = 4.dp,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Confirm Transfer",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = InLockBlue
                )

                Spacer(modifier = Modifier.height(16.dp))

                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    if (product.imageUrl.isNotEmpty()) {
                        KamelImage(
                            resource = asyncPainterResource(product.imageUrl),
                            contentDescription = product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            onLoading = {
                                CircularProgressIndicator(strokeWidth = 2.dp)
                            },
                            onFailure = {
                                AppIcon(
                                    icon = AppIcons.BrokenImage,
                                    contentDescription = "No image",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        )
                    } else {
                        AppIcon(
                            icon = AppIcons.Inventory,
                            contentDescription = "No image",
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    if (product.isRegisteredOnBlockchain) {
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .align(Alignment.TopEnd)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50)),
                            contentAlignment = Alignment.Center
                        ) {
                            AppIcon(
                                icon = AppIcons.VerifiedUser,
                                contentDescription = "Blockchain Verified",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    product.name,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    product.category,
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(24.dp))

                
                Text(
                    "Transfer Details",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    color = InLockBlue
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row {
                    Text(
                        "From:",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(80.dp)
                    )

                    Text("You")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    Text(
                        "To:",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(80.dp)
                    )

                    Text(recipientName)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    Text(
                        "Asset ID:",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(80.dp)
                    )

                    Text(product.id)
                }

                Spacer(modifier = Modifier.height(24.dp))

                
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE8F5E9))
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(
                            icon = AppIcons.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                "Code Verification Complete",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )

                            val validUntil = if (expiryTime > 0) {
                                val expiryTimeFormatted = formatExpiryTime(expiryTime)
                                " Valid until $expiryTimeFormatted"
                            } else ""
                            
                            Text(
                                "Transfer permission confirmed.${validUntil} You can now complete the transfer.",
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (uiState.hasPhysicalVerification) Color(0xFFE8F5E9) else Color(0xFFFFF9C4)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(
                            icon = if (uiState.hasPhysicalVerification) AppIcons.CheckCircle else AppIcons.Warning,
                            contentDescription = null,
                            tint = if (uiState.hasPhysicalVerification) Color(0xFF4CAF50) else Color(0xFFF57F17),
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                if (uiState.hasPhysicalVerification) "Physical Verification Complete" else "Physical Verification Required",
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.hasPhysicalVerification) Color(0xFF4CAF50) else Color(0xFFF57F17)
                            )
                            
                            if (uiState.hasPhysicalVerification) {
                                Text(
                                    "Asset physically verified. ${uiState.physicalVerificationTimeRemaining}",
                                    color = Color(0xFF4CAF50)
                                )
                            } else {
                                Text(
                                    "This asset needs to be physically verified using the NFC Verification screen before transfer.",
                                    color = Color(0xFFF57F17)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = InLockBlue
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Complete Transfer",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun TransferProgressContent(progress: Float) {
        val animatedProgress by animateFloatAsState(targetValue = progress)

        Card(
            elevation = 4.dp,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Transfer in Progress",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = InLockBlue
                )

                Spacer(modifier = Modifier.height(32.dp))

                CircularProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier.size(120.dp),
                    color = InLockBlue,
                    strokeWidth = 8.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold,
                    color = InLockBlue
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    "Please wait while we process the ownership transfer on the blockchain...",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body1
                )
            }
        }
    }

    @Composable
    private fun TransferCompletedContent(onDone: () -> Unit) {
        Card(
            elevation = 4.dp,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50)),
                    contentAlignment = Alignment.Center
                ) {
                    AppIcon(
                        icon = AppIcons.CheckCircle,
                        contentDescription = "Success",
                        tint = Color.White,
                        modifier = Modifier.size(80.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Transfer Complete!",
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "The asset has been successfully transferred to the new owner. " +
                            "The transaction has been recorded on the blockchain.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body1
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onDone,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = InLockBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Done",
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
    
    private fun formatExpiryTime(timestamp: Long): String {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}:${localDateTime.second.toString().padStart(2, '0')}"
    }
}