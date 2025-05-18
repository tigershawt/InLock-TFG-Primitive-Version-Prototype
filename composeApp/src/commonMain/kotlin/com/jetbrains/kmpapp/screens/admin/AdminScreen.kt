package com.jetbrains.kmpapp.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.jetbrains.kmpapp.data.UserData
import com.jetbrains.kmpapp.data.UserRole
import com.jetbrains.kmpapp.ui.theme.InLockBlue
import com.jetbrains.kmpapp.ui.theme.InLockTheme

class AdminScreen : Screen {
    @Composable
    override fun Content() {
        InLockTheme {
            val navigator = LocalNavigator.currentOrThrow
            val screenModel = getScreenModel<AdminScreenModel>()
            val uiState by screenModel.uiState.collectAsState()

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Admin Panel") },
                        navigationIcon = {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { screenModel.loadUsers() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    if (!uiState.isAdmin) {
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
                                "You do not have administrator privileges.",
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

                            Text(
                                "Manage Users",
                                style = MaterialTheme.typography.h6,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )

                            if (uiState.isLoading) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else if (uiState.users.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No users found")
                                }
                            } else {
                                LazyColumn {
                                    items(uiState.users) { user ->
                                        UserListItem(
                                            user = user,
                                            onRoleChanged = { newRole ->
                                                screenModel.updateUserRole(user.id, newRole)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserListItem(
    user: UserData,
    onRoleChanged: (UserRole) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "User",
                    tint = InLockBlue,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        user.displayName,
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        user.email,
                        style = MaterialTheme.typography.body2,
                        color = Color.Gray
                    )
                    Text(
                        "@${user.username}",
                        style = MaterialTheme.typography.caption
                    )
                }

                Box {
                    Button(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = when(user.role) {
                                UserRole.ADMIN -> Color(0xFF4CAF50)
                                UserRole.MANUFACTURER -> Color(0xFFFFA000)
                                UserRole.USER -> Color(0xFF2196F3)
                            }
                        ),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(user.role.name)
                        Icon(Icons.Default.ArrowDropDown, "Select role")
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        UserRole.values().forEach { role ->
                            DropdownMenuItem(
                                onClick = {
                                    onRoleChanged(role)
                                    expanded = false
                                }
                            ) {
                                Text(role.name)
                            }
                        }
                    }
                }
            }
        }
    }
}