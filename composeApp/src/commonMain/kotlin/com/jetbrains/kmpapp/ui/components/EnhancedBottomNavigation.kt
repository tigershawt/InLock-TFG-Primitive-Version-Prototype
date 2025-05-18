package com.jetbrains.kmpapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetbrains.kmpapp.data.FirebaseService
import com.jetbrains.kmpapp.data.UserRole
import com.jetbrains.kmpapp.screens.main.EnhancedMainScreen
import com.jetbrains.kmpapp.ui.icons.AppIcon
import com.jetbrains.kmpapp.ui.icons.AppIcons
import com.jetbrains.kmpapp.ui.theme.*
import com.jetbrains.kmpapp.utils.SecurityUtils
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.jetbrains.kmpapp.utils.getCurrentTimeMillis

enum class EnhancedBottomNavDestination(val title: String) {
    ASSETS("Assets"),
    SCANNER("Scan"),
    PROFILE("Profile"),
    TRANSFER("Transfer"),
    ADMIN("Admin"),
    MANUFACTURER("Manufacture")
}

val screensWithoutNavigation = mutableListOf(
    "LoginScreen",
    "SplashScreen"
)
fun hideBottomNav() {
    EnhancedMainScreen.shouldClearBottomNav = true
}

@Composable
fun EnhancedBottomNavigation(
    currentDestination: EnhancedBottomNavDestination,
    onDestinationSelected: (EnhancedBottomNavDestination) -> Unit
) {
    val navigator = cafe.adriel.voyager.navigator.LocalNavigator.current
    val currentScreen = navigator?.lastItem
    
    if (EnhancedMainScreen.shouldClearBottomNav || 
        (currentScreen != null && 
         (screensWithoutNavigation.contains(currentScreen::class.simpleName)))) {
        return
    }
    
    val scope = rememberCoroutineScope()
    var currentUserRole by remember { mutableStateOf<UserRole?>(null) }
    val isManufacturer = remember { mutableStateOf(false) }
    val isAdmin = remember { mutableStateOf(false) }
    
    val componentId = remember { 
        "nav_${EnhancedMainScreen.uiRecreationKey}_${getCurrentTimeMillis()}" 
    }
    
    val forceRefresh = remember { mutableStateOf(0) }
    
    val firebaseService: FirebaseService = remember {
        object : KoinComponent {
            val service: FirebaseService by inject()
        }.service
    }
    
    LaunchedEffect(Unit) {
        if (EnhancedMainScreen.isPostLogout) {
            EnhancedMainScreen.clearPostLogoutFlag()
            forceRefresh.value = forceRefresh.value + 1
        }
    }
    
    LaunchedEffect(forceRefresh.value, EnhancedMainScreen.uiRecreationKey) {
        try {
            currentUserRole = SecurityUtils.getCurrentUserRole()
            isManufacturer.value = firebaseService.isManufacturer()
            isAdmin.value = firebaseService.isAdmin()
            scope.launch {
                val refreshResult = SecurityUtils.refreshUserData()
                val refreshedRole = SecurityUtils.getCurrentUserRole()
                
                if (refreshedRole != currentUserRole) {
                    currentUserRole = refreshedRole
                    isManufacturer.value = firebaseService.isManufacturer()
                    isAdmin.value = firebaseService.isAdmin()
                }
                
                EnhancedMainScreen.userRole = refreshedRole
            }
        } catch (e: Exception) {
            currentUserRole = UserRole.USER
            EnhancedMainScreen.userRole = UserRole.USER
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
    ) {
        
        Card(
            elevation = 16.dp,  
            shape = RoundedCornerShape(24.dp), 
            backgroundColor = InLockSurface, 
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp) 
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                
                BottomNavItem(
                    destination = EnhancedBottomNavDestination.ASSETS,
                    isSelected = currentDestination == EnhancedBottomNavDestination.ASSETS,
                    onSelected = onDestinationSelected,
                    icon = AppIcons.Inventory,
                    modifier = Modifier.weight(1f)
                )

                
                BottomNavItem(
                    destination = EnhancedBottomNavDestination.TRANSFER,
                    isSelected = currentDestination == EnhancedBottomNavDestination.TRANSFER,
                    onSelected = onDestinationSelected,
                    icon = AppIcons.QrCode,
                    modifier = Modifier.weight(1f)
                )

                
                Spacer(modifier = Modifier.weight(1f))

                
                BottomNavItem(
                    destination = EnhancedBottomNavDestination.PROFILE,
                    isSelected = currentDestination == EnhancedBottomNavDestination.PROFILE,
                    onSelected = onDestinationSelected,
                    icon = AppIcons.AdminPanelSettings,
                    modifier = Modifier.weight(1f)
                )

                if (isAdmin.value || currentUserRole == UserRole.ADMIN) {
                    LaunchedEffect(Unit) {
                        EnhancedMainScreen.userRole = UserRole.ADMIN
                    }
                    
                    BottomNavItem(
                        destination = EnhancedBottomNavDestination.ADMIN,
                        isSelected = currentDestination == EnhancedBottomNavDestination.ADMIN,
                        onSelected = onDestinationSelected,
                        icon = AppIcons.AdminPanelSettings,
                        modifier = Modifier.weight(1f)
                    )
                } else if (isManufacturer.value || currentUserRole == UserRole.MANUFACTURER) {
                    LaunchedEffect(Unit) {
                        EnhancedMainScreen.userRole = UserRole.MANUFACTURER
                    }
                    
                    BottomNavItem(
                        destination = EnhancedBottomNavDestination.MANUFACTURER,
                        isSelected = currentDestination == EnhancedBottomNavDestination.MANUFACTURER,
                        onSelected = onDestinationSelected,
                        icon = AppIcons.Inventory,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LaunchedEffect(Unit) {
                        EnhancedMainScreen.userRole = UserRole.USER
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        ScanButton(
            isSelected = currentDestination == EnhancedBottomNavDestination.SCANNER,
            onClick = { onDestinationSelected(EnhancedBottomNavDestination.SCANNER) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-20).dp)
        )
    }
}

@Composable
private fun BottomNavItem(
    destination: EnhancedBottomNavDestination,
    isSelected: Boolean,
    onSelected: (EnhancedBottomNavDestination) -> Unit,
    icon: com.jetbrains.kmpapp.ui.icons.PlatformIcon,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    val animatedIconSize by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1f,
        animationSpec = tween(300)
    )
    
    val animatedTextAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.8f,
        animationSpec = tween(300)
    )
    
    val iconColor = if (isSelected) InLockPrimary else InLockTextTertiary
    val textColor = if (isSelected) InLockPrimary else InLockTextSecondary
    val fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
    
    val backgroundModifier = if (isSelected) {
        Modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    InLockPrimary.copy(alpha = 0.05f),
                    InLockPrimary.copy(alpha = 0.15f)
                )
            ),
            shape = RoundedCornerShape(16.dp)
        )
    } else Modifier
    
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(backgroundModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onSelected(destination) }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = (-2).dp)
                        .background(InLockPrimary, CircleShape)
                )
            }
            
            AppIcon(
                icon = icon,
                contentDescription = destination.title,
                tint = iconColor,
                modifier = Modifier
                    .size(26.dp)
                    .scale(animatedIconSize)
            )
        }

        Text(
            text = destination.title,
            color = textColor,
            style = MaterialTheme.typography.caption.copy(
                fontWeight = fontWeight,
                fontSize = 11.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(animatedTextAlpha)
        )
    }
}

@Composable
private fun ScanButton(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.95f,
        animationSpec = tween(300)
    )
    
    val animatedElevation by animateFloatAsState(
        targetValue = if (isSelected) 12f else 8f,
        animationSpec = tween(300)
    )
    
    val gradientColors = if (isSelected) {
        listOf(InLockSecondary, InLockPrimary)
    } else {
        listOf(InLockPrimary, InLockPrimaryVariant)
    }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(InLockPrimary.copy(alpha = 0.2f))
            )
        }
        
        Button(
            onClick = onClick,
            modifier = Modifier
                .size(64.dp)
                .scale(animatedScale)
                .clip(CircleShape),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.elevation(
                defaultElevation = animatedElevation.dp,
                pressedElevation = (animatedElevation + 4).dp
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(gradientColors),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                AppIcon(
                    icon = AppIcons.Nfc,
                    contentDescription = "Scan NFC",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}