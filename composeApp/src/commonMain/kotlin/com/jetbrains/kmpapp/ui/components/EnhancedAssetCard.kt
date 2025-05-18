package com.jetbrains.kmpapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.jetbrains.kmpapp.data.Product
import com.jetbrains.kmpapp.ui.icons.AppIcon
import com.jetbrains.kmpapp.ui.icons.AppIcons
import com.jetbrains.kmpapp.ui.theme.*
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun EnhancedAssetCard(
    asset: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var imageLoaded by remember { mutableStateOf(false) }
    
    EnhancedCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        elevation = InLockCardElevation,
        cornerRadius = 16.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(Color(0xFFF5F7FA)),
                contentAlignment = Alignment.Center
            ) {
                if (asset.imageUrl.isNotEmpty()) {
                    KamelImage(
                        resource = asyncPainterResource(asset.imageUrl),
                        contentDescription = asset.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        onLoading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.material.CircularProgressIndicator(
                                    color = InLockPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            imageLoaded = true
                        },
                        onFailure = {
                            AppIcon(
                                icon = AppIcons.BrokenImage,
                                contentDescription = "Image failed to load",
                                tint = InLockTextTertiary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    )
                    
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.3f)
                                    ),
                                    startY = 0f,
                                    endY = Float.POSITIVE_INFINITY
                                )
                            )
                    )
                    
                    
                    if (asset.category.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = asset.category,
                                style = MaterialTheme.typography.caption,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        InLockPrimary.copy(alpha = 0.1f),
                                        InLockSecondary.copy(alpha = 0.1f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AppIcon(
                            icon = AppIcons.Category,
                            contentDescription = "Asset category",
                            tint = InLockTextTertiary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
            
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                
                Text(
                    text = asset.name,
                    style = MaterialTheme.typography.h5,
                    color = InLockTextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                
                if (asset.manufacturerName.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AppIcon(
                            icon = AppIcons.Business,
                            contentDescription = null,
                            tint = InLockTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        
                        Text(
                            text = asset.manufacturerName,
                            style = MaterialTheme.typography.caption,
                            color = InLockTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    
                    if (asset.isRegisteredOnBlockchain) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(InLockSuccess.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AppIcon(
                                icon = AppIcons.VerifiedUser,
                                contentDescription = "Blockchain Verified",
                                tint = InLockSuccess,
                                modifier = Modifier.size(12.dp)
                            )
                            
                            Text(
                                text = "Verified",
                                style = MaterialTheme.typography.caption,
                                fontWeight = FontWeight.Medium,
                                color = InLockSuccess
                            )
                        }
                    }
                    
                    
                }
            }
            
            
            Box(
                modifier = Modifier.padding(end = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                AppIcon(
                    icon = AppIcons.ChevronRight,
                    contentDescription = "View details",
                    tint = InLockTextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

