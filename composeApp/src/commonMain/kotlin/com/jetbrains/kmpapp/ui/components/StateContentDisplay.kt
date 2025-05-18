package com.jetbrains.kmpapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.ui.icons.AppIcon
import com.jetbrains.kmpapp.ui.icons.AppIcons
import com.jetbrains.kmpapp.ui.icons.PlatformIcon
import com.jetbrains.kmpapp.ui.theme.InLockBlue

@Composable
fun StateContentDisplay(
    type: StateDisplayType,
    title: String,
    message: String? = null,
    icon: PlatformIcon? = null,
    primaryAction: ButtonAction? = null,
    secondaryAction: ButtonAction? = null,
    modifier: Modifier = Modifier
) {
    val defaultIcon = when (type) {
        StateDisplayType.LOADING -> null
        StateDisplayType.EMPTY -> AppIcons.Inventory
        StateDisplayType.ERROR -> AppIcons.ErrorOutline
        StateDisplayType.INFO -> AppIcons.AdminPanelSettings
    }
    
    val iconTint = when (type) {
        StateDisplayType.LOADING -> InLockBlue
        StateDisplayType.EMPTY -> Color.Gray.copy(alpha = 0.5f)
        StateDisplayType.ERROR -> Color.Red.copy(alpha = 0.7f)
        StateDisplayType.INFO -> InLockBlue.copy(alpha = 0.7f)
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (type == StateDisplayType.LOADING) {
                CircularProgressIndicator(color = InLockBlue)
                
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                (icon ?: defaultIcon)?.let {
                    AppIcon(
                        icon = it,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(64.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            Text(
                text = title,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            if (message != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.body1,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
            
            if (primaryAction != null || secondaryAction != null) {
                Spacer(modifier = Modifier.height(24.dp))
                
                if (primaryAction != null && secondaryAction != null) {
                    ActionButtonRow(
                        primaryAction = primaryAction,
                        secondaryAction = secondaryAction,
                        modifier = Modifier.fillMaxWidth(0.7f)
                    )
                } else if (primaryAction != null) {
                    Button(
                        onClick = primaryAction.onClick,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = InLockBlue
                        ),
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        primaryAction.icon?.let { icon ->
                            AppIcon(
                                icon = icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        Text(
                            text = primaryAction.text,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

enum class StateDisplayType {
    LOADING,
    EMPTY,
    ERROR,
    INFO
}

@Composable
fun LoadingStateContent(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    StateContentDisplay(
        type = StateDisplayType.LOADING,
        title = message,
        modifier = modifier.fillMaxSize()
    )
}

@Composable
fun EmptyStateContent(
    title: String,
    message: String? = null,
    actionText: String? = null,
    icon: PlatformIcon? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    StateContentDisplay(
        type = StateDisplayType.EMPTY,
        title = title,
        message = message,
        icon = icon,
        primaryAction = if (actionText != null && onAction != null) {
            ButtonAction(
                text = actionText,
                onClick = onAction
            )
        } else null,
        modifier = modifier.fillMaxSize()
    )
}

@Composable
fun ErrorStateContent(
    title: String = "Error",
    errorMessage: String,
    retryText: String? = "Retry",
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    StateContentDisplay(
        type = StateDisplayType.ERROR,
        title = title,
        message = errorMessage,
        primaryAction = if (retryText != null && onRetry != null) {
            ButtonAction(
                text = retryText,
                onClick = onRetry
            )
        } else null,
        modifier = modifier.fillMaxSize()
    )
}