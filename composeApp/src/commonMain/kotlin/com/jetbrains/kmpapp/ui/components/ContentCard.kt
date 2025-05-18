package com.jetbrains.kmpapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.ui.theme.InLockBlue

@Composable
fun ContentCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    cornerRadius: Int = 16,
    elevation: Int = 4,
    titleColor: Color = InLockBlue,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        elevation = elevation.dp,
        shape = RoundedCornerShape(cornerRadius.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    modifier = Modifier.padding(16.dp)
                )
                
                Divider()
            }
            
            Box(modifier = Modifier.padding(16.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    content = content
                )
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 16,
    elevation: Int = 4,
    titleColor: Color = InLockBlue,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    ContentCard(
        title = title,
        modifier = modifier,
        cornerRadius = cornerRadius,
        elevation = elevation,
        titleColor = titleColor,
        onClick = onClick,
        content = content
    )
}