package com.kartikey.rupeeflow.UI_Screens.Home

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.text.style.TextOverflow
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
        
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically, 
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Color.Black)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Contri", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color.Black)
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // ACTION CARDS (Side-by-Side Grid Layout)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ContriGridCard(
                    title = "Create Contri",
                    icon = Icons.Outlined.Add,
                    iconTint = Color(0xFF2E7D32),
                    bgColor = Color(0xFFE8F5E9),
                    modifier = Modifier.weight(1f),
                    onClick = { showCreateDialog = true }
                )
                
                ContriGridCard(
                    title = "Join Contri",
                    icon = Icons.Outlined.GroupAdd,
                    iconTint = Color(0xFF2E7D32),
                    bgColor = Color(0xFFE8F5E9),
                    modifier = Modifier.weight(1f),
                    onClick = { showJoinDialog = true }
                )
            }
            
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
// JOIN CONTRI DIALOG (INSTANT UI - NO ANIMATIONS)
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
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            // NO ANIMATIONS - Instant State Switch
            if (viewState == 0) {
                // --------------------------
                // VIEW 1: OPTIONS
                // --------------------------
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Join Contri", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                    
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().height(80.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp)),
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
                        Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.LightGray))

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

// ==========================================
// PREMIUM GRID CARD (SIDE-BY-SIDE LAYOUT)
// ==========================================
@Composable
fun ContriGridCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "cardScale")

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPressed) 2.dp else 6.dp),
        modifier = modifier
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
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp) // Reduced Box Size
                    .background(bgColor, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp) // Reduced Icon Size
                )
            }
            
            Spacer(modifier = Modifier.width(10.dp))
            
            Text(
                text = title, 
                fontSize = 14.sp, 
                fontWeight = FontWeight.ExtraBold, 
                color = Color.Black,
                maxLines = 1,          // Strictly Single Line
                softWrap = false,      // Prevent line breaking
                overflow = TextOverflow.Ellipsis // Add dots if screen is too small
            )
        }
    }
}
