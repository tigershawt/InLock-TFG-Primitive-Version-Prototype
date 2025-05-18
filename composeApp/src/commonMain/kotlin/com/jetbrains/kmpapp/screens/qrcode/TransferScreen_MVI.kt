package com.jetbrains.kmpapp.screens.qrcode

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.launch

class TransferScreen_MVI(private val recipientUserId: String) : Screen {
    
    @Composable
    override fun Content() {
        InLockTheme {
            val navigator = LocalNavigator.currentOrThrow
            val screenModel = getScreenModel<TransferScreenModelMvi>()
            val state by screenModel.state.collectAsState()
            val scrollState = rememberScrollState()
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                screenModel.processIntent(TransferIntent.SetRecipientId(recipientUserId))
            }

            LaunchedEffect(Unit) {
                screenModel.sideEffects.collect { effect ->
                    when (effect) {
                        is TransferSideEffect.ShowNfcNotSupportedError -> {
                        }
                        is TransferSideEffect.ShowNfcDisabledError -> {

                        }
                        is TransferSideEffect.ShowToast -> {
                        }
                        is TransferSideEffect.NavigateBack -> {
                            navigator.pop()
                        }
                    }
                }
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
                            recipientName = state.recipientName
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        when {
                            state.transferCompleted -> {
                                TransferCompletedContent(
                                    onDone = { navigator.pop() }
                                )
                            }

                            state.isTransferring -> {
                                TransferProgressContent(
                                    progress = state.transferProgress
                                )
                            }

                            state.isRfidVerified && state.selectedProduct != null -> {
                                VerifiedProductContent(
                                    product = state.selectedProduct!!,
                                    recipientName = state.recipientName.ifEmpty { recipientUserId },
                                    expiryTime = state.transferPermissionExpiryTime,
                                    hasPhysicalVerification = state.hasPhysicalVerification,
                                    physicalTimeRemaining = state.physicalVerificationTimeRemaining,
                                    onConfirm = {
                                        screenModel.processIntent(TransferIntent.ConfirmTransfer)
                                    },
                                    onCancel = {
                                        screenModel.processIntent(TransferIntent.ClearSelectedProduct)
                                    }
                                )
                            }
                            
                            state.showConfirmationCodeUI && state.selectedProduct != null -> {
                                ConfirmationCodeContent(
                                    product = state.selectedProduct!!,
                                    confirmationCode = state.confirmationCode,
                                    userEnteredCode = state.userEnteredCode,
                                    hasPhysicalVerification = state.hasPhysicalVerification,
                                    physicalTimeRemaining = state.physicalVerificationTimeRemaining,
                                    onCodeChanged = { code -> 
                                        screenModel.processIntent(TransferIntent.ProcessConfirmationCode(code))
                                    },
                                    onSubmit = {
                                    },
                                    onCancel = {
                                        screenModel.processIntent(TransferIntent.StopRfidScan)
                                        screenModel.processIntent(TransferIntent.ClearSelectedProduct)
                                    }
                                )
                            }

                            state.selectedProduct != null -> {
                                SelectedProductContent(
                                    product = state.selectedProduct!!,
                                    recipientName = state.recipientName.ifEmpty { recipientUserId },
                                    isRfidScanning = state.isRfidScanning,
                                    hasPhysicalVerification = state.hasPhysicalVerification,
                                    physicalTimeRemaining = state.physicalVerificationTimeRemaining,
                                    onStartRfidScan = {
                                        screenModel.processIntent(TransferIntent.StartRfidScan)
                                    },
                                    onCancelRfidScan = {
                                        screenModel.processIntent(TransferIntent.StopRfidScan)
                                    },
                                    onCancel = {
                                        screenModel.processIntent(TransferIntent.ClearSelectedProduct)
                                    }
                                )
                            }

                            state.isRfidScanning -> {
                                if (state.error.isNotEmpty()) {
                                    Column {
                                        ScanErrorContent(
                                            error = state.error,
                                            onRetry = {
                                                screenModel.processIntent(TransferIntent.ResetError)
                                                screenModel.processIntent(TransferIntent.StartRfidScan)
                                            }
                                        )
                                        
                                        RfidScanningContent(
                                            onCancel = {
                                                screenModel.processIntent(TransferIntent.StopRfidScan)
                                            }
                                        )
                                    }
                                } else {
                                    RfidScanningContent(
                                        onCancel = {
                                            screenModel.processIntent(TransferIntent.StopRfidScan)
                                        }
                                    )
                                }
                            }

                            state.isLoading -> {
                                com.jetbrains.kmpapp.ui.components.LoadingStateContent(
                                    message = "Loading assets...",
                                    modifier = Modifier.height(300.dp)
                                )
                            }

                            state.userProducts.isEmpty() -> {
                                com.jetbrains.kmpapp.ui.components.EmptyStateContent(
                                    title = "No Assets Available",
                                    message = "You don't have any assets to transfer",
                                    icon = AppIcons.Inventory,
                                    modifier = Modifier.height(300.dp)
                                )
                            }

                            else -> {
                                SelectProductContent(
                                    products = state.userProducts,
                                    onProductSelected = { productId ->
                                        screenModel.processIntent(TransferIntent.SelectProduct(productId))
                                    },
                                    onScanRfid = {
                                        screenModel.processIntent(TransferIntent.StartRfidScan)
                                    }
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = state.error.isNotEmpty(),
                        enter = androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        Snackbar(
                            action = {
                                TextButton(
                                    onClick = { screenModel.processIntent(TransferIntent.ResetError) }
                                ) {
                                    Text("Dismiss", color = Color.White)
                                }
                            },
                            backgroundColor = MaterialTheme.colors.error
                        ) {
                            Text(state.error, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    
    @Composable
    private fun SelectedProductContent(
        product: Product,
        recipientName: String,
        isRfidScanning: Boolean,
        hasPhysicalVerification: Boolean,
        physicalTimeRemaining: String,
        onStartRfidScan: () -> Unit,
        onCancelRfidScan: () -> Unit,
        onCancel: () -> Unit
    ) {
        com.jetbrains.kmpapp.screens.qrcode.SelectedProductContent(
            product = product,
            recipientName = recipientName,
            isRfidScanning = isRfidScanning,
            hasPhysicalVerification = hasPhysicalVerification,
            physicalTimeRemaining = physicalTimeRemaining,
            onStartRfidScan = onStartRfidScan,
            onCancelRfidScan = onCancelRfidScan,
            onCancel = onCancel
        )
    }
    
    @Composable
    private fun VerifiedProductContent(
        product: Product,
        recipientName: String,
        expiryTime: Long,
        hasPhysicalVerification: Boolean,
        physicalTimeRemaining: String,
        onConfirm: () -> Unit,
        onCancel: () -> Unit
    ) {
        com.jetbrains.kmpapp.screens.qrcode.VerifiedProductContent(
            product = product,
            recipientName = recipientName,
            expiryTime = expiryTime,
            hasPhysicalVerification = hasPhysicalVerification,
            physicalTimeRemaining = physicalTimeRemaining,
            onConfirm = onConfirm,
            onCancel = onCancel
        )
    }
    
    @Composable
    private fun ConfirmationCodeContent(
        product: Product,
        confirmationCode: String,
        userEnteredCode: String,
        hasPhysicalVerification: Boolean,
        physicalTimeRemaining: String,
        onCodeChanged: (String) -> Unit,
        onSubmit: () -> Unit,
        onCancel: () -> Unit
    ) {
        com.jetbrains.kmpapp.screens.qrcode.ConfirmationCodeContent(
            product = product,
            confirmationCode = confirmationCode,
            userEnteredCode = userEnteredCode,
            hasPhysicalVerification = hasPhysicalVerification,
            physicalTimeRemaining = physicalTimeRemaining,
            onCodeChanged = onCodeChanged,
            onSubmit = onSubmit,
            onCancel = onCancel
        )
    }
}

@Composable
private fun ScanErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    ContentCard(
        cornerRadius = 8,
        elevation = 2,
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.error.copy(alpha = 0.1f))
                .padding(16.dp)
        ) {
            Text(
                "Error Scanning",
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colors.error
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                error,
                color = MaterialTheme.colors.error
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            ActionButtonRow(
                primaryAction = ButtonAction(
                    text = "Try Again",
                    onClick = onRetry
                )
            )
        }
    }
}

@Composable
private fun TransferProgressContent(progress: Float) {
    val animatedProgress by animateFloatAsState(targetValue = progress)

    ContentCard(
        cornerRadius = 16,
        elevation = 4
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Transfer in Progress",
                style = MaterialTheme.typography.h6,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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
    ContentCard(
        cornerRadius = 16,
        elevation = 4
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(120.dp)
                    .background(Color(0xFF4CAF50), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AppIcon(
                    icon = AppIcons.CheckCircle,
                    contentDescription = "Success",
                    tint = Color.White,
                    modifier = Modifier
                        .width(80.dp)
                        .height(80.dp)
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

            ActionButtonRow(
                primaryAction = ButtonAction(
                    text = "Done",
                    onClick = onDone
                )
            )
        }
    }
}