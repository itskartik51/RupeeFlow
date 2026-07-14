package com.kartikey.rupeeflow.UI_Screens.Profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileScreen(
    name: String, // Naya variable: Asli naam ke liye
    email: String, // Naya variable: Email ke liye
    paddingValues: PaddingValues, 
    onNameClick: () -> Unit, // Name/Email par click handle karega
    onOptionClick: (String) -> Unit, // Teeno files ke routing ke liye
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // 1. Profile Header (Strict Logic applied)
        // Agar name khali hai toh '?' dikhega, warna first letter
        val displayLetter = if (name.isNotBlank()) name.take(1).uppercase() else "?"
        val displayEmail = if (email.isNotBlank()) email else "Add Mail"

        Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(Color(0xFF2E7D32)), contentAlignment = Alignment.Center) {
            Text(displayLetter, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Name (Clickable)
        Text(
            text = name.ifBlank { "User" }, 
            fontWeight = FontWeight.ExtraBold, 
            fontSize = 22.sp,
            modifier = Modifier.clickable { onNameClick() }
        )
        
        // Email or "Add Mail" (Clickable)
        Text(
            text = displayEmail, 
            color = Color.Gray, 
            fontSize = 14.sp,
            modifier = Modifier.clickable { onNameClick() }
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        // 2. Options List (Updated Sorting Order)
        
        // --- GROUP 2 (Preferences: Upar rakha gaya hai) ---
        ProfileOptionRow(Icons.Default.Lock, "Security Lock", onClick = { onOptionClick("Security Lock") })
        ProfileOptionRow(Icons.Default.CurrencyRupee, "Currency", onClick = { onOptionClick("Currency") }) 
        ProfileOptionRow(Icons.Default.Palette, "Theme", onClick = { onOptionClick("Theme") }) 
        
        // --- GROUP 1 (Utilities: Niche rakha gaya hai) ---
        ProfileOptionRow(Icons.Default.Download, "Data Download", onClick = { onOptionClick("Data Download") }) 
        ProfileOptionRow(Icons.Default.SupportAgent, "Help & Support", onClick = { onOptionClick("Help & Support") }) 
        ProfileOptionRow(Icons.Default.Info, "App Update & Info", onClick = { onOptionClick("App Update & Info") })
        
        Spacer(modifier = Modifier.height(32.dp))

        // 3. Logout
        ProfileOptionRow(Icons.Default.ExitToApp, "Logout", textColor = Color.Red, onClick = { onLogout() })
    }
}

@Composable
fun ProfileOptionRow(icon: ImageVector, title: String, textColor: Color = Color.Black, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = textColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textColor)
    }
    HorizontalDivider(thickness = 1.dp, color = Color(0xFFEEEEEE))
}
