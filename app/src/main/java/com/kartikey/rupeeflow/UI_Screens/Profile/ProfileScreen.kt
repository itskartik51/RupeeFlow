package com.kartikey.rupeeflow.UI_Screens.Profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    username: String,
    name: String, 
    email: String,
    mobile: String,
    profilePicUrl: String, 
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
                    username = username,
                    name = name,
                    email = email,
                    mobile = mobile,
                    profilePicUrl = profilePicUrl,
                    paddingValues = paddingValues,
                    themeMode = themeMode,
                    onThemeChange = onThemeChange,
                    isUpdateAvailable = isUpdateAvailable,
                    onNameClick = { currentProfileView = "Details" },
                    onLogout = onLogout
                )
            }
            "Details" -> {
                ProfileDetailsScreen(
                    username = username,
                    name = name,
                    email = email,
                    mobile = mobile,
                    profilePicUrl = profilePicUrl, 
                    dob = dob,
                    onBackClick = { currentProfileView = "Main" },
                    onProfileUpdated = { onProfileRefresh() }
                )
            }
        }
    }
}

@Composable
private fun ProfileMainContent(
    username: String,
    name: String,
    email: String,
    mobile: String,
    profilePicUrl: String,
    paddingValues: PaddingValues,
    themeMode: Int,
    onThemeChange: (Int) -> Unit,
    isUpdateAvailable: Boolean,
    onNameClick: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("RupeeFlowPrefs", Context.MODE_PRIVATE) }
    
    var securityExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }
    var helpExpanded by remember { mutableStateOf(false) }

    var isLockEnabled by remember { 
        mutableStateOf(sharedPreferences.getBoolean("isSecurityLockEnabled", false)) 
    }

    val encodedEmail = "cnVwZWVmbG93LnJmQGdtYWlsLmNvbQ==" 
    val encodedPhone = "OTE9ODI4ODk3MjY4" 

    val decodedEmail = remember(encodedEmail) {
        try { String(Base64.decode(encodedEmail, Base64.DEFAULT), Charsets.UTF_8) } catch (e: Exception) { "" }
    }
    val decodedPhone = remember(encodedPhone) {
        try { String(Base64.decode(encodedPhone, Base64.DEFAULT), Charsets.UTF_8) } catch (e: Exception) { "" }
    }

    // 🚀 FIXED BIOMETRIC AUTH FOR STANDARD SWITCH
    fun authenticateAndToggleLock(targetState: Boolean) {
        val activity = context as? FragmentActivity ?: return
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    sharedPreferences.edit().putBoolean("isSecurityLockEnabled", targetState).apply()
                    isLockEnabled = targetState
                    Toast.makeText(context, if (targetState) "Security Lock Enabled" else "Security Lock Disabled", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Reset UI State on error
                    isLockEnabled = !targetState
                    Toast.makeText(context, "Auth Error: $errString", Toast.LENGTH_SHORT).show()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("RupeeFlow Security")
            .setSubtitle(if (targetState) "Confirm device lock to enable Security Lock" else "Confirm device lock to disable Security Lock")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        val displayEmail = if (email.isNotBlank()) email else "Add Mail"
        val displayName = name.ifBlank { "User" }

        ProfileAvatar(
            name = displayName,
            profilePicUrl = profilePicUrl,
            size = 100.dp,
            fontSize = 40.sp,
            onClick = onNameClick
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = displayName, 
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

        // Security Lock Block
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(scaleDown = 0.97f) { securityExpanded = !securityExpanded } 
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Lock, contentDescription = "Security Lock", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text("Security Lock", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            }
            
            AnimatedVisibility(visible = securityExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 40.dp, end = 8.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isLockEnabled) "App Lock is Active" else "App Lock is Off", 
                        fontSize = 15.sp, 
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // 🚀 FIXED: Standard Material Design 3 Switch
                    Switch(
                        checked = isLockEnabled,
                        onCheckedChange = { newState ->
                            // Biometric trigger logic still works instantly on click
                            authenticateAndToggleLock(newState)
                        }
                    )
                }
            }
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        }

        // Currency Block (RADIO OPTION FIXED)
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
            }
            
            AnimatedVisibility(visible = currencyExpanded) {
                Column(modifier = Modifier.fillMaxWidth().padding(start = 40.dp, bottom = 12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            // Fixed: Using clickable here makes the entire row interactable like a RadioOption
                            .clickable { } 
                            .padding(vertical = 8.dp), 
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
                        Spacer(modifier = Modifier.width(12.dp)) 
                        Text(text = "₹  INR", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        }
        
        // Theme Block
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
        
        // Data Download
        ProfileOptionRow(
            iconVector = Icons.Default.Download, 
            title = "Data Download", 
            onClick = { 
                Toast.makeText(context, "Data Download feature coming soon!", Toast.LENGTH_SHORT).show() 
            }
        ) 
        
        // Help & Support Block
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(scaleDown = 0.97f) { helpExpanded = !helpExpanded } 
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.SupportAgent, contentDescription = "Help & Support", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text("Help & Support", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            }
            
            AnimatedVisibility(visible = helpExpanded) {
                Column(modifier = Modifier.fillMaxWidth().padding(start = 40.dp, bottom = 12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick(scaleDown = 0.98f) {
                                try {
                                    val mailBody = "Dear RupeeFlow,\nI am Need Help regarding..."
                                    val encodedSubject = Uri.encode("RupeeFlow Support Inquiry")
                                    val encodedBody = Uri.encode(mailBody)
                                    
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:$decodedEmail?subject=$encodedSubject&body=$encodedBody")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Send Email"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 8.dp), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email, 
                            contentDescription = "Email Support", 
                            tint = MaterialTheme.colorScheme.primary, 
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Email Support", 
                            fontSize = 15.sp, 
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick(scaleDown = 0.98f) {
                                try {
                                    val waMessage = "Hi RupeeFlow, I need help regarding "
                                    val encodedText = Uri.encode(waMessage)
                                    val waUrl = "https://wa.me/$decodedPhone?text=$encodedText"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 8.dp), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = "WhatsApp Support", tint = Color(0xFF25D366), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "WhatsApp Support", 
                            fontSize = 15.sp, 
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        }

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

// 🚀 FIXED: UNIVERSAL PROFILE AVATAR WITH CUT-OUT DESIGN
@Composable
fun ProfileAvatar(
    name: String,
    profilePicUrl: String,
    size: androidx.compose.ui.unit.Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    onClick: () -> Unit = {},
    isEditable: Boolean = false,
    onEditClick: () -> Unit = {},
    forceUpdateTrigger: Long = 0L 
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // Using simple CacheManager mock for simulation
    val profileFile = remember { context.getCacheDir().resolve("profile.pic") }
    
    var lastModified by remember(forceUpdateTrigger) { mutableStateOf(if (profileFile.exists()) profileFile.lastModified() else 0L) }
    var fileExists by remember(lastModified) { mutableStateOf(profileFile.exists()) }

    Box(modifier = Modifier.size(size)) {
        // Main Avatar Circle (Static if Editable, Bouncy if Not)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .then(if (!isEditable) Modifier.bounceClick { onClick() } else Modifier),
            contentAlignment = Alignment.Center
        ) {
            if (fileExists) {
                // AsyncImage simulation, will show blank here but data logic is present
                Box(modifier = Modifier.fillMaxSize().background(Color.Gray)) 
            } else {
                val displayLetter = if (name.isNotBlank()) name.take(1).uppercase() else "?"
                Text(displayLetter, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = fontSize)
            }
        }
        
        // 🚀 SMART CUT-OUT CAMERA ICON
        if (isEditable) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp) // Slight offset for perfect overlap
                    .size(size * 0.35f)
                    .background(MaterialTheme.colorScheme.background, CircleShape) // The gap/cut-out matching screen background
                    .padding(4.dp) // Gap thickness
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .bounceClick { onEditClick() }, // Only camera is bouncy
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera, 
                    contentDescription = "Edit Photo", 
                    tint = MaterialTheme.colorScheme.onPrimary, 
                    modifier = Modifier.size(size * 0.18f)
                )
            }
        }
    }
}

@Composable
fun ThemeRadioOption(title: String, modeValue: Int, currentMode: Int, onThemeChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Fixed: Entire row is bouncy and clickable like standard RadioOption
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
        Spacer(modifier = Modifier.width(12.dp))
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

// Simple AppUpdateRow mock for profile screen logic
@Composable
fun AppUpdateRow(isUpdateAvailableBadge: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = Icons.Default.Update, contentDescription = "Update", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text("App Update", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        if (isUpdateAvailableBadge) {
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error)
            )
        }
    }
    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
}

// ProfileDetailsScreen mock for simulation
@Composable
fun ProfileDetailsScreen(
    username: String,
    name: String,
    email: String,
    mobile: String,
    profilePicUrl: String,
    dob: String,
    onBackClick: () -> Unit,
    onProfileUpdated: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Profile Details Screen Simulation", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Button(onClick = onBackClick) { Text("Back") }
    }
}
