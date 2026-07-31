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
import com.kartikey.rupeeflow.UI_Screens.bounceClick

@Composable
fun ProfileScreen(
    username: String,
    name: String, 
    email: String,
    mobile: String,
    password: String,
    dob: String, 
    paddingValues: PaddingValues, 
    onLogout: () -> Unit,
    onProfileRefresh: () -> Unit,
    startInDetails: Boolean = false, 
    onResetDetailsState: () -> Unit = {}
) {
    var currentProfileView by remember { mutableStateOf("Main") }
    var selectedOptionTitle by remember { mutableStateOf("") }

    LaunchedEffect(startInDetails) {
        if (startInDetails) {
            currentProfileView = "Details"
            onResetDetailsState() 
        }
    }

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
                        } else if (option == "App Update") { 
                            currentProfileView = "Update"
                        } else if (option in listOf("Data Download", "Help & Support")) {
                            currentProfileView = "Utility"
                        }
                    },
                    onLogout = onLogout
                )
            }
            "Details" -> {
                ProfileDetailsScreen(
                    username = username,
                    name = name,
                    email = email,
                    mobile = mobile,
                    password = password,
                    dob = dob,
                    onBackClick = { currentProfileView = "Main" },
                    onProfileUpdated = { onProfileRefresh() }
                )
            }
            "Preference" -> {
                PreferenceScreen(optionType = selectedOptionTitle, onBackClick = { currentProfileView = "Main" })
            }
            "Utility" -> {
                ProfileUtility(optionType = selectedOptionTitle, onBackClick = { currentProfileView = "Main" })
            }
            "Update" -> {
                AppUpdateScreen(onBackClick = { currentProfileView = "Main" })
            }
        }
    }
}

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

        val displayLetter = if (name.isNotBlank()) name.take(1).uppercase() else "?"
        val displayEmail = if (email.isNotBlank()) email else "Add Mail"

        Box(
            modifier = Modifier
                .size(100.dp)
                .bounceClick { onNameClick() }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary), 
            contentAlignment = Alignment.Center
        ) {
            Text(displayLetter, color = MaterialTheme.colorScheme.onPrimary, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = name.ifBlank { "User" }, 
            fontWeight = FontWeight.ExtraBold, 
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.bounceClick { onNameClick() }
        )
        
        Text(
            text = displayEmail, 
            color = MaterialTheme.colorScheme.onSurfaceVariant, 
            fontSize = 14.sp,
            modifier = Modifier.bounceClick { onNameClick() }
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        ProfileOptionRow(iconVector = Icons.Default.Lock, title = "Security Lock", onClick = { onOptionClick("Security Lock") })
        ProfileOptionRow(iconVector = Icons.Default.CurrencyRupee, title = "Currency", onClick = { onOptionClick("Currency") }) 
        ProfileOptionRow(iconVector = Icons.Default.Palette, title = "Theme", onClick = { onOptionClick("Theme") }) 
        
        ProfileOptionRow(iconVector = Icons.Default.Download, title = "Data Download", onClick = { onOptionClick("Data Download") }) 
        ProfileOptionRow(iconVector = Icons.Default.SupportAgent, title = "Help & Support", onClick = { onOptionClick("Help & Support") }) 
        
        ProfileOptionRow(
            iconVector = Icons.Default.Info, 
            title = "App Update", 
            onClick = { onOptionClick("App Update") }
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        ProfileOptionRow(
            iconVector = Icons.Default.ExitToApp, 
            title = "Logout", 
            textColor = MaterialTheme.colorScheme.error, 
            onClick = { onLogout() }
        )
    }
}

@Composable
private fun ProfileOptionRow(
    iconVector: ImageVector,
    title: String, 
    textColor: Color? = null, 
    onClick: () -> Unit
) {
    val finalColor = textColor ?: MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.97f) { onClick() } 
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = iconVector, contentDescription = title, tint = finalColor, modifier = Modifier.size(24.dp))
        
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = finalColor)
    }
    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)
}
