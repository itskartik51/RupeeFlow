package com.kartikey.rupeeflow.UI_Screens.Profile

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    username: String,
    name: String, 
    email: String, 
    paddingValues: PaddingValues, 
    onLogout: () -> Unit
) {
    // Mini-Navigator State (Boss Script Routing)
    var currentProfileView by remember { mutableStateOf("Main") }
    var selectedOptionTitle by remember { mutableStateOf("") }

    // BOSS BACK PHYSICS: Agar Main pe nahi hai, toh back aane par Main pe laayega
    BackHandler(enabled = currentProfileView != "Main") {
        currentProfileView = "Main"
    }

    Crossfade(targetState = currentProfileView, animationSpec = tween(300), label = "Profile Nav") { view ->
        when (view) {
            "Main" -> {
                ProfileMainContent(
                    name = name,
                    email = email,
                    paddingValues = paddingValues,
                    onNameClick = { currentProfileView = "Details" },
                    onOptionClick = { option ->
                        selectedOptionTitle = option
                        if (option in listOf("Security Lock", "Currency", "Theme")) {
                            currentProfileView = "Preference"
                        } else if (option in listOf("Data Download", "Help & Support", "App Update & Info")) {
                            currentProfileView = "Utility"
                        }
                    },
                    onLogout = onLogout
                )
            }
            "Details" -> {
                // EDIT: Yahan 'username = username' pass kiya gaya hai
                ProfileDetailsScreen(username = username, onBackClick = { currentProfileView = "Main" })
            }
            "Preference" -> {
                PreferenceScreen(optionType = selectedOptionTitle, onBackClick = { currentProfileView = "Main" })
            }
            "Utility" -> {
                ProfileUtility(optionType = selectedOptionTitle, onBackClick = { currentProfileView = "Main" })
            }
        }
    }
}

// Internal UI function
@Composable
private fun ProfileMainContent(
    name: String,
    email: String,
    paddingValues: PaddingValues,
    onNameClick: () -> Unit,
    onOptionClick: (String) -> Unit,
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

        // Profile Header
        val displayLetter = if (name.isNotBlank()) name.take(1).uppercase() else "?"
        val displayEmail = if (email.isNotBlank()) email else "Add Mail"

        Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(Color(0xFF2E7D32)), contentAlignment = Alignment.Center) {
            Text(displayLetter, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = name.ifBlank { "User" }, 
            fontWeight = FontWeight.ExtraBold, 
            fontSize = 22.sp,
            modifier = Modifier.clickable { onNameClick() }
        )
        
        Text(
            text = displayEmail, 
            color = Color.Gray, 
            fontSize = 14.sp,
            modifier = Modifier.clickable { onNameClick() }
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        // GROUP 2 (Preferences)
        ProfileOptionRow(Icons.Default.Lock, "Security Lock", onClick = { onOptionClick("Security Lock") })
        ProfileOptionRow(Icons.Default.CurrencyRupee, "Currency", onClick = { onOptionClick("Currency") }) 
        ProfileOptionRow(Icons.Default.Palette, "Theme", onClick = { onOptionClick("Theme") }) 
        
        // GROUP 1 (Utilities)
        ProfileOptionRow(Icons.Default.Download, "Data Download", onClick = { onOptionClick("Data Download") }) 
        ProfileOptionRow(Icons.Default.SupportAgent, "Help & Support", onClick = { onOptionClick("Help & Support") }) 
        ProfileOptionRow(Icons.Default.Info, "App Update & Info", onClick = { onOptionClick("App Update & Info") })
        
        Spacer(modifier = Modifier.height(32.dp))

        // Logout
        ProfileOptionRow(Icons.Default.ExitToApp, "Logout", textColor = Color.Red, onClick = { onLogout() })
    }
}

@Composable
private fun ProfileOptionRow(icon: ImageVector, title: String, textColor: Color = Color.Black, onClick: () -> Unit) {
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
