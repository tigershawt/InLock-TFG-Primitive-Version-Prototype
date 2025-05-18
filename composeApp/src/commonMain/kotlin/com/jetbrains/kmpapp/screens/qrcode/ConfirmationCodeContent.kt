package com.jetbrains.kmpapp.screens.qrcode

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.data.Product
import com.jetbrains.kmpapp.ui.icons.AppIcon
import com.jetbrains.kmpapp.ui.icons.AppIcons
import com.jetbrains.kmpapp.ui.theme.InLockBlue
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun ConfirmationCodeContent(
    product: Product,
    confirmationCode: String,
    userEnteredCode: String,
    onCodeChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        elevation = 4.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Verify Transfer Intent",  
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = InLockBlue
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Please confirm your intent to transfer:",
                style = MaterialTheme.typography.subtitle1,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    if (product.imageUrl.isNotEmpty()) {
                        KamelImage(
                            resource = asyncPainterResource(product.imageUrl),
                            contentDescription = product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            onLoading = {
                                CircularProgressIndicator(strokeWidth = 2.dp)
                            },
                            onFailure = {
                                AppIcon(
                                    icon = AppIcons.Inventory,
                                    contentDescription = "No image",
                                    tint = Color.Gray
                                )
                            }
                        )
                    } else {
                        AppIcon(
                            icon = AppIcons.Inventory,
                            contentDescription = "No image",
                            tint = Color.Gray
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        product.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.subtitle1
                    )
                    
                    Text(
                        product.category,
                        style = MaterialTheme.typography.caption,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5))
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Type this code to confirm:",
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        confirmationCode,
                        style = MaterialTheme.typography.h4,
                        fontWeight = FontWeight.Bold,
                        color = InLockBlue,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = userEnteredCode,
                onValueChange = onCodeChanged,
                label = { Text("Enter Confirmation Code") },
                keyboardOptions = KeyboardType.Text.let { KeyboardOptions(keyboardType = it) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = {
                        onCodeChanged(userEnteredCode)
                        onSubmit()
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = InLockBlue
                    ),
                    modifier = Modifier.weight(1f),
                    enabled = userEnteredCode.isNotEmpty()
                ) {
                    Text(
                        "Confirm",
                        color = Color.White
                    )
                }
            }
        }
    }
}