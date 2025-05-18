package com.jetbrains.kmpapp.screens.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.jetbrains.kmpapp.screens.main.EnhancedMainScreen
import com.jetbrains.kmpapp.ui.components.EnhancedBottomNavDestination
import com.jetbrains.kmpapp.ui.icons.AppIcon
import com.jetbrains.kmpapp.ui.icons.AppIcons
import com.jetbrains.kmpapp.ui.theme.InLockBlue
import com.jetbrains.kmpapp.ui.theme.InLockDarkBlue
import com.jetbrains.kmpapp.ui.theme.InLockTheme
import com.jetbrains.kmpapp.ui.theme.InLockWhite
import com.jetbrains.kmpapp.utils.getCurrentTimeMillis

data object LoginScreen : Screen {
    private const val TAG = "LoginScreen"

    @Composable
    override fun Content() {
        LaunchedEffect(Unit) {
            com.jetbrains.kmpapp.ui.components.NavigationManager.hideBottomNavigation()
            com.jetbrains.kmpapp.ui.components.hideBottomNav()
        }
        
        InLockTheme {
            val navigator = LocalNavigator.currentOrThrow
            val screenModel: LoginScreenModel = getScreenModel()
            val uiState by screenModel.uiState.collectAsState()
            val focusManager = LocalFocusManager.current
            val scrollState = rememberScrollState()

            LaunchedEffect(uiState.isAuthenticated) {
                if (uiState.isAuthenticated) {
                    val uniqueKey = "main_${getCurrentTimeMillis()}"
                    val newMainScreen = EnhancedMainScreen(
                        initialDestination = EnhancedBottomNavDestination.ASSETS,
                        screenKey = uniqueKey
                    )
                    
                    EnhancedMainScreen.resetNavigation()
                    EnhancedMainScreen.clearPostLogoutFlag()
                    
                    EnhancedMainScreen.uiRecreationKey = getCurrentTimeMillis()
                    
                    navigator.replaceAll(newMainScreen)
                }
            }

            LaunchedEffect(Unit) {
                screenModel.checkCurrentUser()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                InLockWhite,
                                InLockWhite,
                                Color(0xFFF5F9FF)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(48.dp))

                    Text(
                        text = "INLOCK",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = InLockBlue,
                        letterSpacing = 4.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        text = "PROTECTING RIGHTS",
                        fontSize = 14.sp,
                        letterSpacing = 4.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 48.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            
                            Text(
                                text = if (uiState.isRegistrationMode) "Create Account" else "Welcome Back",
                                style = MaterialTheme.typography.h5.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                ),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Text(
                                text = if (uiState.isRegistrationMode)
                                    "Sign up to get started with InLock"
                                else
                                    "Sign in to continue to your account",
                                style = MaterialTheme.typography.body1.copy(
                                    color = Color.Gray
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 32.dp)
                            )

                            
                            var email by remember { mutableStateOf("") }
                            var password by remember { mutableStateOf("") }
                            var displayName by remember { mutableStateOf("") }
                            var username by remember { mutableStateOf("") }

                            AnimatedVisibility(
                                visible = uiState.isRegistrationMode,
                                enter = fadeIn() + slideInVertically(),
                                exit = fadeOut() + slideOutVertically()
                            ) {
                                Column {
                                    OutlinedTextField(
                                        value = displayName,
                                        onValueChange = { displayName = it },
                                        label = { Text("Full Name") },
                                        leadingIcon = {
                                            AppIcon(
                                                icon = AppIcons.AdminPanelSettings,
                                                contentDescription = null,
                                                tint = InLockBlue
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        keyboardActions = KeyboardActions(
                                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                        ),
                                        singleLine = true,
                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                            focusedBorderColor = InLockBlue,
                                            unfocusedBorderColor = Color.LightGray,
                                            cursorColor = InLockBlue
                                        )
                                    )

                                    OutlinedTextField(
                                        value = username,
                                        onValueChange = { username = it },
                                        label = { Text("Username") },
                                        leadingIcon = {
                                            AppIcon(
                                                icon = AppIcons.AdminPanelSettings,
                                                contentDescription = null,
                                                tint = InLockBlue
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        keyboardActions = KeyboardActions(
                                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                        ),
                                        singleLine = true,
                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                            focusedBorderColor = InLockBlue,
                                            unfocusedBorderColor = Color.LightGray,
                                            cursorColor = InLockBlue
                                        )
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email") },
                                leadingIcon = {
                                    AppIcon(
                                        icon = AppIcons.Category,
                                        contentDescription = null,
                                        tint = InLockBlue
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                singleLine = true,
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = InLockBlue,
                                    unfocusedBorderColor = Color.LightGray,
                                    cursorColor = InLockBlue
                                )
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                leadingIcon = {
                                    AppIcon(
                                        icon = AppIcons.ErrorOutline,
                                        contentDescription = null,
                                        tint = InLockBlue
                                    )
                                },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 24.dp),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                ),
                                singleLine = true,
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = InLockBlue,
                                    unfocusedBorderColor = Color.LightGray,
                                    cursorColor = InLockBlue
                                )
                            )

                            AnimatedVisibility(visible = uiState.error.isNotEmpty()) {
                                Text(
                                    text = uiState.error,
                                    color = MaterialTheme.colors.error,
                                    modifier = Modifier
                                        .padding(bottom = 16.dp)
                                        .background(
                                            color = MaterialTheme.colors.error.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }

                            Button(
                                onClick = {
                                    if (uiState.isRegistrationMode) {
                                        screenModel.signUp(email, password, displayName, username)
                                    } else {
                                        screenModel.signIn(email, password)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = InLockBlue,
                                    contentColor = Color.White
                                ),
                                enabled = !uiState.isLoading
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Text(
                                        if (uiState.isRegistrationMode) "Create Account" else "Sign In",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedButton(
                        onClick = {
                            screenModel.signInAnonymously()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = InLockBlue
                        ),
                        enabled = !uiState.isLoading
                    ) {
                        Text(
                            "Continue as Guest",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    TextButton(
                        onClick = { screenModel.toggleRegistrationMode() },
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            if (uiState.isRegistrationMode)
                                "Already have an account? Sign In"
                            else
                                "Don't have an account? Sign Up",
                            color = InLockDarkBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFFF5F9FF)
                                )
                            )
                        )
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\"FURTA CAVENDA, FALSIFICATIONES TOLLENDAE.\"",
                        fontSize = 10.sp,
                        letterSpacing = 2.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}