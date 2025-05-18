package com.jetbrains.kmpapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.ui.icons.AppIcon
import com.jetbrains.kmpapp.ui.icons.PlatformIcon
import com.jetbrains.kmpapp.ui.theme.InLockBlue

data class ButtonAction(
    val text: String,
    val onClick: () -> Unit,
    val icon: PlatformIcon? = null,
    val isPrimary: Boolean = true,
    val isDanger: Boolean = false,
    val isEnabled: Boolean = true
)

@Composable
fun ActionButtonRow(
    primaryAction: ButtonAction,
    secondaryAction: ButtonAction? = null,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(16.dp),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically
    ) {
        secondaryAction?.let { action ->
            if (action.isPrimary) {
                PrimaryButton(action, Modifier.weight(1f))
            } else {
                SecondaryButton(action, Modifier.weight(1f))
            }
        }
        
        if (primaryAction.isPrimary) {
            PrimaryButton(primaryAction, Modifier.weight(1f))
        } else {
            SecondaryButton(primaryAction, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PrimaryButton(
    action: ButtonAction,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        !action.isEnabled -> Color.Gray
        action.isDanger -> Color.Red
        else -> InLockBlue
    }
    
    Button(
        onClick = action.onClick,
        enabled = action.isEnabled,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor
        ),
        modifier = modifier
    ) {
        action.icon?.let { icon ->
            AppIcon(
                icon = icon,
                contentDescription = null,
                tint = Color.White
            )
            
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Text(
            text = action.text,
            color = Color.White
        )
    }
}

@Composable
private fun SecondaryButton(
    action: ButtonAction,
    modifier: Modifier = Modifier
) {
    val contentColor = when {
        !action.isEnabled -> Color.Gray
        action.isDanger -> Color.Red
        else -> InLockBlue
    }
    
    OutlinedButton(
        onClick = action.onClick,
        enabled = action.isEnabled,
        modifier = modifier
    ) {
        action.icon?.let { icon ->
            AppIcon(
                icon = icon,
                contentDescription = null,
                tint = contentColor
            )
            
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Text(
            text = action.text,
            color = contentColor
        )
    }
}

@Composable
fun ConfirmCancelButtons(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    confirmText: String = "Confirm",
    cancelText: String = "Cancel",
    confirmIcon: PlatformIcon? = null,
    cancelIcon: PlatformIcon? = null,
    isConfirmEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    ActionButtonRow(
        primaryAction = ButtonAction(
            text = confirmText,
            onClick = onConfirm,
            icon = confirmIcon,
            isEnabled = isConfirmEnabled
        ),
        secondaryAction = ButtonAction(
            text = cancelText,
            onClick = onCancel,
            icon = cancelIcon,
            isPrimary = false
        ),
        modifier = modifier
    )
}