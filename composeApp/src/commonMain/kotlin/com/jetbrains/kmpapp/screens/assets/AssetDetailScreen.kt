package com.jetbrains.kmpapp.screens.assets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class AssetDetailScreen(private val assetId: String) : Screen {
    @Composable
    override fun Content() {
        InLockTheme {
            val navigator = LocalNavigator.currentOrThrow
            val screenModel = getScreenModel<AssetDetailScreenModel>()
            val uiState by screenModel.uiState.collectAsState()
            val scrollState = rememberScrollState()

            LaunchedEffect(assetId) {
                screenModel.loadAssetDetails(assetId)
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Asset Details") },
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
                    when {
                        uiState.isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = InLockBlue)
                            }
                        }
                        uiState.asset == null -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                AppIcon(
                                    icon = AppIcons.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(64.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Asset Not Found",
                                    style = MaterialTheme.typography.h6,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = uiState.error.ifEmpty { "The requested asset could not be found" },
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = { navigator.pop() },
                                    colors = ButtonDefaults.buttonColors(backgroundColor = InLockBlue)
                                ) {
                                    Text("Go Back")
                                }
                            }
                        }
                        else -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp)
                                ) {
                                    if (uiState.asset?.imageUrl?.isNotEmpty() == true) {
                                        KamelImage(
                                            resource = asyncPainterResource(uiState.asset!!.imageUrl),
                                            contentDescription = uiState.asset!!.name,
                                            modifier = Modifier.fillMaxSize(),
                                            onLoading = {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.align(Alignment.Center),
                                                    color = InLockBlue
                                                )
                                            },
                                            onFailure = {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color.LightGray),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    AppIcon(
                                                        icon = AppIcons.BrokenImage,
                                                        contentDescription = "Failed to load image",
                                                        tint = Color.Gray,
                                                        modifier = Modifier.size(64.dp)
                                                    )
                                                }
                                            }
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.LightGray),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AppIcon(
                                                icon = AppIcons.Category,
                                                contentDescription = "No image",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(64.dp)
                                            )
                                        }
                                    }

                                    if (uiState.asset?.isRegisteredOnBlockchain == true) {
                                        Box(
                                            modifier = Modifier
                                                .padding(16.dp)
                                                .align(Alignment.TopEnd)
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF4CAF50))
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AppIcon(
                                                icon = AppIcons.VerifiedUser,
                                                contentDescription = "Verified on Blockchain",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = 4.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = uiState.asset?.name ?: "",
                                            style = MaterialTheme.typography.h5,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(InLockBlue.copy(alpha = 0.15f))
                                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = uiState.asset?.category ?: "",
                                                    color = InLockBlue,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Text(
                                            text = uiState.asset?.description ?: "",
                                            style = MaterialTheme.typography.body1
                                        )
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = 4.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Technical Details",
                                            style = MaterialTheme.typography.h6,
                                            fontWeight = FontWeight.Bold,
                                            color = InLockDarkBlue
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        DetailItem(
                                            label = "Asset ID",
                                            value = assetId
                                        )

                                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                                        DetailItem(
                                            label = "Manufacturer",
                                            value = uiState.asset?.manufacturerName ?: "Unknown"
                                        )

                                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                                        DetailItem(
                                            label = "Creation Date",
                                            value = uiState.asset?.createdAt?.let { formatTimestamp(it) } ?: "Unknown"
                                        )

                                        if (uiState.asset?.isRegisteredOnBlockchain == true) {
                                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                AppIcon(
                                                    icon = AppIcons.VerifiedUser,
                                                    contentDescription = null,
                                                    tint = Color(0xFF4CAF50),
                                                    modifier = Modifier.size(24.dp)
                                                )

                                                Spacer(modifier = Modifier.width(8.dp))

                                                Text(
                                                    text = "Registered on Blockchain",
                                                    style = MaterialTheme.typography.body1,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF4CAF50)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (uiState.asset?.properties?.isNotEmpty() == true) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        elevation = 4.dp
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp)
                                        ) {
                                            Text(
                                                text = "Properties",
                                                style = MaterialTheme.typography.h6,
                                                fontWeight = FontWeight.Bold,
                                                color = InLockDarkBlue
                                            )

                                            Spacer(modifier = Modifier.height(16.dp))

                                            uiState.asset?.properties?.forEach { (key, value) ->
                                                DetailItem(
                                                    label = key,
                                                    value = value
                                                )

                                                if (key != uiState.asset?.properties?.keys?.last()) {
                                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                                }
                                            }
                                        }
                                    }
                                }

                                if (uiState.ownershipHistory.isNotEmpty()) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        elevation = 4.dp
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp)
                                        ) {
                                            Text(
                                                text = "Ownership History",
                                                style = MaterialTheme.typography.h6,
                                                fontWeight = FontWeight.Bold,
                                                color = InLockDarkBlue
                                            )

                                            Spacer(modifier = Modifier.height(16.dp))

                                            uiState.ownershipHistory.forEachIndexed { index, record ->
                                                OwnershipHistoryItem(
                                                    record = record,
                                                    isLast = index == uiState.ownershipHistory.size - 1
                                                )

                                                if (index < uiState.ownershipHistory.size - 1) {
                                                    Divider(
                                                        modifier = Modifier
                                                            .padding(vertical = 8.dp)
                                                            .padding(start = 32.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    if (uiState.isUserOwner) {

                                        OutlinedButton(
                                            onClick = { screenModel.transferAsset() },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Transfer Ownership",
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                    }


                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        AnimatedVisibility(
                            visible = uiState.actionError.isNotEmpty(),
                            enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                            exit = fadeOut(animationSpec = tween(durationMillis = 300))
                        ) {
                            Snackbar(
                                modifier = Modifier.padding(16.dp),
                                backgroundColor = MaterialTheme.colors.error,
                                action = {
                                    TextButton(onClick = { screenModel.clearActionError() }) {
                                        Text("Dismiss", color = Color.White)
                                    }
                                }
                            ) {
                                Text(uiState.actionError, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun DetailItem(
        label: String,
        value: String
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = MaterialTheme.typography.caption,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.body1
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