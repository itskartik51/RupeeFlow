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
fun ProfileScreen(username: String, paddingValues: PaddingValues, onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // 1. Profile Header
        Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(Color(0xFF2E7D32)), contentAlignment = Alignment.Center) {
            Text(username.take(2).uppercase(), color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(username, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        Text("contact@rupeeflow.com", color = Color.Gray, fontSize = 14.sp) // Dummy email for now
        
        Spacer(modifier = Modifier.height(32.dp))

        // 2. Options List
        ProfileOptionRow(Icons.Default.Lock, "Security Lock", onClick = { /* TODO: Next step logic */ })
        ProfileOptionRow(Icons.Default.ArrowDropDown, "Data Download", onClick = { /* TODO: Next step logic */ }) 
        ProfileOptionRow(Icons.Default.AccountBox, "Currency", onClick = { /* TODO: Next step logic */ }) 
        ProfileOptionRow(Icons.Default.Settings, "Theme", onClick = { /* TODO: Next step logic */ }) 
        ProfileOptionRow(Icons.Default.Email, "Help & Support", onClick = { /* TODO: Next step logic */ }) 
        ProfileOptionRow(Icons.Default.Info, "App Update & Info", onClick = { /* TODO: Next step logic */ })
        
        Spacer(modifier = Modifier.height(32.dp))

        // 3. Logout (Isme onLogout() direct connect kar diya hai)
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
