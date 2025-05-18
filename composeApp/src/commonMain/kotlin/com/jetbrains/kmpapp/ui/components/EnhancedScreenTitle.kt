package com.jetbrains.kmpapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetbrains.kmpapp.ui.icons.AppIcon
import com.jetbrains.kmpapp.ui.icons.AppIcons
import com.jetbrains.kmpapp.ui.icons.PlatformIcon
import com.jetbrains.kmpapp.ui.theme.*


enum class ScreenTitleStyle {
    GRADIENT,  
    CARD       
}

@Composable
fun EnhancedScreenTitle(
    title: String,
    onBackClick: (() -> Unit)? = null,
    gradientColors: List<Color> = listOf(InLockPrimary, InLockPrimaryVariant),
    actionIcon: PlatformIcon? = null,
    onActionClick: (() -> Unit)? = null,
    subtitle: String? = null,
    style: ScreenTitleStyle = ScreenTitleStyle.GRADIENT
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    when (style) {
        ScreenTitleStyle.GRADIENT -> {
            GradientStyleTitle(
                title = title,
                subtitle = subtitle,
                onBackClick = onBackClick,
                actionIcon = actionIcon,
                onActionClick = onActionClick,
                gradientColors = gradientColors,
                visible = visible
            )
        }
        
        ScreenTitleStyle.CARD -> {
            CardStyleTitle(
                title = title,
                subtitle = subtitle,
                onBackClick = onBackClick,
                actionIcon = actionIcon,
                onActionClick = onActionClick,
                primaryColor = gradientColors.first(),
                visible = visible
            )
        }
    }
}

@Composable
private fun GradientStyleTitle(
    title: String,
    subtitle: String?,
    onBackClick: (() -> Unit)?,
    actionIcon: PlatformIcon?,
    onActionClick: (() -> Unit)?,
    gradientColors: List<Color>,
    visible: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (subtitle != null) 200.dp else 170.dp)
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(
                brush = Brush.linearGradient(gradientColors)
            )
    ) {
        
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            
            val lineColor = Color.White.copy(alpha = 0.05f)
            val lineWidth = 1.dp.toPx()
            val spacing = 20.dp.toPx()
            
            
            for (i in 0..size.width.toInt() step spacing.toInt()) {
                drawLine(
                    color = lineColor,
                    start = Offset(i.toFloat(), 0f),
                    end = Offset(0f, i.toFloat()),
                    strokeWidth = lineWidth
                )
            }
        }
        
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    AppIcon(
                        icon = AppIcons.ChevronRight,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(700)) +
                        slideInVertically(
                            animationSpec = tween(700),
                            initialOffsetY = { -40 }
                        )
            ) {
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.h5.copy(
                        fontWeight = FontWeight.ExtraBold, 
                        color = Color.White,
                        letterSpacing = 0.5.sp,
                        fontSize = 22.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }

            
            if (actionIcon != null && onActionClick != null) {
                IconButton(
                    onClick = onActionClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    AppIcon(
                        icon = actionIcon,
                        contentDescription = "Action",
                        tint = Color.White
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
        
        
        subtitle?.let {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(1000, delayMillis = 300)) +
                        slideInVertically(
                            animationSpec = tween(1000, delayMillis = 300),
                            initialOffsetY = { 40 }
                        ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                
                Card(
                    backgroundColor = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp),
                    elevation = 0.dp,
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.body2.copy(
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            letterSpacing = 0.25.sp,
                            fontWeight = FontWeight.SemiBold 
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CardStyleTitle(
    title: String,
    subtitle: String?,
    onBackClick: (() -> Unit)?,
    actionIcon: PlatformIcon?,
    onActionClick: (() -> Unit)?,
    primaryColor: Color,
    visible: Boolean
) {
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (subtitle != null) 200.dp else 170.dp)
    ) {
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (subtitle != null) 200.dp else 170.dp)
                .background(InLockBackground)
        )
        
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(top = 40.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = InLockSurface,
            elevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(700)) +
                            slideInVertically(
                                animationSpec = tween(700),
                                initialOffsetY = { -40 }
                            )
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.h5.copy(
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            fontSize = 22.sp,
                            letterSpacing = 0.5.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                
                subtitle?.let {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(1000, delayMillis = 300))
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.body2.copy(
                                color = InLockTextSecondary,
                                textAlign = TextAlign.Center,
                                letterSpacing = 0.25.sp
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 16.dp)
                        )
                    }
                }
            }
        }
        
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            
            if (onBackClick != null) {
                FloatingActionButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(48.dp),
                    backgroundColor = primaryColor,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    AppIcon(
                        icon = AppIcons.ChevronRight,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
            
            
            if (actionIcon != null && onActionClick != null) {
                FloatingActionButton(
                    onClick = onActionClick,
                    modifier = Modifier.size(48.dp),
                    backgroundColor = primaryColor,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    AppIcon(
                        icon = actionIcon,
                        contentDescription = "Action",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
    }
}

@Composable
fun EnhancedActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: PlatformIcon? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        backgroundColor = InLockPrimary,
        contentColor = Color.White
    ),
    elevation: ButtonElevation = ButtonDefaults.elevation(
        defaultElevation = InLockButtonElevation,
        pressedElevation = (InLockButtonElevation * 1.5f)
    )
) {
    Button(
        onClick = onClick,
        colors = colors,
        shape = RoundedCornerShape(InLockButtonCornerRadius),
        elevation = elevation,
        modifier = modifier.heightIn(min = 54.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                AppIcon(
                    icon = icon,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            Text(
                text = text,
                style = MaterialTheme.typography.button.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
fun EnhancedCancelButton(
    text: String = "Cancel",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(InLockButtonCornerRadius),
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = Color.Transparent,
            contentColor = InLockTextPrimary
        ),
        modifier = modifier.heightIn(min = 54.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.button.copy(
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}