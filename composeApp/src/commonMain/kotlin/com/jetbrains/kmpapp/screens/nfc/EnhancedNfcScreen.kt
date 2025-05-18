package com.jetbrains.kmpapp.screens.nfc

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.ui.platform.LocalDensity
import com.jetbrains.kmpapp.ui.components.EnhancedScreenTitle
import com.jetbrains.kmpapp.ui.components.ScreenTitleStyle
import com.jetbrains.kmpapp.ui.components.EnhancedActionButton
import com.jetbrains.kmpapp.ui.components.EnhancedCancelButton
import com.jetbrains.kmpapp.ui.components.StatusBadge
import com.jetbrains.kmpapp.ui.components.StatusBadgeStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.jetbrains.kmpapp.data.OwnershipRecord
import com.jetbrains.kmpapp.screens.main.EnhancedMainScreen
import com.jetbrains.kmpapp.ui.components.EnhancedBottomNavDestination
import com.jetbrains.kmpapp.ui.icons.AppIcon
import com.jetbrains.kmpapp.ui.icons.AppIcons
import com.jetbrains.kmpapp.ui.icons.PlatformIcon
import com.jetbrains.kmpapp.ui.theme.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private fun capitalizeStr(input: String): String {
    return if (input.isNotEmpty()) {
        input[0].uppercase() + input.substring(1)
    } else {
        input
    }
}

class EnhancedNfcScreen : Screen {
    @Composable
    override fun Content() {
        InLockTheme {
            val navigator = LocalNavigator.currentOrThrow
            val screenModel = getScreenModel<NfcScreenModel>()
            val uiState by screenModel.uiState.collectAsState()
            val verificationResult by screenModel.verificationResult.collectAsState()
            val scrollState = rememberScrollState()
            
            val headerHeight by animateDpAsState(
                targetValue = if (scrollState.value > 100 && verificationResult != null) 80.dp else 170.dp,
                animationSpec = tween(300)
            )
            
            val isScrollable = verificationResult != null
            LaunchedEffect(Unit) {
                EnhancedMainScreen.currentDestination.value = 
                    EnhancedBottomNavDestination.SCANNER
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(InLockBackground)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = headerHeight) 
                        .then(
                            if (isScrollable) {
                                Modifier.verticalScroll(scrollState)
                            } else {
                                Modifier
                            }
                        )
                ) {
                        AnimatedVisibility(
                        visible = uiState.error.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            elevation = 4.dp,
                            backgroundColor = InLockError.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIcon(
                                    icon = AppIcons.ErrorOutline,
                                    contentDescription = null,
                                    tint = InLockError,
                                    modifier = Modifier.size(24.dp)
                                )
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Text(
                                    text = uiState.error,
                                    color = InLockError,
                                    style = MaterialTheme.typography.body2
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        elevation = 4.dp,
                        shape = RoundedCornerShape(24.dp),
                        backgroundColor = InLockSurface
                    ) {
                        when {
                            verificationResult != null -> {
                                EnhancedVerificationResultView(
                                    verificationResult = verificationResult!!,
                                    onScanAgain = { screenModel.resetScan() }
                                )
                            }
                            uiState.isScanning -> {
                                EnhancedNfcScanningView(
                                    onCancelScan = { screenModel.stopNfcScan() }
                                )
                            }
                            else -> {
                                EnhancedNfcScanButton(
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
                        }
                    }

                    AnimatedVisibility(
                        visible = uiState.blockchainResponse.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(
                                    width = 1.dp,
                                    color = InLockInfo.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            StatusBadge(
                                text = uiState.blockchainResponse,
                                icon = AppIcons.Blockchain,
                                color = InLockInfo,
                                style = StatusBadgeStyle.SUBTLE
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(100.dp))
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeight)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(InLockPrimary, InLockPrimaryVariant)
                            )
                        )
                ) {
                    val density = LocalDensity.current
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        for (i in 0..6) {
                            val y = size.height * (0.2f + i * 0.1f)
                            drawLine(
                                color = Color.White.copy(alpha = 0.08f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.5f
                            )
                        }
                        
                        drawLine(
                            color = Color.White.copy(alpha = 0.1f),
                            start = Offset(size.width * 0.1f, 0f),
                            end = Offset(size.width * 0.4f, size.height),
                            strokeWidth = 2f
                        )
                        
                        drawLine(
                            color = Color.White.copy(alpha = 0.1f),
                            start = Offset(size.width * 0.5f, 0f),
                            end = Offset(size.width * 0.8f, size.height),
                            strokeWidth = 2f
                        )
                        
                        drawLine(
                            color = Color.White.copy(alpha = 0.1f),
                            start = Offset(size.width * 0.9f, 0f),
                            end = Offset(size.width * 0.6f, size.height),
                            strokeWidth = 2f
                        )
                    }
                    
                    if (headerHeight > 100.dp) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "NFC Verification",
                                style = MaterialTheme.typography.h5,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Verify product authenticity using your device",
                                style = MaterialTheme.typography.subtitle1,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "NFC Verification",
                                style = MaterialTheme.typography.h6,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun EnhancedNfcScanButton(
        isScanning: Boolean,
        enabled: Boolean,
        onScanClick: () -> Unit
    ) {
        val infiniteTransition = rememberInfiniteTransition()
        
        val scale1 by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Restart
            )
        )
        
        val scale2 by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = EaseInOutCubic, delayMillis = 400),
                repeatMode = RepeatMode.Restart
            )
        )
        
        val scale3 by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = EaseInOutCubic, delayMillis = 800),
                repeatMode = RepeatMode.Restart
            )
        )
        
        val buttonScale = remember { mutableStateOf(1f) }
        
        val hoverAnim by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500),
                repeatMode = RepeatMode.Reverse
            )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "NFC Tag Authentication",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = InLockTextPrimary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Scan any product tag to verify its authenticity on the blockchain",
                textAlign = TextAlign.Center,
                color = InLockTextSecondary,
                style = MaterialTheme.typography.body2
            )

            Spacer(modifier = Modifier.height(40.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(220.dp)
                    .scale(hoverAnim)
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(scale1)
                        .alpha((1.5f - scale1) * 0.3f)
                        .clip(CircleShape)
                        .background(InLockPrimary.copy(alpha = 0.2f))
                )
                
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(scale2)
                        .alpha((1.5f - scale2) * 0.3f)
                        .clip(CircleShape)
                        .background(InLockPrimary.copy(alpha = 0.2f))
                )
                
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(scale3)
                        .alpha((1.5f - scale3) * 0.3f)
                        .clip(CircleShape)
                        .background(InLockPrimary.copy(alpha = 0.2f))
                )

                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(buttonScale.value)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    InLockPrimary,
                                    InLockSecondary
                                )
                            )
                        )
                        .clickable(
                            enabled = enabled,
                            onClick = {
                                buttonScale.value = 0.9f
                                onScanClick()
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AppIcon(
                            icon = AppIcons.Nfc,
                            contentDescription = "Scan NFC",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "TAP TO SCAN",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Hold your phone near a product tag to authenticate",
                textAlign = TextAlign.Center,
                color = InLockTextSecondary,
                style = MaterialTheme.typography.caption
            )
        }
    }

    @Composable
    private fun EnhancedNfcScanningView(
        onCancelScan: () -> Unit
    ) {
        val infiniteTransition = rememberInfiniteTransition()
        
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing)
            )
        )
        
        val pulse1 by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Restart
            )
        )
        
        val pulse2 by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = EaseInOutCubic, delayMillis = 500),
                repeatMode = RepeatMode.Restart
            )
        )
        
        val pulse3 by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = EaseInOutCubic, delayMillis = 1000),
                repeatMode = RepeatMode.Restart
            )
        )
        
        val colorPulseAnim by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000),
                repeatMode = RepeatMode.Reverse
            )
        )
        
        val colorPulse = if (colorPulseAnim < 0.5f) InLockPrimary else InLockSecondary

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            Text(
                text = "Scanning for NFC Tags",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = InLockTextPrimary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Hold your phone near a product tag for authentication",
                textAlign = TextAlign.Center,
                color = InLockTextSecondary,
                style = MaterialTheme.typography.body2
            )

            Spacer(modifier = Modifier.height(40.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(240.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .scale(pulse1)
                        .alpha((1.8f - pulse1) * 0.5f)
                        .clip(CircleShape)
                        .background(InLockPrimary.copy(alpha = 0.2f))
                )
                
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .scale(pulse2)
                        .alpha((1.8f - pulse2) * 0.5f)
                        .clip(CircleShape)
                        .background(InLockPrimary.copy(alpha = 0.2f))
                )
                
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .scale(pulse3)
                        .alpha((1.8f - pulse3) * 0.5f)
                        .clip(CircleShape)
                        .background(InLockPrimary.copy(alpha = 0.2f))
                )
                
                Canvas(
                    modifier = Modifier.size(200.dp)
                ) {
                    drawArc(
                        color = InLockPrimary,
                        startAngle = rotation,
                        sweepAngle = 150f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                    
                    drawArc(
                        color = InLockSecondary,
                        startAngle = rotation + 180f,
                        sweepAngle = 150f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(colorPulse.copy(alpha = 0.2f))
                        .border(
                            width = 2.dp,
                            color = colorPulse,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AppIcon(
                            icon = AppIcons.Nfc,
                            contentDescription = "NFC",
                            tint = colorPulse,
                            modifier = Modifier.size(48.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        CircularProgressIndicator(
                            color = colorPulse,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                modifier = Modifier.height(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(3) { index ->
                    val delay = index * 300
                    val animAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = EaseInOutCubic, delayMillis = delay),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                    
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(8.dp)
                            .alpha(animAlpha)
                            .clip(CircleShape)
                            .background(InLockPrimary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            EnhancedCancelButton(
                text = "Cancel Scanning",
                onClick = onCancelScan,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        }
    }

    @Composable
    private fun EnhancedVerificationResultView(
        verificationResult: VerificationResult,
        onScanAgain: () -> Unit
    ) {
        val infiniteTransition = rememberInfiniteTransition()
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            )
        )
        
        var isVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            isVisible = true
        }
        
        val successColor = Color(0xFF4CAF50)
        val errorColor = InLockError

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(500)) + 
                        expandVertically(animationSpec = tween(500)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .border(
                            width = 2.dp,
                            color = if (verificationResult.isAuthentic) successColor.copy(alpha = 0.3f) else errorColor.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val iconColor = if (verificationResult.isAuthentic) successColor else errorColor
                    val scaleModifier = if (verificationResult.isAuthentic) 
                        Modifier.scale(pulseScale) else Modifier
                        
                    AppIcon(
                        icon = if (verificationResult.isAuthentic) 
                            AppIcons.VerifiedUser else AppIcons.ErrorOutline,
                        contentDescription = "Verification Status",
                        tint = iconColor,
                        modifier = Modifier
                            .size(70.dp)
                            .then(scaleModifier)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (verificationResult.isAuthentic) 
                            "Authentic Product" else "Not Authentic",
                        style = MaterialTheme.typography.h5,
                        fontWeight = FontWeight.Bold,
                        color = iconColor
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = if (verificationResult.isAuthentic)
                            "This product has been verified as authentic" 
                        else
                            "This product could not be verified",
                        textAlign = TextAlign.Center,
                        color = if (verificationResult.isAuthentic) 
                            successColor.copy(alpha = 0.8f) 
                        else 
                            errorColor.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.body2
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (verificationResult.isAuthentic) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (verificationResult.properties.containsKey("status") && 
                                    verificationResult.properties["status"] == "stolen") {
                                StatusBadge(
                                    text = "Stolen",
                                    icon = AppIcons.ErrorOutline,
                                    color = errorColor,
                                    style = StatusBadgeStyle.SUBTLE
                                )
                            } else {
                                StatusBadge(
                                    text = "Blockchain Verified",
                                    icon = AppIcons.Blockchain,
                                    color = successColor,
                                    style = StatusBadgeStyle.SUBTLE
                                )
                            }
                            
                            if (verificationResult.category.isNotEmpty()) {
                                StatusBadge(
                                    text = verificationResult.category,
                                    icon = AppIcons.Category,
                                    color = InLockAccent,
                                    style = StatusBadgeStyle.SUBTLE
                                )
                            }
                        }
                    }
                }
            }

            if (verificationResult.isAuthentic) {
                Spacer(modifier = Modifier.height(24.dp))

                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(500, delayMillis = 300)) + 
                            expandVertically(animationSpec = tween(500, delayMillis = 300)),
                ) {
                    EnhancedDetailCard(
                        title = "Product Details",
                        icon = AppIcons.Category,
                        iconTint = InLockPrimary,
                        pairs = listOf(
                            "Product Name" to verificationResult.productName,
                            "Category" to verificationResult.category,
                            "Asset ID" to verificationResult.assetId
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(500, delayMillis = 600)) + 
                            expandVertically(animationSpec = tween(500, delayMillis = 600)),
                ) {
                    EnhancedDetailCard(
                        title = "Ownership Details",
                        icon = AppIcons.Business, 
                        iconTint = InLockSecondary,
                        pairs = buildList {
                            add("Current Owner" to verificationResult.currentOwner)
                            add("Manufacturer" to verificationResult.manufacturer)
                            add("Registration Date" to verificationResult.registrationDate)
                            if (verificationResult.previousOwners > 0) {
                                add("Previous Owners" to verificationResult.previousOwners.toString())
                                add("Last Transfer" to verificationResult.lastTransferDate)
                            }
                        }
                    )
                }

                if (verificationResult.properties.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(animationSpec = tween(500, delayMillis = 900)) + 
                                expandVertically(animationSpec = tween(500, delayMillis = 900)),
                    ) {
                        EnhancedDetailCard(
                            title = "Properties",
                            icon = AppIcons.Category,
                            iconTint = InLockAccent,
                            pairs = verificationResult.properties
                                .filter { (key, value) -> key != "status" || value == "stolen" }
                                .map { (key, value) -> capitalizeStr(key) to value }
                        )
                    }
                }

                if (verificationResult.ownershipHistory.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(animationSpec = tween(500, delayMillis = 1200)) + 
                                expandVertically(animationSpec = tween(500, delayMillis = 1200)),
                    ) {
                        Card(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(InLockPrimary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AppIcon(
                                            icon = AppIcons.Category,
                                            contentDescription = null,
                                            tint = InLockPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(16.dp))
                                    
                                    Text(
                                        text = "Ownership History",
                                        style = MaterialTheme.typography.h6,
                                        fontWeight = FontWeight.Bold,
                                        color = InLockTextPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                verificationResult.ownershipHistory.forEachIndexed { index, record ->
                                    EnhancedOwnershipHistoryItem(
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
            }

            Spacer(modifier = Modifier.height(32.dp))

            EnhancedActionButton(
                text = "Scan Another Product",
                onClick = onScanAgain,
                icon = AppIcons.Nfc,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    private fun EnhancedDetailCard(
        title: String,
        icon: PlatformIcon,
        iconTint: Color,
        pairs: List<Pair<String, String>>
    ) {
        Card(
            elevation = 4.dp,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(iconTint.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AppIcon(
                            icon = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = title,
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold,
                        color = InLockTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                pairs.forEachIndexed { index, (label, value) ->
                    EnhancedDetailItem(
                        label = label,
                        value = value
                    )

                    if (index < pairs.size - 1) {
                        Divider(
                            color = Color(0xFFEEEEEE),
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun EnhancedDetailItem(
        label: String,
        value: String
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = MaterialTheme.typography.caption,
                color = InLockTextSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.Medium,
                color = InLockTextPrimary
            )
        }
    }

    @Composable
    private fun EnhancedOwnershipHistoryItem(
        record: OwnershipRecord,
        isLast: Boolean
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(modifier = Modifier.width(48.dp)) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (isLast) InLockPrimary else InLockTextTertiary
                        )
                        .align(Alignment.TopCenter),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLast) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
                
                if (!isLast) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(80.dp)
                            .background(InLockTextTertiary)
                            .align(Alignment.TopCenter)
                            .offset(y = 16.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (record.action) {
                        "register" -> "Initial Registration"
                        "transfer" -> "Transfer of Ownership"
                        else -> capitalizeStr(record.action)
                    },
                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                    color = if (isLast) InLockPrimary else InLockTextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Owner ID: ${record.user_id}",
                    style = MaterialTheme.typography.caption,
                    color = InLockTextSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Date: ${formatTimestamp((record.timestamp * 1000).toLong())}",
                    style = MaterialTheme.typography.caption,
                    color = InLockTextSecondary
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
}