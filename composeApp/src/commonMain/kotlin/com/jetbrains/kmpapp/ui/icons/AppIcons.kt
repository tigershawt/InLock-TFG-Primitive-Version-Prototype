package com.jetbrains.kmpapp.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

expect class PlatformIcon

expect object AppIcons {

    val Nfc: PlatformIcon
    val Inventory: PlatformIcon
    val AdminPanelSettings: PlatformIcon
    val BrokenImage: PlatformIcon
    val Category: PlatformIcon
    val QrCode: PlatformIcon
    val VerifiedUser: PlatformIcon
    val ErrorOutline: PlatformIcon
    val AddPhotoAlternate: PlatformIcon
    val CheckCircle: PlatformIcon
    val ChevronRight: PlatformIcon
    val Refresh: PlatformIcon
    val Blockchain: PlatformIcon
    val Business: PlatformIcon
    val Whatshot: PlatformIcon
    val Warning: PlatformIcon
}

@Composable
expect fun AppIcon(
    icon: PlatformIcon,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
)