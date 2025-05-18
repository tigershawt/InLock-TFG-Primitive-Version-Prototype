
package com.jetbrains.kmpapp.screens.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.jetbrains.kmpapp.data.UserRole
import com.jetbrains.kmpapp.screens.login.LoginScreen
import com.jetbrains.kmpapp.screens.main.EnhancedMainScreen
import com.jetbrains.kmpapp.screens.manufacturer.ManufacturerDashboardScreenCommon
import com.jetbrains.kmpapp.screens.admin.AdminScreen
import com.jetbrains.kmpapp.screens.nfc.NfcScreenCommon
import com.jetbrains.kmpapp.screens.assets.AssetsScreen
import com.jetbrains.kmpapp.ui.components.EnhancedBottomNavDestination
import com.jetbrains.kmpapp.ui.icons.AppIcon
import com.jetbrains.kmpapp.ui.icons.AppIcons
import com.jetbrains.kmpapp.ui.theme.*
import com.jetbrains.kmpapp.ui.components.EnhancedScreenTitle
import com.jetbrains.kmpapp.ui.components.EnhancedActionButton
import com.jetbrains.kmpapp.utils.SecurityUtils

class ProfileScreen : Screen {
    @Composable
    override fun Content() {
        InLockTheme {
            val screenModel = getScreenModel<ProfileScreenModel>()
            val uiState by screenModel.uiState.collectAsState()
            val navigator = LocalNavigator.currentOrThrow
            val scrollState = rememberScrollState()

            LaunchedEffect(Unit) {
                screenModel.loadUserProfile()
            }

            LaunchedEffect(uiState.isSignedOut) {
                if (uiState.isSignedOut) {
                    navigator.replaceAll(LoginScreen)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp) 
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        InLockPrimary,
                                        InLockPrimaryVariant,
                                        InLockSecondary
                                    )
                                )
                            )
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp) 
                            .padding(top = 80.dp) 
                            .align(Alignment.BottomCenter),
                        shape = RoundedCornerShape(24.dp), 
                        elevation = 8.dp,
                        backgroundColor = InLockSurface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 24.dp) 
                                .padding(top = 100.dp), 
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(color = InLockPrimary)
                            } else {
                                uiState.userData?.let { user ->
                                    Text(
                                        text = user.displayName,
                                        style = MaterialTheme.typography.h5,
                                        color = InLockTextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "@${user.username}",
                                        style = MaterialTheme.typography.subtitle1,
                                        color = InLockTextSecondary
                                    )
                                    val roleColor = when(uiState.userData?.role) {
                                        UserRole.ADMIN -> InLockSuccess
                                        UserRole.MANUFACTURER -> InLockAccent
                                        UserRole.USER -> InLockPrimary
                                        null -> InLockTextSecondary
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Card(
                                        shape = RoundedCornerShape(50),
                                        backgroundColor = roleColor.copy(alpha = 0.1f),
                                        elevation = 0.dp
                                    ) {
                                        Text(
                                            text = getRoleDisplayName(uiState.userData?.role),
                                            color = roleColor,
                                            style = MaterialTheme.typography.caption,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                    
                                    if (uiState.isGuest) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            backgroundColor = InLockSurface,
                                            elevation = 2.dp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = "Device Information",
                                                    style = MaterialTheme.typography.subtitle2,
                                                    fontWeight = FontWeight.Bold,
                                                    color = InLockBlue
                                                )
                                                
                                                Divider(
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                    color = InLockTextSecondary.copy(alpha = 0.2f)
                                                )
                                                
                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalAlignment = Alignment.Start
                                                ) {
                                                    Text(
                                                        text = "Device ID",
                                                        style = MaterialTheme.typography.caption,
                                                        color = InLockTextSecondary
                                                    )
                                                    
                                                    Text(
                                                        text = uiState.deviceId,
                                                        style = MaterialTheme.typography.body2,
                                                        fontWeight = FontWeight.Medium,
                                                        color = InLockTextPrimary
                                                    )
                                                    
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    
                                                    Text(
                                                        text = "User ID",
                                                        style = MaterialTheme.typography.caption,
                                                        color = InLockTextSecondary
                                                    )
                                                    
                                                    Text(
                                                        text = uiState.userId,
                                                        style = MaterialTheme.typography.body2,
                                                        fontWeight = FontWeight.Medium,
                                                        color = InLockTextPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(120.dp) 
                            .align(Alignment.TopCenter)
                            .offset(y = 100.dp) 
                            .clip(CircleShape)
                            .background(
                                color = InLockSurface,
                                shape = CircleShape
                            )
                            .border(
                                width = 5.dp, 
                                brush = Brush.linearGradient(
                                    colors = listOf(InLockPrimary, InLockSecondary)
                                ),
                                shape = CircleShape
                            )
                            .padding(5.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        InLockPrimary.copy(alpha = 0.1f),
                                        InLockSurface
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AppIcon(
                            icon = AppIcons.AdminPanelSettings,
                            contentDescription = "Profile",
                            tint = InLockPrimary,
                            modifier = Modifier.size(64.dp) 
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = InLockBlue)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = 4.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Account Information",
                                    style = MaterialTheme.typography.h6,
                                    fontWeight = FontWeight.Bold,
                                    color = InLockBlue
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                
                                ProfileInfoItem(
                                    icon = AppIcons.Category,
                                    label = "Email",
                                    value = uiState.userData?.email ?: "Not available"
                                )

                                Divider(modifier = Modifier.padding(vertical = 8.dp))

                                
                                val roleColor = when(uiState.userData?.role) {
                                    UserRole.ADMIN -> Color(0xFF4CAF50)
                                    UserRole.MANUFACTURER -> Color(0xFFFFA000)
                                    UserRole.USER -> InLockBlue
                                    null -> InLockTextSecondary
                                }

                                ProfileInfoItem(
                                    icon = AppIcons.VerifiedUser,
                                    label = "Role",
                                    value = getRoleDisplayName(uiState.userData?.role),
                                    valueColor = roleColor
                                )

                                Divider(modifier = Modifier.padding(vertical = 8.dp))

                                if (uiState.isGuest) {
                                    ProfileInfoItem(
                                        icon = AppIcons.Inventory,
                                        label = "Device ID",
                                        value = uiState.deviceId,
                                        valueColor = InLockPrimary
                                    )
                                    
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                    
                                    ProfileInfoItem(
                                        icon = AppIcons.AdminPanelSettings,
                                        label = "User ID",
                                        value = uiState.userId,
                                        valueColor = InLockPrimary
                                    )
                                    
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                }
                                
                                ProfileInfoItem(
                                    icon = AppIcons.Nfc,
                                    label = "Token Balance",
                                    value = uiState.tokenBalance.toString()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        
                        Text(
                            text = "Actions",
                            style = MaterialTheme.typography.h6,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp),
                            color = InLockTextPrimary
                        )

                        
                        ActionButton(
                            icon = AppIcons.Inventory,
                            text = "My Assets",
                            description = "View and manage your registered assets",
                            onClick = { 
                                
                                EnhancedMainScreen.currentDestination.value = EnhancedBottomNavDestination.ASSETS
                                
                                navigator.push(AssetsScreen())
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        
                        if (uiState.userData?.role == UserRole.MANUFACTURER ||
                            uiState.userData?.role == UserRole.ADMIN) {
                            ActionButton(
                                icon = AppIcons.Inventory,
                                text = "Manufacturer Dashboard",
                                description = "Manage your products and registrations",
                                onClick = {
                                    
                                    EnhancedMainScreen.currentDestination.value = EnhancedBottomNavDestination.MANUFACTURER
                                    
                                    
                                    navigator.push(ManufacturerDashboardScreenCommon())
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        
                        if (uiState.userData?.role == UserRole.ADMIN) {
                            ActionButton(
                                icon = AppIcons.AdminPanelSettings,
                                text = "Admin Panel",
                                description = "Manage users and system settings",
                                onClick = {
                                    
                                    EnhancedMainScreen.currentDestination.value = EnhancedBottomNavDestination.ADMIN
                                    
                                    
                                    navigator.push(AdminScreen())
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        ActionButton(
                            icon = AppIcons.Nfc,
                            text = "Scan NFC Tag",
                            description = "Scan and verify asset ownership",
                            onClick = {
                                
                                EnhancedMainScreen.currentDestination.value = EnhancedBottomNavDestination.SCANNER
                                
                                
                                navigator.push(NfcScreenCommon())
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        
                        Button(
                            onClick = { screenModel.signOut() },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = InLockError
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Sign Out",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    @Composable
    private fun ProfileInfoItem(
        icon: com.jetbrains.kmpapp.ui.icons.PlatformIcon,
        label: String,
        value: String,
        valueColor: Color = InLockTextPrimary
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(
                icon = icon,
                contentDescription = null,
                tint = InLockBlue,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.caption,
                    color = InLockTextSecondary
                )

                Text(
                    text = value,
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Medium,
                    color = valueColor
                )
            }
        }
    }

    @Composable
    private fun ActionButton(
        icon: com.jetbrains.kmpapp.ui.icons.PlatformIcon,
        text: String,
        description: String,
        onClick: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            elevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(InLockBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    AppIcon(
                        icon = icon,
                        contentDescription = null,
                        tint = InLockBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.Bold,
                        color = InLockTextPrimary
                    )

                    Text(
                        text = description,
                        style = MaterialTheme.typography.caption,
                        color = InLockTextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                AppIcon(
                    icon = AppIcons.Category, 
                    contentDescription = "Open",
                    tint = InLockTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    private fun getRoleDisplayName(role: UserRole?): String {
        return when (role) {
            UserRole.ADMIN -> "Administrator"
            UserRole.MANUFACTURER -> "Manufacturer"
            UserRole.USER -> "Regular User"
            null -> "Unknown"
        }
    }
}