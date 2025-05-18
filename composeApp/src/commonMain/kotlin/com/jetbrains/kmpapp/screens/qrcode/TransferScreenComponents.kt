package com.jetbrains.kmpapp.screens.qrcode

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.data.Product
import com.jetbrains.kmpapp.ui.components.ActionButtonRow
import com.jetbrains.kmpapp.ui.components.BlockchainBadge
import com.jetbrains.kmpapp.ui.components.ButtonAction
import com.jetbrains.kmpapp.ui.components.ContentCard
import com.jetbrains.kmpapp.ui.components.ProductImageContainer
import com.jetbrains.kmpapp.ui.components.StatusBadgeStyle
import com.jetbrains.kmpapp.ui.icons.AppIcon
import com.jetbrains.kmpapp.ui.icons.AppIcons
import com.jetbrains.kmpapp.ui.theme.InLockBlue


@Composable
fun RecipientInfoCard(
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
fun SelectProductContent(
    products: List<Product>,
    onProductSelected: (String) -> Unit,
    onScanRfid: () -> Unit
) {
    ContentCard(
        title = "Select Asset to Transfer",
        cornerRadius = 16,
        elevation = 4
    ) {
        OutlinedButton(
            onClick = onScanRfid,
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
        
        Text(
            "Or select from your assets:",
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(products) { product ->
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
fun ProductListItem(
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
fun RfidScanningContent(
    onCancel: () -> Unit
) {
    ContentCard(
        cornerRadius = 16,
        elevation = 4
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
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
                        .background(InLockBlue.copy(alpha = 0.5f))
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

            ActionButtonRow(
                primaryAction = ButtonAction(
                    text = "Cancel Scan",
                    onClick = onCancel,
                    isDanger = true
                )
            )
        }
    }
}

@Composable
fun SelectedProductContent(
    product: Product,
    recipientName: String,
    isRfidScanning: Boolean,
    hasPhysicalVerification: Boolean,
    physicalTimeRemaining: String,
    onStartRfidScan: () -> Unit,
    onCancelRfidScan: () -> Unit,
    onCancel: () -> Unit
) {
    ContentCard(
        title = "Verify Asset Ownership",
        cornerRadius = 16,
        elevation = 4
    ) {
        ProductImageContainer(
            product = product,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            cornerRadius = 8
        )

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

        VerificationStatusCard(
            title = "Code Verification Required",
            message = "Please scan the physical asset's RFID tag to verify your ownership before transferring.",
            backgroundColor = Color(0xFFF9FBE7),
            textColor = Color(0xFF827717)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        PhysicalVerificationStatusCard(
            hasVerification = hasPhysicalVerification,
            timeRemaining = physicalTimeRemaining
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isRfidScanning) {
            ActionButtonRow(
                primaryAction = ButtonAction(
                    text = "Cancel Scanning",
                    onClick = onCancelRfidScan,
                    isDanger = true
                )
            )
        } else {
            ActionButtonRow(
                primaryAction = ButtonAction(
                    text = "Verify Intent",
                    onClick = onStartRfidScan,
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

@Composable
fun VerificationStatusCard(
    title: String,
    message: String,
    backgroundColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        Column {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                message,
                color = textColor
            )
        }
    }
}

@Composable
fun PhysicalVerificationStatusCard(
    hasVerification: Boolean,
    timeRemaining: String
) {
    val backgroundColor = if (hasVerification) Color(0xFFE8F5E9) else Color(0xFFFFF9C4)
    val textColor = if (hasVerification) Color(0xFF4CAF50) else Color(0xFFF57F17)
    val icon = if (hasVerification) AppIcons.CheckCircle else AppIcons.Warning
    val title = if (hasVerification) "Physical Verification Complete" else "Physical Verification Required"
    val message = if (hasVerification) {
        "Asset physically verified. $timeRemaining"
    } else {
        "Please use the NFC Verification screen to physically verify this asset before transferring."
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(
                icon = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                
                Text(
                    message,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun VerifiedProductContent(
    product: Product,
    recipientName: String,
    expiryTime: Long,
    hasPhysicalVerification: Boolean,
    physicalTimeRemaining: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    ContentCard(
        title = "Confirm Transfer",
        cornerRadius = 16,
        elevation = 4
    ) {
        ProductImageContainer(
            product = product,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            cornerRadius = 8
        )

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
        
        Text(
            "Transfer Details",
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold,
            color = InLockBlue
        )

        Spacer(modifier = Modifier.height(16.dp))

        TransferDetailRow("From:", "You")
        TransferDetailRow("To:", recipientName)
        TransferDetailRow("Asset ID:", product.id)

        Spacer(modifier = Modifier.height(24.dp))

        VerificationStatusCard(
            title = "Code Verification Complete",
            message = "Transfer permission confirmed. ${if (expiryTime > 0) "Valid until ${formatExpiryTime(expiryTime)}" else ""} You can now complete the transfer.",
            backgroundColor = Color(0xFFE8F5E9),
            textColor = Color(0xFF4CAF50)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        
        PhysicalVerificationStatusCard(
            hasVerification = hasPhysicalVerification,
            timeRemaining = physicalTimeRemaining
        )

        Spacer(modifier = Modifier.height(24.dp))

        ActionButtonRow(
            primaryAction = ButtonAction(
                text = "Complete Transfer",
                onClick = onConfirm
            ),
            secondaryAction = ButtonAction(
                text = "Cancel",
                onClick = onCancel,
                isPrimary = false
            )
        )
    }
}

@Composable
fun TransferDetailRow(label: String, value: String) {
    Row {
        Text(
            label,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(80.dp)
        )

        Text(value)
    }
    
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun ConfirmationCodeContent(
    product: Product,
    confirmationCode: String,
    userEnteredCode: String,
    hasPhysicalVerification: Boolean,
    physicalTimeRemaining: String,
    onCodeChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit
) {
    ContentCard(
        title = "Confirm Transfer Intent",
        cornerRadius = 16,
        elevation = 4
    ) {
        Text(
            "To verify your intent to transfer this asset, please enter the confirmation code below.",
            style = MaterialTheme.typography.body1
        )

        Spacer(modifier = Modifier.height(24.dp))

        ProductImageContainer(
            product = product,
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.CenterHorizontally),
            cornerRadius = 8
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            product.name,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        
        Text(
            "Confirmation Code:",
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            confirmationCode,
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold,
            color = InLockBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        
        OutlinedTextField(
            value = userEnteredCode,
            onValueChange = { onCodeChanged(it.uppercase()) },
            label = { Text("Enter Code") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.h6.copy(textAlign = TextAlign.Center)
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        // Physical verification status
        PhysicalVerificationStatusCard(
            hasVerification = hasPhysicalVerification,
            timeRemaining = physicalTimeRemaining
        )

        Spacer(modifier = Modifier.height(24.dp))

        ActionButtonRow(
            primaryAction = ButtonAction(
                text = "Cancel",
                onClick = onCancel,
                isDanger = true
            )
        )
    }
}

fun formatExpiryTime(timestamp: Long): String {
    try {
        // Simple date formatting without using datetime library
        val totalSeconds = timestamp / 1000
        val hours = (totalSeconds / 3600) % 24
        val minutes = (totalSeconds / 60) % 60
        val seconds = totalSeconds % 60
        
        return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } catch (e: Exception) {
        // Fallback formatting if conversion fails
        return "soon"
    }
}