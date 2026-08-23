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
// STRICT CORE COLORS
// ==========================================
val PrimaryGreenLight = Color(0xFF2E7D32)
val PrimaryGreenDark = Color(0xFF22C55E) // Crisp Emerald/Neon Green (Locked)

val PrimaryContainerLight = Color(0xFFE8F5E9)
val PrimaryContainerDark = Color(0xFF1B3B22) 

val BackgroundLight = Color(0xFFF8F9FA) 
val BackgroundDark = Color(0xFF0A0A0A) // True Neutral Dark / Pitch Black

val SurfaceLight = Color.White 
val SurfaceDark = Color(0xFF141819) // Custom Deep Slate for Cards & Dialogs

val SurfaceVariantLight = Color(0xFFEEEEEE)
val SurfaceVariantDark = Color(0xFF262626) // Pure Grey Borders & Dividers

val TextPrimaryLight = Color.Black
val TextPrimaryDark = Color(0xFFF5F5F5)

val TextSecondaryLight = Color.Gray
val TextSecondaryDark = Color(0xFFA1A1A1) // Neutral Faded Grey for Subtext

val ErrorRed = Color(0xFFFC5F3A) // Custom Vibrant Orange-Red

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
    themeMode: Int = 0, // 0 = System, 1 = Light, 2 = Dark
    darkTheme: Boolean = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    },
    content: @Composable () -> Unit
) {
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
