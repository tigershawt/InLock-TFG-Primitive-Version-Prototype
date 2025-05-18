package com.jetbrains.kmpapp.screens.assets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.jetbrains.kmpapp.screens.nfc.NfcScreenCommon
import com.jetbrains.kmpapp.ui.components.EnhancedAssetCard
import com.jetbrains.kmpapp.ui.components.EnhancedCard
import com.jetbrains.kmpapp.ui.components.EnhancedGradientCard
import com.jetbrains.kmpapp.ui.components.EnhancedCollapsibleHeader
import com.jetbrains.kmpapp.ui.icons.AppIcon
import com.jetbrains.kmpapp.ui.icons.AppIcons
import com.jetbrains.kmpapp.ui.theme.*

class EnhancedAssetsScreen : Screen {
    @Composable
    override fun Content() {
        InLockTheme {
            val navigator = LocalNavigator.currentOrThrow
            val screenModel = getScreenModel<AssetsScreenModel>()
            val uiState by screenModel.uiState.collectAsState()
            val scrollState = rememberScrollState()
            
            
            LaunchedEffect(Unit) {
                EnhancedMainScreen.currentDestination.value = 
                    EnhancedBottomNavDestination.ASSETS
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("My Assets") },
                        backgroundColor = InLockBlue,
                        contentColor = Color.White,
                        actions = {
                            
                            IconButton(onClick = { screenModel.loadUserAssets() }) {
                                AppIcon(
                                    icon = AppIcons.Refresh,
                                    contentDescription = "Refresh",
                                    tint = Color.White
                                )
                            }
                        }
                    )
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(InLockBackground),
                        
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        uiState.isLoading -> {
                            EnhancedLoadingState()
                        }
                        uiState.isFetchingFromBlockchain -> {
                            EnhancedBlockchainFetchingState(
                                errorMessage = uiState.error
                            )
                        }
                        uiState.error.isNotEmpty() && uiState.assets.isEmpty() -> {
                            EnhancedErrorState(
                                errorMessage = uiState.error,
                                onRetry = { screenModel.retryAssetLoading() },
                                onScanAsset = { navigator.push(NfcScreenCommon()) },
                                onCheckBlockchain = { screenModel.checkBlockchainAssets() }
                            )
                        }
                        uiState.assets.isEmpty() -> {
                            EnhancedEmptyState(
                                onScanAsset = { navigator.push(NfcScreenCommon()) }
                            )
                        }
                        else -> {
                            
                            EnhancedAssetsList(
                                assets = uiState.assets,
                                onAssetClick = { asset ->
                                    navigator.push(EnhancedAssetDetailScreen(asset.id))
                                }
                            )
                        }
                    }

                    
                    AnimatedVisibility(
                        visible = uiState.error.isNotEmpty() && uiState.assets.isNotEmpty(),
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it }
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                        ) {
                            Snackbar(
                                shape = RoundedCornerShape(size = InLockSmallCornerRadius),
                                backgroundColor = Color(0xFF323232),
                                action = {
                                    TextButton(onClick = { screenModel.loadUserAssets() }) {
                                        Text("Retry", color = InLockAccent)
                                    }
                                }
                            ) {
                                Text(uiState.error, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun EnhancedLoadingState() {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            androidx.compose.material.CircularProgressIndicator(
                color = InLockPrimary,
                strokeWidth = 4.dp,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Loading your assets...",
                style = MaterialTheme.typography.h6,
                color = InLockTextPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Please wait while we fetch your latest asset data",
                style = MaterialTheme.typography.body2,
                color = InLockTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }

    @Composable
    private fun EnhancedBlockchainFetchingState(errorMessage: String) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppIcon(
                icon = AppIcons.Blockchain,
                contentDescription = null,
                tint = InLockPrimary.copy(alpha = 0.7f),
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(size = 4.dp)),
                color = InLockPrimary,
                backgroundColor = InLockPrimary.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Verifying Blockchain Assets",
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold,
                color = InLockTextPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "We're checking the blockchain for your registered assets. This may take a moment...",
                style = MaterialTheme.typography.body2,
                color = InLockTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                EnhancedCard(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(8.dp),
                    backgroundColor = InLockError.copy(alpha = 0.1f),
                    cornerRadius = 8.dp,
                    elevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(
                            icon = AppIcons.ErrorOutline,
                            contentDescription = null,
                            tint = InLockError,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp)
                        )
                        
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.caption,
                            color = InLockError
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun EnhancedErrorState(
        errorMessage: String,
        onRetry: () -> Unit,
        onScanAsset: () -> Unit,
        onCheckBlockchain: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppIcon(
                icon = AppIcons.ErrorOutline,
                contentDescription = null,
                tint = InLockError.copy(alpha = 0.7f),
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Unable to Load Assets",
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold,
                color = InLockTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                errorMessage,
                style = MaterialTheme.typography.body1,
                color = InLockTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = InLockPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(size = InLockButtonCornerRadius),
                elevation = ButtonDefaults.elevation(
                    defaultElevation = InLockButtonElevation
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Retry", modifier = Modifier.padding(vertical = 8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCheckBlockchain,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = InLockSecondary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(size = InLockButtonCornerRadius),
                elevation = ButtonDefaults.elevation(
                    defaultElevation = InLockButtonElevation
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Check Blockchain", modifier = Modifier.padding(vertical = 8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onScanAsset,
                shape = RoundedCornerShape(size = InLockButtonCornerRadius),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    AppIcon(
                        icon = AppIcons.Nfc,
                        contentDescription = null,
                        tint = InLockPrimary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text("Scan New Asset")
                }
            }
        }
    }

    @Composable
    private fun EnhancedEmptyState(onScanAsset: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            AppIcon(
                icon = AppIcons.Inventory,
                contentDescription = null,
                tint = InLockPrimary.copy(alpha = 0.3f),
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "No Assets Yet",
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = InLockTextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "You don't have any assets registered yet. Scan an NFC tag to add your first asset.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.body1,
                color = InLockTextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            
            EnhancedGradientCard(
                modifier = Modifier.fillMaxWidth(0.7f),
                gradientStart = InLockPrimary,
                gradientEnd = InLockSecondary,
                cornerRadius = 16.dp,
                elevation = InLockButtonElevation,
                onClick = onScanAsset
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    AppIcon(
                        icon = AppIcons.Nfc,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        "Scan NFC Tag",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.button
                    )
                }
            }
        }
    }

    @Composable
    private fun EnhancedAssetsList(
        assets: List<com.jetbrains.kmpapp.data.Product>,
        onAssetClick: (com.jetbrains.kmpapp.data.Product) -> Unit
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            
            item {
                Text(
                    "Your Assets",
                    style = MaterialTheme.typography.h4,
                    fontWeight = FontWeight.Bold,
                    color = InLockTextPrimary,
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 4.dp)
                )
            }
            
            
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(size = 12.dp))
                            .background(InLockPrimary.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${assets.size} ${if (assets.size == 1) "Asset" else "Assets"}",
                            style = MaterialTheme.typography.caption,
                            fontWeight = FontWeight.Medium,
                            color = InLockPrimary
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    
                    val verifiedCount = assets.count { it.isRegisteredOnBlockchain }
                    if (verifiedCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(size = 12.dp))
                                .background(InLockSuccess.copy(alpha = 0.1f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            AppIcon(
                                icon = AppIcons.VerifiedUser,
                                contentDescription = null,
                                tint = InLockSuccess,
                                modifier = Modifier.size(14.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            Text(
                                text = "$verifiedCount Verified",
                                style = MaterialTheme.typography.caption,
                                fontWeight = FontWeight.Medium,
                                color = InLockSuccess
                            )
                        }
                    }
                }
            }

            
            items(assets) { asset ->
                EnhancedAssetCard(
                    asset = asset,
                    onClick = { onAssetClick(asset) }
                )
            }
            
            
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}