package com.jetbrains.kmpapp.screens.assets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.jetbrains.kmpapp.data.Product
import com.jetbrains.kmpapp.screens.nfc.NfcScreenCommon
import com.jetbrains.kmpapp.ui.components.LoadingStateContent
import com.jetbrains.kmpapp.ui.icons.AppIcon
import com.jetbrains.kmpapp.ui.icons.AppIcons
import com.jetbrains.kmpapp.ui.theme.InLockBlue
import com.jetbrains.kmpapp.ui.theme.InLockTheme
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

class AssetsScreen : Screen {
    @Composable
    override fun Content() {
        InLockTheme {
            val navigator = LocalNavigator.currentOrThrow
            val screenModel = getScreenModel<AssetsScreenModel>()
            val uiState by screenModel.uiState.collectAsState()

            var showAssetRecoveryDialog by remember { mutableStateOf(false) }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("My Assets") },
                        backgroundColor = InLockBlue,
                        contentColor = Color.White,
                        actions = {
                            
                            IconButton(onClick = { screenModel.loadUserAssets() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
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
                    when {
                        uiState.isLoading -> {
                            LoadingStateContent()
                        }
                        uiState.isFetchingFromBlockchain -> {
                            BlockchainFetchingContent(
                                errorMessage = uiState.error
                            )
                        }
                        uiState.error.isNotEmpty() && uiState.assets.isEmpty() -> {
                            ErrorStateContent(
                                errorMessage = uiState.error,
                                onRetry = { screenModel.retryAssetLoading() },
                                onScanAsset = { navigator.push(NfcScreenCommon()) }
                            )
                        }
                        uiState.assets.isEmpty() -> {
                            EmptyStateContent(
                                onScanAsset = { navigator.push(NfcScreenCommon()) }
                            )
                        }
                        else -> {
                            
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                items(uiState.assets) { asset ->
                                    AssetItem(
                                        asset = asset,
                                        onAssetClick = {
                                            navigator.push(EnhancedAssetDetailScreen(asset.id))
                                        }
                                    )
                                }
                            }
                        }
                    }

                    
                    if (uiState.error.isNotEmpty() && uiState.assets.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                        ) {
                            Snackbar(
                                action = {
                                    TextButton(onClick = { screenModel.loadUserAssets() }) {
                                        Text("Retry", color = Color.White)
                                    }
                                }
                            ) {
                                Text(uiState.error)
                            }
                        }
                    }
                }
            }

        }
    }

    @Composable
    private fun LoadingStateContent() {
        com.jetbrains.kmpapp.ui.components.LoadingStateContent(
            message = "Loading your assets..."
        )
    }

    @Composable
    private fun BlockchainFetchingContent(errorMessage: String) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppIcon(
                icon = AppIcons.Nfc,
                contentDescription = null,
                tint = InLockBlue.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = InLockBlue
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Checking blockchain for your assets...",
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.Medium
            )

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    errorMessage,
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }



    @Composable
    private fun ErrorStateContent(
        errorMessage: String,
        onRetry: () -> Unit,
        onScanAsset: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            AppIcon(
                icon = AppIcons.ErrorOutline,
                contentDescription = null,
                tint = Color.Red.copy(alpha = 0.7f),
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Error Loading Assets",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                errorMessage,
                style = MaterialTheme.typography.body1,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(backgroundColor = InLockBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Retry", color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onScanAsset,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(
                        icon = AppIcons.Nfc,
                        contentDescription = null,
                        tint = InLockBlue
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text("Scan NFC Tag")
                }
            }

        }
    }

    @Composable
    private fun EmptyStateContent(onScanAsset: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppIcon(
                icon = AppIcons.Inventory,
                contentDescription = null,
                tint = Color.Gray.copy(alpha = 0.5f),
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "No Assets Found",
                style = MaterialTheme.typography.h6,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "You don't have any assets yet. Create or scan assets to get started.",
                textAlign = TextAlign.Center,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onScanAsset,
                colors = ButtonDefaults.buttonColors(backgroundColor = InLockBlue),
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(
                        icon = AppIcons.Nfc,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        "Scan NFC Tag",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    @Composable
    private fun AssetItem(asset: Product, onAssetClick: () -> Unit) {
        val screenModel = getScreenModel<AssetsScreenModel>()
        val uiState by screenModel.uiState.collectAsState()


        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable(onClick = onAssetClick),
            elevation = 4.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        if (asset.imageUrl.isNotEmpty()) {
                            KamelImage(
                                resource = asyncPainterResource(asset.imageUrl),
                                contentDescription = asset.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                onLoading = {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        color = InLockBlue
                                    )
                                },
                                onFailure = {
                                    AppIcon(
                                        icon = AppIcons.BrokenImage,
                                        contentDescription = "No image",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            )
                        } else {
                            AppIcon(
                                icon = AppIcons.Category,
                                contentDescription = "No image",
                                tint = Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = asset.name,
                            style = MaterialTheme.typography.h6,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = asset.category.ifEmpty { "Unknown Category" },
                            style = MaterialTheme.typography.body2,
                            color = Color.Gray
                        )

                        if (asset.isRegisteredOnBlockchain) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                AppIcon(
                                    icon = AppIcons.VerifiedUser,
                                    contentDescription = "Blockchain Verified",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Text(
                                    text = "Blockchain Verified",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }

                    }

                    AppIcon(
                        icon = AppIcons.Category,
                        contentDescription = "View details",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (asset.isRegisteredOnBlockchain) {
                    Divider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        OutlinedButton(
                            onClick = onAssetClick,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = InLockBlue
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppIcon(
                                    icon = AppIcons.Category,
                                    contentDescription = null,
                                    tint = InLockBlue,
                                    modifier = Modifier.size(16.dp)
                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Text(
                                    text = "View Details",
                                    style = MaterialTheme.typography.button
                                )
                            }
                        }
                    }
                }
            }
        }

    }
}