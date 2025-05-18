package com.jetbrains.kmpapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetbrains.kmpapp.ui.icons.AppIcon
import com.jetbrains.kmpapp.ui.icons.AppIcons
import com.jetbrains.kmpapp.ui.icons.PlatformIcon
import com.jetbrains.kmpapp.ui.theme.*

enum class StatusBadgeStyle {
    SUBTLE,      
    OUTLINED,    
    SOLID,       
    GRADIENT     
}

@Composable
fun StatusBadge(
    text: String,
    icon: PlatformIcon? = null,
    color: Color = InLockPrimary,
    style: StatusBadgeStyle = StatusBadgeStyle.SUBTLE,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    
    
    val (backgroundColor, textColor, borderColor) = when (style) {
        StatusBadgeStyle.SUBTLE -> Triple(
            color.copy(alpha = 0.1f),
            color,
            Color.Transparent
        )
        StatusBadgeStyle.OUTLINED -> Triple(
            Color.Transparent,
            color,
            color.copy(alpha = 0.5f)
        )
        StatusBadgeStyle.SOLID -> Triple(
            color,
            Color.White,
            Color.Transparent
        )
        StatusBadgeStyle.GRADIENT -> Triple(
            Color.Transparent, 
            Color.White,
            Color.Transparent
        )
    }
    
    val baseModifier = Modifier
        .clip(shape)
        .then(
            if (style == StatusBadgeStyle.GRADIENT) {
                Modifier.background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            color,
                            color.copy(alpha = 0.8f)
                        )
                    ),
                    shape = shape
                )
            } else {
                Modifier.background(backgroundColor, shape)
            }
        )
        .then(
            if (style == StatusBadgeStyle.OUTLINED) {
                Modifier.border(
                    width = 1.dp,
                    color = borderColor,
                    shape = shape
                )
            } else {
                Modifier
            }
        )
        .padding(horizontal = 12.dp, vertical = 6.dp)
        .then(modifier)
    
    Row(
        modifier = baseModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        icon?.let {
            AppIcon(
                icon = it,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier
                    .size(14.dp)
                    .padding(end = 4.dp)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
        }
        
        Text(
            text = text,
            style = MaterialTheme.typography.caption.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.25.sp
            ),
            color = textColor
        )
    }
}

@Composable
fun VerifiedBadge(
    verified: Boolean = true,
    style: StatusBadgeStyle = StatusBadgeStyle.SUBTLE,
    modifier: Modifier = Modifier
) {
    val (text, icon, color) = if (verified) {
        Triple(
            "Verified",
            AppIcons.VerifiedUser,
            InLockSuccess
        )
    } else {
        Triple(
            "Unverified",
            AppIcons.ErrorOutline,
            InLockError
        )
    }
    
    StatusBadge(
        text = text,
        icon = icon,
        color = color,
        style = style,
        modifier = modifier
    )
}

@Composable
fun BlockchainBadge(
    registered: Boolean = true,
    style: StatusBadgeStyle = StatusBadgeStyle.SUBTLE,
    modifier: Modifier = Modifier
) {
    val (text, icon, color) = if (registered) {
        Triple(
            "Blockchain",
            AppIcons.Blockchain,
            InLockPrimary
        )
    } else {
        Triple(
            "Not Registered",
            AppIcons.ErrorOutline,
            InLockError
        )
    }
    
    StatusBadge(
        text = text,
        icon = icon,
        color = color,
        style = style,
        modifier = modifier
    )
}

@Composable
fun CategoryBadge(
    category: String,
    style: StatusBadgeStyle = StatusBadgeStyle.SUBTLE,
    modifier: Modifier = Modifier
) {
    StatusBadge(
        text = category,
        icon = AppIcons.Category,
        color = InLockAccent,
        style = style,
        modifier = modifier
    )
}

@Composable
fun AssetStatusBadgeRow(
    isVerified: Boolean,
    isRegisteredOnBlockchain: Boolean,
    category: String? = null,
    style: StatusBadgeStyle = StatusBadgeStyle.SUBTLE,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VerifiedBadge(
            verified = isVerified,
            style = style
        )
        
        if (isRegisteredOnBlockchain) {
            BlockchainBadge(
                registered = true,
                style = style
            )
        }
        
        if (!category.isNullOrBlank()) {
            CategoryBadge(
                category = category,
                style = style
            )
        }
    }
}