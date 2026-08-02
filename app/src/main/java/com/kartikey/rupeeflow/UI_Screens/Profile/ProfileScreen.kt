package com.kartikey.rupeeflow.UI_Screens.Profile

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    themeMode: Int,                     
    onThemeChange: (Int) -> Unit,
    isUpdateAvailable: Boolean,
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
                    themeMode = themeMode,
                    onThemeChange = onThemeChange,
                    isUpdateAvailable = isUpdateAvailable,
                    onNameClick = { currentProfileView = "Details" },
                    onOptionClick = { option ->
                        selectedOptionTitle = option
                        if (option == "Security Lock") {
                            currentProfileView = "Preference"
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
        }
    }
}

@Composable
private fun ProfileMainContent(
    name: String,
    email: String,
    paddingValues: PaddingValues,
    themeMode: Int,
    onThemeChange: (Int) -> Unit,
    isUpdateAvailable: Boolean,
    onNameClick: () -> Unit,
    onOptionClick: (String) -> Unit,
    onLogout: () -> Unit
) {
    var themeExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp)
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
        
        // Expandable Currency Block
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(scaleDown = 0.97f) { currencyExpanded = !currencyExpanded } 
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CurrencyRupee, contentDescription = "Currency", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text("Currency", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (currencyExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            AnimatedVisibility(visible = currencyExpanded) {
                Column(modifier = Modifier.fillMaxWidth().padding(start = 40.dp, bottom = 12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick(scaleDown = 0.98f) { }
                            .padding(vertical = 2.dp), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = true,
                            onClick = { }, 
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary, 
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp)) 
                        Text(text = "₹  INR", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        }
        
        // Expandable Theme Block
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(scaleDown = 0.97f) { themeExpanded = !themeExpanded } 
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Palette, contentDescription = "Theme", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text("Theme", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (themeExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            AnimatedVisibility(visible = themeExpanded) {
                Column(modifier = Modifier.fillMaxWidth().padding(start = 40.dp, bottom = 12.dp)) {
                    ThemeRadioOption("System Default", 0, themeMode, onThemeChange)
                    ThemeRadioOption("Light", 1, themeMode, onThemeChange)
                    ThemeRadioOption("Dark", 2, themeMode, onThemeChange)
                }
            }
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        }
        
        ProfileOptionRow(iconVector = Icons.Default.Download, title = "Data Download", onClick = { onOptionClick("Data Download") }) 
        ProfileOptionRow(iconVector = Icons.Default.SupportAgent, title = "Help & Support", onClick = { onOptionClick("Help & Support") }) 
        
        AppUpdateRow(isUpdateAvailableBadge = isUpdateAvailable)
        
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                modifier = Modifier.bounceClick { onLogout() },
                color = MaterialTheme.colorScheme.surfaceVariant, 
                shape = RoundedCornerShape(50), 
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp, 
                        contentDescription = "Log out", 
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Log out", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 14.sp, 
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

// FIX: Added the missing helper functions back to ProfileScreen.kt
@Composable
fun ThemeRadioOption(title: String, modeValue: Int, currentMode: Int, onThemeChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.98f) { onThemeChange(modeValue) }
            .padding(vertical = 2.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = (currentMode == modeValue),
            onClick = { onThemeChange(modeValue) },
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary, 
                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = title, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ProfileOptionRow(
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
    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
}
