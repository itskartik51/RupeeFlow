package com.kartikey.rupeeflow.UI_Screens

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ==========================================
// STRICT CORE COLORS (NO MATERIAL YOU OVERRIDE)
// ==========================================
val PrimaryGreenLight = Color(0xFF2E7D32)
val PrimaryGreenDark = Color(0xFF1ED760) // UPDATE: Naya Neon/Vibrant Green shade for Dark Mode

val PrimaryContainerLight = Color(0xFFE8F5E9)
val PrimaryContainerDark = Color(0xFF1B3B22) // UPDATE: Ye dark hi rahega taaki 'K' avatar dark green dikhe

val BackgroundLight = Color(0xFFF8F9FA) 
val BackgroundDark = Color(0xFF121212)

// CARDS KE LIYE PURE WHITE AUR PURE DARK GRAY
val SurfaceLight = Color.White 
val SurfaceDark = Color(0xFF1E1E1E)

val SurfaceVariantLight = Color(0xFFEEEEEE)
val SurfaceVariantDark = Color(0xFF2C2C2C)

val TextPrimaryLight = Color.Black
val TextPrimaryDark = Color(0xFFF5F5F5)

val TextSecondaryLight = Color.Gray
val TextSecondaryDark = Color(0xFFAAAAAA)

val ErrorRed = Color(0xFFD32F2F)

// ==========================================
// THEME SCHEMES
// ==========================================
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreenDark,
    primaryContainer = PrimaryContainerDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onPrimary = Color.Black, 
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    error = ErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreenLight,
    primaryContainer = PrimaryContainerLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onPrimary = Color.White,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    error = ErrorRed
)

@Composable
fun RupeeFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Dynamic Color Engine completely removed. Only STRICT Light/Dark.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
