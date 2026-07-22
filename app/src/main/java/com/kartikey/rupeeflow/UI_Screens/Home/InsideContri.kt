package com.kartikey.rupeeflow.UI_Screens.Home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InsideContriScreen(
    roomName: String,
    onBackClick: () -> Unit,
    onLeaveClick: () -> Unit,
    onAddClick: () -> Unit
) {
    // 10-Character Truncation Logic
    val formattedName = if (roomName.length > 10) "${roomName.take(10)}..." else roomName

    // Using Scaffold here automatically gives us the Full-Screen UI and hides MainScreen's bottom bar
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .statusBarsPadding(), // Keeps header safe from mobile notch/status bar
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Color.Black)
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = formattedName,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Leave Room Icon
                IconButton(onClick = onLeaveClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ExitToApp, 
                        contentDescription = "Leave Room", 
                        tint = Color.Red
                    )
                }
            }
        },
        floatingActionButton = {
            PremiumFloatingButton(onClick = onAddClick)
        },
        containerColor = Color(0xFFFAFAFA)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Aapka middle content (Tabs aur Expenses list) yahan aayega future me
        }
    }
}

// ==========================================
// PREMIUM BOUNCE FLOATING BUTTON
// ==========================================
@Composable
fun PremiumFloatingButton(onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.85f else 1f, label = "fabScale")

    Box(
        modifier = Modifier
            .scale(scale)
            .size(56.dp)
            .background(Color(0xFF2E7D32), shape = CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = "Add Expense",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}
