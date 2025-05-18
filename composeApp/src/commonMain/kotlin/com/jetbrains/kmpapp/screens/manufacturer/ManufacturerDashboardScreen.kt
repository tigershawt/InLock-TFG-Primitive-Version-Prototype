package com.jetbrains.kmpapp.screens.manufacturer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.jetbrains.kmpapp.data.Product
import com.jetbrains.kmpapp.screens.main.EnhancedMainScreen
import com.jetbrains.kmpapp.ui.components.EnhancedBottomNavDestination
import com.jetbrains.kmpapp.ui.components.EnhancedCollapsibleHeader
import com.jetbrains.kmpapp.ui.icons.AppIcons
import com.jetbrains.kmpapp.ui.icons.AppIcon
import com.jetbrains.kmpapp.ui.theme.*
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

class ManufacturerDashboardScreen : Screen {
    @Composable
    override fun Content() {
        InLockTheme {
            val navigator = LocalNavigator.currentOrThrow
            val screenModel = getScreenModel<ManufacturerDashboardScreenModelBase>()
            val uiState by screenModel.uiState.collectAsState()
            val scope = rememberCoroutineScope()
            val scrollState = rememberScrollState()

            LaunchedEffect(Unit) {
                if (uiState.isFirstLoad) {
                    screenModel.loadProductTemplates()
                }
                
                EnhancedMainScreen.currentDestination.value = 
                    EnhancedBottomNavDestination.MANUFACTURER
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Product Templates") },
                        backgroundColor = InLockBlue,
                        contentColor = Color.White,
                        actions = {
                            IconButton(
                                onClick = {
                                    if (!uiState.isLoading) {
                                        screenModel.loadProductTemplates()
                                    }
                                }
                            ) {
                                AppIcon(
                                    icon = AppIcons.Refresh,
                                    contentDescription = "Refresh",
                                    tint = Color.White
                                )
                            }
                        }
                    )
                },
                floatingActionButton = {
                    if (uiState.isManufacturer) {
                        FloatingActionButton(
                            onClick = { navigator.push(ProductRegistrationScreen()) },
                            backgroundColor = InLockBlue
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Create New Template",
                                tint = Color.White
                            )
                        }
                    }
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
                        Column(modifier = Modifier.fillMaxSize()) {
                            AnimatedVisibility(visible = uiState.error.isNotEmpty()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
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
                                        .padding(16.dp),
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

                            when {
                                uiState.isLoading -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = InLockBlue)
                                    }
                                }
                                uiState.templates.isEmpty() -> {
                                    EmptyTemplatesView()
                                }
                                else -> {
                                    TemplatesList(
                                        templates = uiState.templates,
                                        onInstantiate = { templateId ->
                                            screenModel.startRfidScan(templateId)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.isRfidScanning) {
                        ScanningOverlay(
                            onCancel = { screenModel.stopRfidScan() }
                        )
                    }

                    if (uiState.scannedRfidId.isNotEmpty() && uiState.selectedTemplateId.isNotEmpty()) {
                        ConfirmationDialog(
                            rfidId = uiState.scannedRfidId,
                            templateId = uiState.selectedTemplateId,
                            onConfirm = {
                                screenModel.processProductInstantiation(uiState.selectedTemplateId, uiState.scannedRfidId)
                            },
                            onDismiss = {
                                screenModel.clearRfidScan()
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun EmptyTemplatesView() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppIcon(
                icon = AppIcons.Inventory,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Gray.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "No Product Templates",
                style = MaterialTheme.typography.h6,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Create product templates to instantiate with RFID tags",
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        }
    }

    @Composable
    private fun TemplatesList(
        templates: List<Product>,
        onInstantiate: (String) -> Unit
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            items(templates) { template ->
                TemplateItem(
                    template = template,
                    onInstantiate = { onInstantiate(template.id) }
                )
            }
        }
    }

    @Composable
    private fun TemplateItem(
        template: Product,
        onInstantiate: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (template.imageUrl.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        KamelImage(
                            resource = asyncPainterResource(template.imageUrl),
                            contentDescription = "Template image",
                            modifier = Modifier.fillMaxSize(),
                            onLoading = {
                                CircularProgressIndicator()
                            },
                            onFailure = {
                                AppIcon(
                                    icon = AppIcons.BrokenImage,
                                    contentDescription = "Failed to load image",
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.Gray
                                )
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    template.name,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    template.description,
                    style = MaterialTheme.typography.body1
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon(
                        icon = AppIcons.Category,
                        contentDescription = "Category",
                        tint = InLockBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        template.category,
                        style = MaterialTheme.typography.caption
                    )
                }

                if (template.properties.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Properties:",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Bold
                    )

                    Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
                        template.properties.forEach { (key, value) ->
                            Text(
                                "$key: $value",
                                style = MaterialTheme.typography.caption
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onInstantiate,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = InLockBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppIcon(
                        icon = AppIcons.Nfc,
                        contentDescription = "Create Product",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Product", color = Color.White)
                }
            }
        }
    }

    @Composable
    private fun ScanningOverlay(onCancel: () -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp),
                elevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Scan RFID Tag",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CircularProgressIndicator(color = InLockBlue)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Hold your device near the RFID tag to create the product",
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Red
                        )
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                }
            }
        }
    }

    @Composable
    private fun ConfirmationDialog(
        rfidId: String,
        templateId: String,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Confirm Product Creation") },
            text = {
                Column {
                    Text("Create product with the following details?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("RFID: $rfidId", fontWeight = FontWeight.Bold)
                    Text("This will register the product on the blockchain.")
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(backgroundColor = InLockBlue)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}