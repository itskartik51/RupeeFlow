package com.kartikey.rupeeflow.UI_Screens.Home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ContriScreen(
    paddingValues: PaddingValues,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(Color(0xFFFAFAFA)) // Premium subtle background
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // Base Header with Back Button
        Row(
            verticalAlignment = Alignment.CenterVertically, 
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Color.Black)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Contri Hub", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color.Black)
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Scrollable Content Area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // ==========================================
            // ACTIVE ROOMS SECTION
            // ==========================================
            // Yahan par hum future me user ke active rooms ki list (loop) lagayenge.
            // Jab room data aayega, toh wo automatically sabse upar dikhega aur 
            // niche wale dono Action Cards natural tarike se scroll down ho jayenge.
            
            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // ACTION CARDS (Create & Join)
            // ==========================================
            ContriActionCard(
                title = "Create New Room",
                subtitle = "Start a new split-expense group with your friends.",
                icon = Icons.Outlined.Add,
                iconTint = Color(0xFF2E7D32),
                bgColor = Color(0xFFE8F5E9),
                onClick = {
                    // TODO: Create Room UI logic will trigger here
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ContriActionCard(
                title = "Join a Room",
                subtitle = "Have a Room Code? Scan QR or enter passkey to join.",
                icon = Icons.Outlined.GroupAdd,
                iconTint = Color(0xFF0277BD),
                bgColor = Color(0xFFE1F5FE),
                onClick = {
                    // TODO: Join Room UI logic will trigger here
                }
            )
            
            Spacer(modifier = Modifier.height(40.dp)) // Bottom breathing space
        }
    }
}

// ==========================================
// CUSTOM COMPONENT: PREMIUM ACTION CARD
// ==========================================
@Composable
fun ContriActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    bgColor: Color,
    onClick: () -> Unit
) {
    // Smart Interaction: Touch scale animation for physical button feel
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "cardScale")

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPressed) 2.dp else 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onClick()
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(bgColor, shape = RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
