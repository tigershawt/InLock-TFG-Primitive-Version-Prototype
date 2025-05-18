
package com.jetbrains.kmpapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp


val InLockPrimary = Color(0xFF3D5AF1)        
val InLockPrimaryVariant = Color(0xFF2A3EB1)  
val InLockSecondary = Color(0xFF00D1B2)      
val InLockSecondaryVariant = Color(0xFF00A896) 
val InLockAccent = Color(0xFFFFA000)         
val InLockBackground = Color(0xFFF8F9FC)     
val InLockSurface = Color(0xFFFFFFFF)        
val InLockTextPrimary = Color(0xFF1A1B25)    
val InLockTextSecondary = Color(0xFF696A75)  
val InLockTextTertiary = Color(0xFF9394A0)   
val InLockDivider = Color(0xFFEAEAF2)        
val InLockSuccess = Color(0xFF4CAF50)        
val InLockError = Color(0xFFFF3D71)          
val InLockWarning = Color(0xFFFFAA00)        
val InLockInfo = Color(0xFF2196F3)           


val InLockBlue = InLockPrimary
val InLockDarkBlue = InLockPrimaryVariant
val InLockLightBlue = InLockSecondary
val InLockWhite = InLockSurface


private val LightColorPalette = lightColors(
    primary = InLockPrimary,
    primaryVariant = InLockPrimaryVariant,
    secondary = InLockSecondary,
    background = InLockBackground,
    surface = InLockSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = InLockTextPrimary,
    onSurface = InLockTextPrimary,
    error = InLockError,
    onError = Color.White
)


private val DarkColorPalette = darkColors(
    primary = InLockPrimary,
    primaryVariant = InLockPrimaryVariant,
    secondary = InLockSecondary,
    background = Color(0xFF121C2D), 
    surface = Color(0xFF1A2536),    
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    error = InLockError,
    onError = Color.White
)


private val InLockTypography = Typography(
    h1 = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = (-0.5).sp,
        color = InLockTextPrimary
    ),
    h2 = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = (-0.25).sp,
        color = InLockTextPrimary
    ),
    h3 = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = 0.sp,
        color = InLockTextPrimary
    ),
    h4 = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        letterSpacing = 0.sp,
        color = InLockTextPrimary
    ),
    h5 = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.sp,
        color = InLockTextPrimary
    ),
    h6 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        letterSpacing = 0.15.sp,
        color = InLockTextPrimary
    ),
    subtitle1 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp,
        color = InLockTextSecondary
    ),
    subtitle2 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp,
        color = InLockTextSecondary
    ),
    body1 = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp,
        color = InLockTextPrimary
    ),
    body2 = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp,
        color = InLockTextSecondary
    ),
    button = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 1.25.sp
    ),
    caption = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp,
        color = InLockTextTertiary
    ),
    overline = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        letterSpacing = 1.5.sp,
        color = InLockTextSecondary
    )
)


val InLockCardElevation = 2.dp
val InLockButtonElevation = 4.dp
val InLockCardCornerRadius = 12.dp
val InLockButtonCornerRadius = 8.dp
val InLockSmallCornerRadius = 4.dp
val InLockInputCornerRadius = 8.dp
val InLockDefaultAnimationDuration = 300

@Composable
fun InLockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = InLockTypography,
        content = content
    )
}