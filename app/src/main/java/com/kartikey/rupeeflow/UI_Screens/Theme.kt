package com.kartikey.rupeeflow.UI_Screens

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ==========================================
// CORE COLORS FOR PREMIUM FINANCE UI
// ==========================================
val PrimaryGreenLight = Color(0xFF2E7D32) // तुम्हारा ओरिजिनल डार्क ग्रीन
val PrimaryGreenDark = Color(0xFF81C784)  // डार्क मोड के लिए सॉफ्ट/ब्राइट ग्रीन

val PrimaryContainerLight = Color(0xFFE8F5E9)
val PrimaryContainerDark = Color(0xFF1B3B22)  // डार्क मोड में कंटेनर के लिए डीप ग्रीन

val BackgroundLight = Color(0xFFF8F9FA)
val BackgroundDark = Color(0xFF121212)

val SurfaceLight = Color.White
val SurfaceDark = Color(0xFF1E1E1E)

val SurfaceVariantLight = Color(0xFFF5F5F5)
val SurfaceVariantDark = Color(0xFF2C2C2C)

val TextPrimaryLight = Color.Black
val TextPrimaryDark = Color(0xFFF5F5F5)

val TextSecondaryLight = Color.Gray
val TextSecondaryDark = Color(0xFFAAAAAA)

val ErrorRedLight = Color(0xFFD32F2F)
val ErrorRedDark = Color(0xFFEF5350)

// ==========================================
// THEME SCHEMES
// ==========================================
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreenDark,
    primaryContainer = PrimaryContainerDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onPrimary = Color.Black, // डार्क मोड में बटन के अंदर का टेक्स्ट ब्लैक अच्छा लगता है
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    error = ErrorRedDark
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
    error = ErrorRedLight
)

@Composable
fun RupeeFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // इसे False रखा है ताकि फोन के डिफ़ॉल्ट कलर्स हमारी ग्रीन थीम को ओवरराइड ना करें
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    // Status bar (Top bar) color handling based on Theme
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
