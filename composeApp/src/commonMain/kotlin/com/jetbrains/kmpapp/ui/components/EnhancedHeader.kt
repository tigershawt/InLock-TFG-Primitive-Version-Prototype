package com.jetbrains.kmpapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.ui.icons.AppIcons
import com.jetbrains.kmpapp.ui.icons.PlatformIcon
import com.jetbrains.kmpapp.ui.theme.InLockPrimary
import com.jetbrains.kmpapp.ui.theme.InLockPrimaryVariant

@Composable
fun EnhancedHeader(
    title: String,
    subtitle: String? = null,
    height: Int = 170,
    isCollapsed: Boolean = false,
    icon: PlatformIcon? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val headerHeight = if (isCollapsed) 80.dp else height.dp
    var isVisible by remember { mutableStateOf(false) }
    
    
    val titleOffsetAnim by animateFloatAsState(
        targetValue = if (isVisible) 0f else 30f,
        animationSpec = tween(500, easing = EaseOutQuad)
    )
    
    
    val subtitleAlphaAnim by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(800, easing = EaseInOut)
    )
    
    
    LaunchedEffect(Unit) {
        isVisible = true
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
        
        
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, end = 8.dp)
                    .align(Alignment.TopEnd)
            ) {
                actions()
            }
            
            
            if (isCollapsed) {
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (icon != null) {
                        com.jetbrains.kmpapp.ui.icons.AppIcon(
                            icon = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    
                    Text(
                        text = title,
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .offset(y = titleOffsetAnim.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (icon != null) {
                            com.jetbrains.kmpapp.ui.icons.AppIcon(
                                icon = icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        
                        Text(
                            text = title,
                            style = MaterialTheme.typography.h5,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.subtitle1,
                            color = Color.White.copy(alpha = subtitleAlphaAnim),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedCollapsibleHeader(
    title: String,
    subtitle: String? = null,
    isCollapsed: Boolean,
    icon: PlatformIcon? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    
    val headerHeight by animateDpAsState(
        targetValue = if (isCollapsed) 80.dp else 170.dp,
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    )
    
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
        
        
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, end = 8.dp)
                    .align(Alignment.TopEnd)
            ) {
                actions()
            }
            
            
            if (isCollapsed) {
                
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { -40 },
                    exit = fadeOut() + slideOutVertically { -40 },
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (icon != null) {
                            com.jetbrains.kmpapp.ui.icons.AppIcon(
                                icon = icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        
                        Text(
                            text = title,
                            style = MaterialTheme.typography.h6,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            } else {
                
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { 40 },
                    exit = fadeOut() + slideOutVertically { 40 },
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (icon != null) {
                                com.jetbrains.kmpapp.ui.icons.AppIcon(
                                    icon = icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            
                            Text(
                                text = title,
                                style = MaterialTheme.typography.h5,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        if (subtitle != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.subtitle1,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}