package com.jetbrains.kmpapp.screens.assets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.jetbrains.kmpapp.data.OwnershipRecord
import com.jetbrains.kmpapp.data.Product
import com.jetbrains.kmpapp.ui.icons.AppIcon
import com.jetbrains.kmpapp.ui.icons.AppIcons
import com.jetbrains.kmpapp.ui.theme.InLockBlue
import com.jetbrains.kmpapp.ui.theme.InLockTextSecondary
import com.jetbrains.kmpapp.ui.theme.InLockTextPrimary
import com.jetbrains.kmpapp.ui.theme.InLockTextTertiary
import com.jetbrains.kmpapp.ui.components.StatusBadge
import com.jetbrains.kmpapp.ui.components.StatusBadgeStyle
import com.jetbrains.kmpapp.ui.components.VerifiedBadge
import com.jetbrains.kmpapp.ui.components.BlockchainBadge
import com.jetbrains.kmpapp.ui.components.CategoryBadge
import com.jetbrains.kmpapp.ui.components.AssetStatusBadgeRow
import com.jetbrains.kmpapp.ui.theme.InLockTheme
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class EnhancedAssetDetailScreen(private val assetId: String) : Screen {
    @Composable
    override fun Content() {
        InLockTheme {
            val navigator = LocalNavigator.currentOrThrow
            val screenModel = getScreenModel<EnhancedAssetDetailScreenModel>()
            val uiState by screenModel.uiState.collectAsState()
            val scrollState = rememberScrollState()
            val scope = rememberCoroutineScope()

            var showReportDialog by remember { mutableStateOf(false) }

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
                        contentColor = Color.White,
                        actions = {
                            IconButton(onClick = {
                            }) {
                                AppIcon(
                                    icon = AppIcons.QrCode,
                                    contentDescription = "Share QR",
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
                ) {
                    when {
                        uiState.isLoading -> {
                            LoadingIndicator()
                        }
                        uiState.asset == null -> {
                            AssetNotFoundContent(
                                errorMessage = uiState.error,
                                onGoBack = { navigator.pop() }
                            )
                        }
                        else -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                            ) {
                                AssetHeader(
                                    uiState = uiState
                                )

                                AssetDetailsSection(
                                    uiState = uiState,
                                    isUserOwner = uiState.isUserOwner
                                )

                                AssetStatusSection(
                                    uiState = uiState
                                )

                                AssetPropertiesSection(
                                    uiState = uiState
                                )

                                OwnershipHistorySection(
                                    uiState = uiState
                                )

                                AssetActionsSection(
                                    uiState = uiState,
                                    onTransferAsset = {
                                    },
                                    onReportStolen = { showReportDialog = true }
                                )

                                Spacer(modifier = Modifier.height(32.dp))
                            }

                            if (showReportDialog) {
                                ReportStolenDialog(
                                    onDismiss = { showReportDialog = false },
                                    onConfirm = {
                                        showReportDialog = false
                                        screenModel.reportAssetStolen()
                                    },
                                    assetName = uiState.asset?.name ?: "Asset"
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = uiState.actionError.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        Snackbar(
                            action = {
                                TextButton(
                                    onClick = { screenModel.clearActionError() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                                ) {
                                    Text("Dismiss")
                                }
                            },
                            backgroundColor = MaterialTheme.colors.error
                        ) {
                            Text(uiState.actionError, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun AssetHeader(
        uiState: EnhancedAssetDetailUiState
    ) {
        val asset = uiState.asset ?: return

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            AssetHeaderImage(imageUrl = asset.imageUrl, assetName = asset.name)
            AssetHeaderBadges(
                isBlockchainRegistered = asset.isRegisteredOnBlockchain,
                isStolen = uiState.isStolen
            )
            AssetHeaderInfo(
                name = asset.name,
                category = asset.category,
                id = asset.id
            )
        }
    }
    
    @Composable
    private fun AssetHeaderImage(
        imageUrl: String,
        assetName: String
    ) {
        if (imageUrl.isNotEmpty()) {
            KamelImage(
                resource = asyncPainterResource(imageUrl),
                contentDescription = assetName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onLoading = { AssetImagePlaceholder(isLoading = true) },
                onFailure = { AssetImagePlaceholder(isError = true) }
            )
        } else {
            AssetImagePlaceholder()
        }
    }
    
    @Composable
    private fun AssetImagePlaceholder(
        isLoading: Boolean = false,
        isError: Boolean = false
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> CircularProgressIndicator(color = InLockBlue)
                isError -> AppIcon(
                    icon = AppIcons.BrokenImage,
                    contentDescription = "Failed to load image",
                    tint = Color.Gray,
                    modifier = Modifier.size(64.dp)
                )
                else -> AppIcon(
                    icon = AppIcons.Category,
                    contentDescription = "No image",
                    tint = Color.Gray,
                    modifier = Modifier.size(64.dp)
                )
            }
        }
    }
    
    @Composable
    private fun AssetHeaderBadges(
        isBlockchainRegistered: Boolean,
        isStolen: Boolean
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            if (isBlockchainRegistered) {
                BlockchainBadge(
                    registered = true,
                    style = StatusBadgeStyle.GRADIENT
                )
            }

            if (isStolen) {
                StatusBadge(
                    text = "Stolen",
                    icon = AppIcons.ErrorOutline,
                    color = Color.Red,
                    style = StatusBadgeStyle.SOLID
                )
            }
            
            VerifiedBadge(
                verified = true,
                style = StatusBadgeStyle.GRADIENT
            )
            }
        }
    }
    
    @Composable
    private fun AssetHeaderInfo(
        name: String,
        category: String,
        id: String
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
            Text(
                text = name,
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = category,
                style = MaterialTheme.typography.subtitle1,
                color = Color.White.copy(alpha = 0.8f)
            )

            AssetIdChip(id = id)
            }
        }
    }
    
    @Composable
    private fun AssetIdChip(id: String) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.2f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "ID: $id",
                style = MaterialTheme.typography.caption,
                color = Color.White
            )
        }
    }

    @Composable
    private fun StatusBadge(
        icon: com.jetbrains.kmpapp.ui.icons.PlatformIcon,
        color: Color,
        tooltip: String
    ) {
        com.jetbrains.kmpapp.ui.components.StatusBadge(
            text = tooltip,
            icon = icon,
            color = color,
            style = StatusBadgeStyle.GRADIENT
        )
    }

    @Composable
    private fun AssetDetailsSection(
        uiState: EnhancedAssetDetailUiState,
        isUserOwner: Boolean
    ) {
        val asset = uiState.asset ?: return

        InfoCard(title = "Asset Details") {
            AssetDescription(description = asset.description)
            
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            
            AssetDetailsList(
                asset = asset,
                isUserOwner = isUserOwner
            )
        }
    }
    
    @Composable
    private fun InfoCard(
        title: String,
        content: @Composable () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Styles.standardPadding),
            shape = Styles.cardShape,
            elevation = Styles.cardElevation
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Styles.standardPadding)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = InLockBlue
                )
                
                Spacer(modifier = Modifier.height(Styles.standardPadding))
                
                content()
            }
        }
    }
    
    @Composable
    private fun AssetDescription(description: String) {
        Text(
            text = "Description",
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.body1
        )
    }
    
    @Composable
    private fun AssetDetailsList(
        asset: Product,
        isUserOwner: Boolean
    ) {
        DetailItem(
            label = "Manufacturer",
            value = asset.manufacturerName
        )

        Spacer(modifier = Modifier.height(8.dp))

        DetailItem(
            label = "Current Owner",
            value = if (isUserOwner) "You" else "Other User ID: ${asset.currentOwner}",
            isHighlighted = isUserOwner
        )

        Spacer(modifier = Modifier.height(8.dp))

        DetailItem(
            label = "Creation Date",
            value = formatTimestamp(asset.createdAt)
        )
    }

    @Composable
    private fun AssetStatusSection(uiState: EnhancedAssetDetailUiState) {
        val asset = uiState.asset ?: return
        val assetStatus = asset.properties["status"]
        val isStolen = assetStatus == "stolen"
        
        
        if (!(asset.isRegisteredOnBlockchain || isStolen || assetStatus != null)) {
            return
        }

        InfoCard(title = "Asset Status") {
            AssetStatusBadges(
                asset = asset,
                isStolen = isStolen,
                assetStatus = assetStatus
            )
            
            AssetStatusDescriptions(
                isRegisteredOnBlockchain = asset.isRegisteredOnBlockchain,
                isStolen = isStolen,
                assetStatus = assetStatus
            )
        }
    }
    
    @Composable
    private fun AssetStatusBadges(
        asset: Product,
        isStolen: Boolean,
        assetStatus: String?
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            
            if (asset.isRegisteredOnBlockchain) {
                BlockchainBadge(
                    registered = true,
                    style = StatusBadgeStyle.GRADIENT
                )
            }
            
            
            if (isStolen) {
                StatusBadge(
                    text = "Reported Stolen",
                    icon = AppIcons.ErrorOutline,
                    color = Color.Red,
                    style = StatusBadgeStyle.SOLID
                )
            }
            
            
            if (assetStatus != null && assetStatus != "stolen") {
                StatusBadge(
                    text = assetStatus.capitalize(),
                    icon = AppIcons.Category,
                    color = Color.Gray,
                    style = StatusBadgeStyle.OUTLINED
                )
            }
            
            
            if (asset.category.isNotEmpty()) {
                CategoryBadge(
                    category = asset.category,
                    style = StatusBadgeStyle.SUBTLE
                )
            }
        }
    }
    
    @Composable
    private fun AssetStatusDescriptions(
        isRegisteredOnBlockchain: Boolean,
        isStolen: Boolean,
        assetStatus: String?
    ) {
        val statusDescriptions = buildList {
            if (isRegisteredOnBlockchain) {
                add("This asset is registered and verified on the blockchain")
            }
            
            if (isStolen) {
                add("This asset has been reported as stolen")
            }
            
            if (assetStatus != null && assetStatus != "stolen") {
                add("This asset has a special status: ${assetStatus.capitalize()}")
            }
        }
        
        statusDescriptions.forEachIndexed { index, description ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Text(
                text = description,
                style = MaterialTheme.typography.caption,
                color = InLockTextSecondary
            )
        }
    }


    @Composable
    private fun AssetPropertiesSection(
        uiState: EnhancedAssetDetailUiState
    ) {
        val asset = uiState.asset ?: return

        if (asset.properties.isNotEmpty()) {
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
                        color = InLockBlue
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    asset.properties.forEach { (key, value) ->
                        DetailItem(
                            label = key,
                            value = value
                        )

                        if (key != asset.properties.keys.last()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun OwnershipHistorySection(
        uiState: EnhancedAssetDetailUiState
    ) {
        val history = uiState.ownershipHistory
        
        if (history.isEmpty()) {
            return
        }
        
        InfoCard(title = "Ownership History") {
            Timeline(
                items = history,
                itemContent = { record, isLast ->
                    OwnershipHistoryItemContent(record = record, isLast = isLast)
                }
            )
        }
    }
    
    @Composable
    private fun <T> Timeline(
        items: List<T>,
        itemContent: @Composable (item: T, isLast: Boolean) -> Unit
    ) {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                TimelineIndicator(
                    isLast = index == items.size - 1,
                    showConnector = index < items.size - 1
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Box(modifier = Modifier.weight(1f)) {
                    itemContent(item, index == items.size - 1)
                }
            }
            
            if (index < items.size - 1) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    
    @Composable
    private fun TimelineIndicator(
        isLast: Boolean,
        showConnector: Boolean
    ) {
        Box(
            modifier = Modifier.width(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (showConnector) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(50.dp)
                        .background(Color.LightGray)
                        .align(Alignment.BottomCenter)
                )
            }

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (isLast) InLockBlue else Color.LightGray)
            )
        }
    }
    
    @Composable
    private fun OwnershipHistoryItemContent(
        record: OwnershipRecord,
        isLast: Boolean
    ) {
        val textColor = if (isLast) InLockBlue else Color.Black
        val fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal
        
        Column {
            Text(
                text = getActionLabel(record.action),
                style = MaterialTheme.typography.body1,
                fontWeight = fontWeight,
                color = textColor
            )

            Spacer(modifier = Modifier.height(Styles.tinySpacing))

            OwnershipRecordDetails(
                userId = record.user_id,
                timestamp = record.timestamp.toLong()
            )
        }
    }
    
    @Composable
    private fun OwnershipRecordDetails(
        userId: String, 
        timestamp: Long
    ) {
        Text(
            text = "Owner: $userId",
            style = MaterialTheme.typography.caption,
            color = InLockTextSecondary
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Date: ${formatTimestamp((timestamp * 1000).toLong())}",
            style = MaterialTheme.typography.caption,
            color = InLockTextSecondary
        )
    }

    @Composable
    private fun AssetActionsSection(
        uiState: EnhancedAssetDetailUiState,
        onTransferAsset: () -> Unit,
        onReportStolen: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = 4.dp,
            backgroundColor = Color(0xFFF5F7FA)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Actions",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = InLockBlue
                )

                Spacer(modifier = Modifier.height(16.dp))

                ActionButton(
                    icon = AppIcons.Nfc,
                    text = "Transfer Asset",
                    description = "Transfer this asset to another user",
                    onClick = onTransferAsset,
                    isEnabled = !uiState.isStolen
                )

                Spacer(modifier = Modifier.height(12.dp))

                ActionButton(
                    icon = AppIcons.ErrorOutline,
                    text = "Report Stolen",
                    description = "Mark this asset as stolen",
                    onClick = onReportStolen,
                    isEnabled = !uiState.isStolen,
                    isDangerous = true
                )
            }
        }
    }

    @Composable
    private fun ActionButton(
        icon: com.jetbrains.kmpapp.ui.icons.PlatformIcon,
        text: String,
        description: String,
        onClick: () -> Unit,
        isEnabled: Boolean = true,
        isDangerous: Boolean = false
    ) {
        val backgroundColor = when {
            !isEnabled -> Color.Gray.copy(alpha = 0.1f)
            isDangerous -> Color.Red.copy(alpha = 0.1f)
            else -> InLockBlue.copy(alpha = 0.1f)
        }

        val contentColor = when {
            !isEnabled -> Color.Gray
            isDangerous -> Color.Red
            else -> InLockBlue
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor)
                .let {
                    if (isEnabled) {
                        it.clickable(onClick = onClick)
                    } else {
                        it
                    }
                }
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    AppIcon(
                        icon = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )

                    Text(
                        text = description,
                        style = MaterialTheme.typography.caption,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }

                if (isEnabled) {
                    AppIcon(
                        icon = AppIcons.Category,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun DetailItem(
        label: String,
        value: String,
        isHighlighted: Boolean = false
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.width(110.dp)
            )

            Text(
                text = value,
                style = MaterialTheme.typography.body1,
                color = if (isHighlighted) InLockBlue else Color.Black
            )
        }
    }



    @Composable
    private fun ReportStolenDialog(
        onDismiss: () -> Unit,
        onConfirm: () -> Unit,
        assetName: String
    ) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                shape = RoundedCornerShape(16.dp),
                backgroundColor = Color.White,
                elevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DialogWarningIcon()
                    DialogTitle(title = "Report Stolen", color = Color.Red)
                    DialogContent(
                        assetName = assetName,
                        description = "This action cannot be undone and will alert all connected systems that this asset has been stolen."
                    )
                    DialogActions(
                        dismissText = "Cancel",
                        confirmText = "Report Stolen",
                        isDangerous = true,
                        onDismiss = onDismiss,
                        onConfirm = onConfirm
                    )
                }
            }
        }
    }
    
    @Composable
    private fun DialogWarningIcon() {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.Red),
            contentAlignment = Alignment.Center
        ) {
            AppIcon(
                icon = AppIcons.ErrorOutline,
                contentDescription = "Warning",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
    
    @Composable
    private fun DialogTitle(title: String, color: Color = InLockBlue) {
        Text(
            text = title,
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
    
    @Composable
    private fun DialogContent(
        assetName: String,
        description: String
    ) {
        Text(
            text = "Are you sure you want to report '$assetName' as stolen?",
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.caption,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
    
    @Composable
    private fun DialogActions(
        dismissText: String,
        confirmText: String,
        isDangerous: Boolean = false,
        onDismiss: () -> Unit,
        onConfirm: () -> Unit
    ) {
        val confirmColor = if (isDangerous) Color.Red else InLockBlue
        
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = InLockBlue
                ),
                border = BorderStroke(1.dp, InLockBlue),
                modifier = Modifier.weight(1f)
            ) {
                Text(dismissText)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = confirmColor
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    confirmText,
                    color = Color.White
                )
            }
        }
    }



    
    private object Styles {
        val standardPadding = 16.dp
        val smallSpacing = 8.dp
        val tinySpacing = 4.dp
        
        val cardShape = RoundedCornerShape(16.dp)
        val chipShape = RoundedCornerShape(16.dp)
        
        val cardElevation = 4.dp
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
    
    private fun String.isNotEmpty(): Boolean {
        return this.length > 0
    }
    
    private fun getActionLabel(action: String): String {
        return when (action) {
            "register" -> "Initial Registration"
            "transfer" -> "Transfer of Ownership"
            else -> action.capitalize()
        }
    }
    
    @Composable
    private fun LoadingIndicator() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = InLockBlue)
        }
    }
    
    @Composable
    private fun AssetNotFoundContent(
        errorMessage: String,
        onGoBack: () -> Unit
    ) {
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
                text = errorMessage.ifEmpty { "The requested asset could not be found" },
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onGoBack,
                colors = ButtonDefaults.buttonColors(backgroundColor = InLockBlue)
            ) {
                Text("Go Back")
            }
        }
    }
}