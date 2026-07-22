package com.kartikey.rupeeflow.UI_Screens.Home

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InsideContriScreen(
    room: ContriRoomModel,
    onBackClick: () -> Unit,
    onLeaveClick: () -> Unit,
    onAddClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    // 10-Character Truncation Logic
    val formattedName = if (room.roomName.length > 10) "${room.roomName.take(10)}..." else room.roomName

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .statusBarsPadding(),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ==========================================
            // GREY INFO CARD (Total Expense & Code)
            // ==========================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT SIDE: Total Amount
                    Column {
                        Text("Total Expense", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("₹", fontSize = 20.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("0", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black) // DB connect hone par dynamically update hoga
                        }
                    }

                    // RIGHT SIDE: Code, Pin & Copy Icon
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Texts are Left-Aligned inside this block
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = room.roomCode, 
                                fontSize = 15.sp, 
                                fontWeight = FontWeight.ExtraBold, 
                                color = Color.Black, 
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Pin: ${room.pin}", 
                                fontSize = 12.sp, 
                                color = Color.Gray, 
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // Copy Icon
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString("Join my RupeeFlow Contri!\nCode: ${room.roomCode}\nPin: ${room.pin}"))
                                Toast.makeText(context, "Code Copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White, shape = CircleShape)
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = Color.Black, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
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
