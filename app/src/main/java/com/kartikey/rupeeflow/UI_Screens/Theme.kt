package com.kartikey.rupeeflow.UI_Screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Aapke pasandida Mint aur Lavender colors
val MintColor = Color(0xFF98FF98) 
val LavenderColor = Color(0xFFE6E6FA)
val DarkText = Color(0xFF1E1E1E)

private val RupeeFlowColorScheme = lightColorScheme(
    primary = MintColor,
    secondary = LavenderColor,
    background = Color.White,
    onPrimary = DarkText,
    onBackground = DarkText
)

@Composable
fun RupeeFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RupeeFlowColorScheme,
        content = content
    )
}
