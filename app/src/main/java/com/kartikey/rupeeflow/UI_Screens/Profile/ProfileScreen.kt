package com.kartikey.rupeeflow.UI_Screens.Profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
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

        // Expandable Security Lock Block
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
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (securityExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                    CustomLockToggleSwitch(
                        checked = isLockEnabled,
                        onCheckedChange = { newState ->
                            authenticateAndToggleLock(newState)
                        }
                    )
                }
            }
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        }

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
        
        // Simple Clickable Data Download
        ProfileOptionRow(
            iconVector = Icons.Default.Download, 
            title = "Data Download", 
            onClick = { 
                Toast.makeText(context, "Data Download feature coming soon!", Toast.LENGTH_SHORT).show() 
            }
        ) 
        
        // Expandable Help & Support Block
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
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (helpExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            AnimatedVisibility(visible = helpExpanded) {
                Column(modifier = Modifier.fillMaxWidth().padding(start = 40.dp, bottom = 12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick(scaleDown = 0.98f) {
                                try {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:rupeeflow.rf@gmail.com")
                                        putExtra(Intent.EXTRA_SUBJECT, "RupeeFlow Support Inquiry")
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
                            contentDescription = "Mail", 
                            tint = MaterialTheme.colorScheme.primary, 
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "rupeeflow.rf@gmail.com", 
                            fontSize = 15.sp, 
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
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

// ==========================================
// CUSTOM GRADIENT LOCK TOGGLE SWITCH (Uiverse.io Exact Replica)
// ==========================================
@Composable
fun CustomLockToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 52.dp else 4.dp,
        animationSpec = tween(durationMillis = 300),
        label = "ThumbOffset"
    )

    val trackBrush = if (checked) {
        Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF064E3B)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFFFB7185), Color(0xFF7F1D1D)))
    }

    Box(
        modifier = Modifier
            .width(96.dp)
            .height(48.dp)
            .clip(CircleShape)
            .background(trackBrush)
            .bounceClick(scaleDown = 0.95f) { onCheckedChange(!checked) }
    ) {
        val lockPathData = "M30,46V38a20,20,0,0,1,40,0v8a8,8,0,0,1,8,8V74a8,8,0,0,1-8,8H30a8,8,0,0,1-8-8V54A8,8,0,0,1,30,46Zm32-8v8H38V38a12,12,0,0,1,24,0Z"
        val unlockPathData = "M50,18A19.9,19.9,0,0,0,30,38v8a8,8,0,0,0-8,8V74a8,8,0,0,0,8,8H70a8,8,0,0,0,8-8V54a8,8,0,0,0-8-8H38V38a12,12,0,0,1,23.6-3,4,4,0,1,0,7.8-2A20.1,20.1,0,0,0,50,18Z"

        val iconPath = remember(checked) {
            PathParser().parsePathString(
                if (checked) unlockPathData else lockPathData
            ).toPath()
        }

        Box(
            modifier = Modifier
                .offset(x = thumbOffset, y = 4.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFF9FAFB)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(24.dp)) {
                val scaleX = size.width / 100f
                val scaleY = size.height / 100f
                scale(scaleX, scaleY, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                    drawPath(
                        path = iconPath,
                        color = Color(0xFF111827)
                    )
                }
            }
        }
    }
}

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
