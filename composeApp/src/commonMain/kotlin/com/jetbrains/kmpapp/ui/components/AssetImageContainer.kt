package com.jetbrains.kmpapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.data.Product
import com.jetbrains.kmpapp.ui.icons.AppIcon
import com.jetbrains.kmpapp.ui.icons.AppIcons
import com.jetbrains.kmpapp.ui.theme.InLockBlue
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun AssetImageContainer(
    imageUrl: String,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 8,
    showBlockchainBadge: Boolean = false,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNotEmpty()) {
            KamelImage(
                resource = asyncPainterResource(imageUrl),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onLoading = {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = InLockBlue
                    )
                },
                onFailure = {
                    AppIcon(
                        icon = AppIcons.BrokenImage,
                        contentDescription = "Image not available",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            )
        } else {
            AppIcon(
                icon = AppIcons.Inventory,
                contentDescription = "No image",
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }

        if (showBlockchainBadge) {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopEnd)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50)),
                contentAlignment = Alignment.Center
            ) {
                AppIcon(
                    icon = AppIcons.VerifiedUser,
                    contentDescription = "Blockchain Verified",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ProductImageContainer(
    product: Product,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 8,
) {
    AssetImageContainer(
        imageUrl = product.imageUrl,
        contentDescription = product.name,
        modifier = modifier,
        cornerRadius = cornerRadius,
        showBlockchainBadge = product.isRegisteredOnBlockchain
    )
}