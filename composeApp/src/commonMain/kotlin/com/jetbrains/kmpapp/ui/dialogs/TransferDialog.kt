
package com.jetbrains.kmpapp.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.jetbrains.kmpapp.ui.theme.InLockBlue
import com.jetbrains.kmpapp.ui.theme.InLockTheme

@Composable
fun TransferDialog(
    assetName: String,
    assetId: String,
    onDismiss: () -> Unit,
    onTransfer: (recipientId: String) -> Unit
) {
    var recipientId by remember { mutableStateOf("") }
    var confirmTransfer by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                
                Text(
                    text = "Transfer Asset",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = InLockBlue
                )

                Spacer(modifier = Modifier.height(8.dp))

                
                Text(
                    text = assetName,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "ID: $assetId",
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (!confirmTransfer) {
                    
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Enter Recipient User ID",
                            style = MaterialTheme.typography.body1
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = recipientId,
                            onValueChange = {
                                recipientId = it
                                isError = false
                            },
                            label = { Text("Recipient User ID") },
                            isError = isError,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = InLockBlue,
                                unfocusedBorderColor = Color.Gray
                            )
                        )

                        if (isError) {
                            Text(
                                text = "Please enter a valid user ID",
                                color = MaterialTheme.colors.error,
                                style = MaterialTheme.typography.caption,
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = onDismiss,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Color.Gray
                                )
                            ) {
                                Text("Cancel")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    if (recipientId.isBlank()) {
                                        isError = true
                                    } else {
                                        confirmTransfer = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = InLockBlue
                                )
                            ) {
                                Text(
                                    text = "Next",
                                    color = Color.White
                                )
                            }
                        }
                    }
                } else {
                    
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFF3E0))
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Confirmation Required",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "You are about to transfer ownership of this asset. This action cannot be undone.",
                                    color = Color(0xFFE65100)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        
                        Text(
                            text = "Transfer Details",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Recipient: $recipientId",
                            style = MaterialTheme.typography.body2
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { confirmTransfer = false },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Color.Gray
                                )
                            ) {
                                Text("Back")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { onTransfer(recipientId) },
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFFE65100)
                                )
                            ) {
                                Text(
                                    text = "Confirm Transfer",
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}