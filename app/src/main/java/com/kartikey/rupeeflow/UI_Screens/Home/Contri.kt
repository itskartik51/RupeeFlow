package com.kartikey.rupeeflow.UI_Screens.Home

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ContriScreen(
    paddingValues: PaddingValues,
    onBackClick: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(Color(0xFFFAFAFA))
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            ContriActionCard(
                title = "Create New Room",
                subtitle = "Start a new split-expense group with your friends.",
                icon = Icons.Outlined.Add,
                iconTint = Color(0xFF2E7D32),
                bgColor = Color(0xFFE8F5E9),
                onClick = { showCreateDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ContriActionCard(
                title = "Join a Room",
                subtitle = "Have a Room Code? Scan QR or enter passkey to join.",
                icon = Icons.Outlined.GroupAdd,
                iconTint = Color(0xFF2E7D32),
                bgColor = Color(0xFFE8F5E9),
                onClick = { showJoinDialog = true }
            )
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showCreateDialog) {
        CreateContriDialog(onDismiss = { showCreateDialog = false })
    }

    if (showJoinDialog) {
        JoinContriDialog(onDismiss = { showJoinDialog = false })
    }
}

// ==========================================
// CREATE CONTRI DIALOG
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateContriDialog(onDismiss: () -> Unit) {
    var contriName by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Create Room", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = contriName,
                    onValueChange = { 
                        contriName = it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } 
                    },
                    label = { Text("Contri Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) pin = it },
                    label = { Text("Create Pin") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                AnimatedVisibility(visible = pin.isNotEmpty() && pin.length < 6) {
                    Text(
                        text = "Enter 6 digits", 
                        color = Color.Red, 
                        fontSize = 11.sp, 
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 2.dp),
                        textAlign = TextAlign.Start
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { /* TODO: Connect to backend handleCreateContri */ },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Create Contri", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// JOIN CONTRI DIALOG (60FPS ANIMATED UI)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinContriDialog(onDismiss: () -> Unit) {
    var viewState by remember { mutableIntStateOf(0) }
    
    var roomCode by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        // animateContentSize added back with matched 250ms duration for jitter-free resize
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = tween(250)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            AnimatedContent(
                targetState = viewState,
                transitionSpec = {
                    // 33% Slide (it/3) + Fade transition for buttery smooth 60fps feel
                    if (targetState == 1) {
                        (slideInHorizontally(animationSpec = tween(250)) { it / 3 } + fadeIn(tween(250))).togetherWith(
                            slideOutHorizontally(animationSpec = tween(250)) { -it / 3 } + fadeOut(tween(250))
                        )
                    } else {
                        (slideInHorizontally(animationSpec = tween(250)) { -it / 3 } + fadeIn(tween(250))).togetherWith(
                            slideOutHorizontally(animationSpec = tween(250)) { it / 3 } + fadeOut(tween(250))
                        )
                    }
                }, label = "join_animation"
            ) { state ->
                if (state == 0) {
                    // --------------------------
                    // VIEW 1: OPTIONS
                    // --------------------------
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Join Room", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                        Text("Choose how you want to join", fontSize = 12.sp, color = Color.Gray)
                        
                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().height(90.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // QR SCAN OPTION
                            Column(
                                modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)).clickable { 
                                    // TODO: QR Scanner trigger
                                },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Scan QR", modifier = Modifier.size(28.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Scan QR", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }

                            // DIVIDER
                            Box(modifier = Modifier.width(1.dp).height(50.dp).background(Color.LightGray))

                            // MANUAL OPTION (Black Theme)
                            Column(
                                modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)).clickable { 
                                    viewState = 1 // Switch to Manual
                                },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Outlined.Keyboard, contentDescription = "Manual", modifier = Modifier.size(28.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Enter Manually", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                } else {
                    // --------------------------
                    // VIEW 2: MANUAL ENTRY FORM
                    // --------------------------
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewState = 0 }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Color.Black)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text("Enter Details", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                            Spacer(modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.size(24.dp)) 
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = roomCode,
                            onValueChange = { raw ->
                                val clean = raw.replace("-", "").filter { it.isLetterOrDigit() }.uppercase().take(9)
                                val formatted = clean.chunked(3).joinToString("-")
                                roomCode = formatted
                            },
                            label = { Text("Enter Contri Code") },
                            placeholder = { Text("ABC-123-XYZ") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = pin,
                            onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) pin = it },
                            label = { Text("Enter Pin") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        AnimatedVisibility(visible = pin.isNotEmpty() && pin.length < 6) {
                            Text(
                                text = "Enter 6 digits", 
                                color = Color.Red, 
                                fontSize = 11.sp, 
                                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 2.dp),
                                textAlign = TextAlign.Start
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Green Button Theme
                        Button(
                            onClick = { /* TODO: Connect to backend handleJoinContri */ },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Text("Join Contri", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// PREMIUM ACTION CARD
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
                Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subtitle, fontSize = 13.sp, color = Color.Gray, lineHeight = 18.sp)
            }
        }
    }
}
