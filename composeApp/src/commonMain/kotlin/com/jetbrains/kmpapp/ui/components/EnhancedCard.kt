package com.jetbrains.kmpapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.ui.theme.*

@Composable
fun EnhancedCard(
    modifier: Modifier = Modifier,
    elevation: Dp = InLockCardElevation,
    cornerRadius: Dp = InLockCardCornerRadius,
    backgroundColor: Color = InLockSurface,
    contentColor: Color = MaterialTheme.colors.onSurface,
    border: BorderStroke? = null,
    gradientColors: List<Color>? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1f,
        animationSpec = tween(150)
    )
    
    
    val animatedElevation by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) (elevation.value * 0.8f) else elevation.value,
        animationSpec = tween(150)
    )
    
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null, 
            onClick = onClick
        )
    } else {
        Modifier
    }
    
    Card(
        modifier = modifier
            .scale(scale)
            .then(clickModifier),
        shape = RoundedCornerShape(cornerRadius),
        backgroundColor = if (gradientColors != null) Color.Transparent else backgroundColor,
        contentColor = contentColor,
        elevation = animatedElevation.dp,
        border = border
    ) {
        if (gradientColors != null) {
            
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.linearGradient(gradientColors),
                        shape = RoundedCornerShape(cornerRadius)
                    )
                    .clip(RoundedCornerShape(cornerRadius))
            ) {
                content()
            }
        } else {
            content()
        }
    }
}

@Composable
fun EnhancedListItemCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    EnhancedCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        elevation = 2.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.h6,
                color = InLockTextPrimary
            )
            
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.subtitle2,
                    color = InLockTextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            content()
        }
    }
}

@Composable
fun EnhancedGradientCard(
    modifier: Modifier = Modifier,
    gradientStart: Color = InLockPrimary.copy(alpha = 0.8f),
    gradientEnd: Color = InLockSecondary,
    cornerRadius: Dp = InLockCardCornerRadius,
    elevation: Dp = InLockCardElevation,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    EnhancedCard(
        modifier = modifier,
        cornerRadius = cornerRadius,
        elevation = elevation,
        gradientColors = listOf(gradientStart, gradientEnd),
        onClick = onClick
    ) {
        content()
    }
}