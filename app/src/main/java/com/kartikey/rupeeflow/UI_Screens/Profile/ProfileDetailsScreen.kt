package com.kartikey.rupeeflow.UI_Screens.Profile

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

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
                        } else if (option == "App Update") { // Ranamed Here
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
    val context = LocalContext.current
    var isUpdateAvailable by remember { mutableStateOf(false) }

    // Smart Trigger: Silent background check for updates
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode.toInt() else packageInfo.versionCode
                
                val request = Request.Builder()
                    .url("https://raw.githubusercontent.com/itskartik51/RupeeFlow/main/Updates/version.json")
                    .build()
                val response = OkHttpClient().newCall(request).execute()
                val jsonData = response.body?.string()

                if (jsonData != null) {
                    val json = JSONObject(jsonData)
                    val serverCode = json.optInt("latest_version_code", 0)
                    if (serverCode > currentVersionCode) {
                        withContext(Dispatchers.Main) { isUpdateAvailable = true }
                    }
                }
            } catch (e: Exception) {
                // Ignore silently
            }
        }
    }

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

        // Bounce on Profile Avatar
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
        
        // Smart Animated App Update Row
        ProfileOptionRow(
            customIcon = { AnimatedUpdateIcon(isUpdateAvailable = isUpdateAvailable) },
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

// CUSTOM ANIMATED UPDATE ICON (CSS Equivalent)
@Composable
fun AnimatedUpdateIcon(isUpdateAvailable: Boolean, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    
    if (isUpdateAvailable) {
        val infiniteTransition = rememberInfiniteTransition(label = "update_anim")
        
        // 1. Spinning Border (like animateC)
        val angle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "spin_angle"
        )
        
        // 2. Dropping Line (like animate)
        val dropY by infiniteTransition.animateFloat(
            initialValue = -12f,
            targetValue = 12f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "drop_y"
        )
        
        val dropAlpha by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "drop_alpha"
        )

        Canvas(modifier = modifier.size(24.dp)) {
            rotate(angle) {
                drawArc(
                    color = primaryColor,
                    startAngle = -90f,
                    sweepAngle = 180f, // 180 deg simulate border-top and right
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            
            drawLine(
                color = primaryColor.copy(alpha = dropAlpha),
                start = Offset(size.width / 2, size.height / 2 + dropY - 4.dp.toPx()),
                end = Offset(size.width / 2, size.height / 2 + dropY + 4.dp.toPx()),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    } else {
        // Normal Icon (Green Color)
        Icon(
            imageVector = Icons.Default.SystemUpdate,
            contentDescription = "App Update",
            tint = primaryColor,
            modifier = modifier.size(24.dp)
        )
    }
}

@Composable
private fun ProfileOptionRow(
    iconVector: ImageVector? = null,
    customIcon: (@Composable () -> Unit)? = null,
    title: String, 
    textColor: Color? = null, 
    onClick: () -> Unit
) {
    val finalColor = textColor ?: MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.97f) { onClick() } // Gentle bounce for rows
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (customIcon != null) {
            customIcon()
        } else if (iconVector != null) {
            Icon(imageVector = iconVector, contentDescription = title, tint = finalColor, modifier = Modifier.size(24.dp))
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = finalColor)
    }
    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)
}
