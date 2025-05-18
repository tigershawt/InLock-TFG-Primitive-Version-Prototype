package com.jetbrains.kmpapp.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.jetbrains.kmpapp.data.UserRole
import com.jetbrains.kmpapp.screens.admin.AdminScreen
import com.jetbrains.kmpapp.screens.login.LoginScreen
import com.jetbrains.kmpapp.screens.manufacturer.ManufacturerDashboardScreenCommon
import com.jetbrains.kmpapp.screens.nfc.NfcScreenCommon
import com.jetbrains.kmpapp.ui.icons.AppIcons
import com.jetbrains.kmpapp.ui.icons.AppIcon
import com.jetbrains.kmpapp.ui.theme.InLockBlue
import com.jetbrains.kmpapp.ui.theme.InLockTextPrimary
import com.jetbrains.kmpapp.ui.theme.InLockTheme

data object HomeScreen : Screen {
    @Composable
    override fun Content() {
        InLockTheme {
            val navigator = LocalNavigator.currentOrThrow
            val screenModel: HomeScreenModel = getScreenModel()
            val uiState by screenModel.uiState.collectAsState()

            LaunchedEffect(uiState.isSignedOut) {
                if (uiState.isSignedOut) {
                    navigator.replaceAll(LoginScreen)
                }
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("InLock") },
                        actions = {
                            IconButton(onClick = { screenModel.signOut() }) {
                                AppIcon(
                                    icon = AppIcons.Category,
                                    contentDescription = "Sign out",
                                    tint = Color.White
                                )
                            }
                        },
                        backgroundColor = InLockBlue,
                        contentColor = Color.White
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        "Welcome to InLock",
                        style = MaterialTheme.typography.h5.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (uiState.isAnonymous) {
                        Text(
                            "You are currently signed in as a guest",
                            style = MaterialTheme.typography.body1,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        Button(
                            onClick = { navigator.replaceAll(LoginScreen) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(backgroundColor = InLockBlue)
                        ) {
                            Text("Create Full Account", color = Color.White)
                        }
                    } else {
                        Text(
                            "You are signed in with your account",
                            style = MaterialTheme.typography.body1,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        uiState.userData?.let { user ->
                            Text(
                                "Name: ${user.displayName}",
                                style = MaterialTheme.typography.body1,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            Text(
                                "Email: ${user.email}",
                                style = MaterialTheme.typography.body1,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            Text(
                                "Username: ${user.username}",
                                style = MaterialTheme.typography.body1,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            val roleColor = when(user.role) {
                                UserRole.ADMIN -> Color(0xFF4CAF50)
                                UserRole.MANUFACTURER -> Color(0xFFFFA000)
                                UserRole.USER -> InLockBlue
                            }

                            Text(
                                "Role: ${user.role.name}",
                                style = MaterialTheme.typography.body1.copy(
                                    color = roleColor,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(bottom = 24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        "Available Actions",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = InLockTextPrimary
                    )

                    NavButton(
                        icon = AppIcons.Nfc,
                        text = "Scan NFC Tag",
                        onClick = { navigator.push(NfcScreenCommon()) }
                    )

                    if (uiState.userData?.role == UserRole.MANUFACTURER || uiState.userData?.role == UserRole.ADMIN) {
                        NavButton(
                            icon = AppIcons.Inventory,
                            text = "Manufacturer Dashboard",
                            onClick = { navigator.push(ManufacturerDashboardScreenCommon()) }
                        )
                    }

                    if (uiState.userData?.role == UserRole.ADMIN) {
                        NavButton(
                            icon = AppIcons.AdminPanelSettings,
                            text = "Admin Panel",
                            onClick = { navigator.push(AdminScreen()) }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun NavButton(
        icon: com.jetbrains.kmpapp.ui.icons.PlatformIcon,
        text: String,
        onClick: () -> Unit
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(vertical = 4.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = InLockBlue,
                contentColor = Color.White
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(
                    icon = icon,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}