package com.jetbrains.kmpapp.screens.manufacturer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.jetbrains.kmpapp.ui.icons.AppIcons
import com.jetbrains.kmpapp.ui.icons.AppIcon
import com.jetbrains.kmpapp.ui.theme.InLockBlue
import com.jetbrains.kmpapp.ui.theme.InLockTheme

class ProductRegistrationScreen : Screen {
    @Composable
    override fun Content() {
        InLockTheme {
            val navigator = LocalNavigator.currentOrThrow
            val screenModel = getScreenModel<ProductRegistrationScreenModel>()
            val uiState by screenModel.uiState.collectAsState()
            val focusManager = LocalFocusManager.current
            val scrollState = rememberScrollState()

            
            var productName by remember { mutableStateOf("") }
            var productDescription by remember { mutableStateOf("") }
            var productCategory by remember { mutableStateOf("") }

            
            var propertyKey by remember { mutableStateOf("") }
            var propertyValue by remember { mutableStateOf("") }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Create Product Template") },
                        navigationIcon = {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    if (!uiState.isManufacturer) {
                        
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Access Denied",
                                style = MaterialTheme.typography.h5,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "You do not have manufacturer privileges.",
                                style = MaterialTheme.typography.body1
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = { navigator.pop() },
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = InLockBlue
                                )
                            ) {
                                Text("Go Back")
                            }
                        }
                    } else {
                        
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(scrollState)
                        ) {
                            
                            AnimatedVisibility(visible = uiState.error.isNotEmpty()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.1f),
                                    elevation = 0.dp
                                ) {
                                    Text(
                                        uiState.error,
                                        modifier = Modifier.padding(16.dp),
                                        color = MaterialTheme.colors.error
                                    )
                                }
                            }

                            AnimatedVisibility(visible = uiState.success.isNotEmpty()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    backgroundColor = Color(0xFF4CAF50).copy(alpha = 0.1f),
                                    elevation = 0.dp
                                ) {
                                    Text(
                                        uiState.success,
                                        modifier = Modifier.padding(16.dp),
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                            }

                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                elevation = 4.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        "Template Information",
                                        style = MaterialTheme.typography.h6,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    
                                    OutlinedTextField(
                                        value = productName,
                                        onValueChange = { productName = it },
                                        label = { Text("Product Name*") },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        keyboardActions = KeyboardActions(
                                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                        ),
                                        singleLine = true,
                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                            focusedBorderColor = InLockBlue,
                                            cursorColor = InLockBlue
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    
                                    OutlinedTextField(
                                        value = productDescription,
                                        onValueChange = { productDescription = it },
                                        label = { Text("Product Description*") },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        keyboardActions = KeyboardActions(
                                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                        ),
                                        maxLines = 5,
                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                            focusedBorderColor = InLockBlue,
                                            cursorColor = InLockBlue
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    
                                    OutlinedTextField(
                                        value = productCategory,
                                        onValueChange = { productCategory = it },
                                        label = { Text("Product Category*") },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(
                                            onDone = { focusManager.clearFocus() }
                                        ),
                                        singleLine = true,
                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                            focusedBorderColor = InLockBlue,
                                            cursorColor = InLockBlue
                                        )
                                    )
                                }
                            }

                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                elevation = 4.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        "Additional Properties",
                                        style = MaterialTheme.typography.h6,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = propertyKey,
                                            onValueChange = { propertyKey = it },
                                            label = { Text("Property Name") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                                focusedBorderColor = InLockBlue,
                                                cursorColor = InLockBlue
                                            )
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        OutlinedTextField(
                                            value = propertyValue,
                                            onValueChange = { propertyValue = it },
                                            label = { Text("Value") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                                focusedBorderColor = InLockBlue,
                                                cursorColor = InLockBlue
                                            )
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        IconButton(
                                            onClick = {
                                                if (propertyKey.isNotEmpty() && propertyValue.isNotEmpty()) {
                                                    screenModel.updateProductProperty(propertyKey, propertyValue)
                                                    propertyKey = ""
                                                    propertyValue = ""
                                                }
                                            },
                                            modifier = Modifier
                                                .background(
                                                    InLockBlue,
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .size(48.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Add Property",
                                                tint = Color.White
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    
                                    if (uiState.productProperties.isNotEmpty()) {
                                        Text(
                                            "Added Properties:",
                                            fontWeight = FontWeight.Bold
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        uiState.productProperties.forEach { (key, value) ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("$key: $value")

                                                IconButton(
                                                    onClick = {
                                                        val updatedProperties = uiState.productProperties.toMutableMap()
                                                        updatedProperties.remove(key)
                                                        screenModel.updateProductProperty(key, "")
                                                    }
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Remove Property",
                                                        tint = Color.Red
                                                    )
                                                }
                                            }

                                            Divider()
                                        }
                                    }
                                }
                            }

                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                elevation = 4.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Product Image",
                                        style = MaterialTheme.typography.h6,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                            .border(
                                                1.dp,
                                                Color.Gray.copy(alpha = 0.5f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (uiState.imageBytes != null) {
                                            Text(
                                                "Image Selected (${uiState.imageBytes!!.size / 1024} KB)",
                                                color = InLockBlue
                                            )
                                        } else {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                AppIcon(
                                                    icon = AppIcons.AddPhotoAlternate,
                                                    contentDescription = "Add Image",
                                                    modifier = Modifier.size(48.dp),
                                                    tint = Color.Gray
                                                )

                                                Text(
                                                    "Select Image",
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        "Note: Image upload is not available in the prototype",
                                        style = MaterialTheme.typography.caption,
                                        color = Color.Gray
                                    )
                                }
                            }

                            
                            Button(
                                onClick = {
                                    screenModel.createTemplate(
                                        name = productName,
                                        description = productDescription,
                                        category = productCategory
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                enabled = !uiState.isLoading &&
                                        productName.isNotEmpty() &&
                                        productDescription.isNotEmpty() &&
                                        productCategory.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = InLockBlue,
                                    disabledBackgroundColor = InLockBlue.copy(alpha = 0.5f)
                                )
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Text(
                                        "Create Template",
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
}