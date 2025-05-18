package com.jetbrains.kmpapp.ui.icons

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color


actual typealias PlatformIcon = String

actual object AppIcons {

    actual val Nfc: PlatformIcon = "nfc"
    actual val Inventory: PlatformIcon = "inventory"
    actual val AdminPanelSettings: PlatformIcon = "settings"
    actual val BrokenImage: PlatformIcon = "broken_image"
    actual val Category: PlatformIcon = "category"
    actual val QrCode: PlatformIcon = "qr_code"
    actual val VerifiedUser: PlatformIcon = "verified"
    actual val ErrorOutline: PlatformIcon = "error"
    actual val AddPhotoAlternate: PlatformIcon = "add_photo"
    actual val CheckCircle: PlatformIcon = "check_circle"
    actual val ChevronRight: PlatformIcon = "chevron_right"
    actual val Refresh: PlatformIcon = "refresh"
    actual val Blockchain: PlatformIcon = "blockchain"
    actual val Business: PlatformIcon = "business"
    actual val Whatshot: PlatformIcon = "whatshot"
    actual val Warning: PlatformIcon = "warning"
}

@Composable
actual fun AppIcon(
    icon: PlatformIcon,
    contentDescription: String?,
    modifier: Modifier,
    tint: Color
) {
    
    
    Text(
        text = icon,
        color = tint,
        modifier = modifier
    )
}