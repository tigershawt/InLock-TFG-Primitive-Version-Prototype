package com.jetbrains.kmpapp.ui.icons

import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

actual typealias PlatformIcon = ImageVector

object AndroidIcons {
    fun init() {
    }
}

actual object AppIcons {
    actual val Nfc: PlatformIcon = Icons.Default.RadioButtonChecked
    actual val Inventory: PlatformIcon = Icons.Default.List
    actual val AdminPanelSettings: PlatformIcon = Icons.Default.Settings
    actual val BrokenImage: PlatformIcon = Icons.Default.BrokenImage
    actual val Category: PlatformIcon = Icons.Default.Inbox
    actual val QrCode: PlatformIcon = Icons.Default.QrCode2
    actual val VerifiedUser: PlatformIcon = Icons.Default.Verified
    actual val ErrorOutline: PlatformIcon = Icons.Default.Error
    actual val AddPhotoAlternate: PlatformIcon = Icons.Default.AddAPhoto
    actual val CheckCircle: PlatformIcon = Icons.Default.CheckCircle
    actual val ChevronRight: PlatformIcon = Icons.Default.ChevronRight
    actual val Refresh: PlatformIcon = Icons.Default.Refresh
    actual val Blockchain: PlatformIcon = Icons.Default.Language
    actual val Business: PlatformIcon = Icons.Default.Business
    actual val Whatshot: PlatformIcon = Icons.Default.LocalFireDepartment
    actual val Warning: PlatformIcon = Icons.Default.Warning
}

@Composable
actual fun AppIcon(
    icon: PlatformIcon,
    contentDescription: String?,
    modifier: Modifier,
    tint: Color
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}