package com.kartikey.rupeeflow.UI_Screens.Home.Contri

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kartikey.rupeeflow.UI_Screens.Home.Contri.InContri.InsideContriScreen
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ContriScreen(
    username: String,
    contriRooms: List<ContriRoomModel>,
    paddingValues: PaddingValues,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    
    // Scanner, QR & Inside Room States
    var showScanner by remember { mutableStateOf(false) }
    var scannedRoomCode by remember { mutableStateOf("") }
    var qrRoomToDisplay by remember { mutableStateOf<ContriRoomModel?>(null) }
    var openedRoom by remember { mutableStateOf<ContriRoomModel?>(null) }

    // Auto-Join State from QR (Code, Pin, RoomName)
    var autoJoinData by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val fetchedRooms by remember(contriRooms) { mutableStateOf(contriRooms) }

    // REFRESH ANIMATION STATES
    var isRefreshing by remember { mutableStateOf(false) }
    var currentRotation by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            while (true) {
                delay(16)
                currentRotation -= 8f
            }
        } else {
            currentRotation = 0f
        }
    }

    // Screen Routing Logic
    if (showScanner) {
        ScanQRScreen(
            onBackClick = { showScanner = false },
            onQrScanned = { qrValue -> 
                showScanner = false
                if (fetchedRooms.size >= 5) {
                    Toast.makeText(context, "Limit reached! Maximum 5 Contri rooms allowed.", Toast.LENGTH_SHORT).show()
                } else if (qrValue.contains("|")) {
                    val parts = qrValue.split("|")
                    val code = parts.getOrNull(0) ?: ""
                    val pin = parts.getOrNull(1) ?: ""
                    val rName = parts.getOrNull(2) ?: "Contri Room"
                    if (code.isNotBlank() && pin.isNotBlank()) {
                        autoJoinData = Triple(code, pin, rName)
                    } else {
                        scannedRoomCode = qrValue
                        showJoinDialog = true
                    }
                } else {
                    scannedRoomCode = qrValue
                    showJoinDialog = true
                }
            }
        )
    } else if (openedRoom != null) {
        Dialog(
            onDismissRequest = { openedRoom = null },
            properties = DialogProperties(
                dismissOnBackPress = true, 
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false 
            )
        ) {
            InsideContriScreen(
                username = username, 
                room = openedRoom!!,
                onBackClick = { openedRoom = null },
                onLeaveClick = { 
                    openedRoom = null 
                    onRefresh()
                }
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Box(modifier = Modifier.bounceClick { onBackClick() }.padding(4.dp)) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Contri", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = MaterialTheme.colorScheme.onBackground)

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .bounceClick {
                            if (!isRefreshing) {
                                isRefreshing = true
                                coroutineScope.launch {
                                    onRefresh()
                                    delay(1000)
                                    isRefreshing = false
                                }
                            }
                        }
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Sync,
                        contentDescription = "Sync",
                        tint = if (isRefreshing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer { scaleX = -1f }
                            .rotate(currentRotation)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                if (fetchedRooms.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("No Contri Rooms yet. Create or Join one!", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                } else {
                    fetchedRooms.forEach { room ->
                        ActiveRoomCard(
                            room = room, 
                            onClick = { openedRoom = room }, 
                            onQrClick = { qrRoomToDisplay = room }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ContriGridCard(
                        title = "Create Contri",
                        icon = Icons.Outlined.Add,
                        iconTint = MaterialTheme.colorScheme.onSurface,
                        bgColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f),
                        onClick = { 
                            if (fetchedRooms.size >= 5) {
                                Toast.makeText(context, "Limit reached! Maximum 5 Contri rooms allowed.", Toast.LENGTH_SHORT).show()
                            } else {
                                showCreateDialog = true 
                            }
                        }
                    )
                    
                    ContriGridCard(
                        title = "Join Contri",
                        icon = Icons.Outlined.GroupAdd,
                        iconTint = MaterialTheme.colorScheme.onSurface,
                        bgColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f),
                        onClick = { 
                            if (fetchedRooms.size >= 5) {
                                Toast.makeText(context, "Limit reached! Maximum 5 Contri rooms allowed.", Toast.LENGTH_SHORT).show()
                            } else {
                                scannedRoomCode = ""
                                showJoinDialog = true 
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // ==========================================
    // QR DISPLAY DIALOG
    // ==========================================
    if (qrRoomToDisplay != null) {
        val qrPayload = "${qrRoomToDisplay!!.roomCode}|${qrRoomToDisplay!!.pin}|${qrRoomToDisplay!!.roomName}"
        
        Dialog(
            onDismissRequest = { qrRoomToDisplay = null },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = qrRoomToDisplay!!.roomName, 
                        fontSize = 20.sp, 
                        fontWeight = FontWeight.ExtraBold, 
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Ask your friend to scan to join instantly", 
                        fontSize = 12.sp, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    PremiumQRCode(
                        data = qrPayload,
                        size = 185.dp,
                        cornerRadius = 16.dp
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Room Code", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = qrRoomToDisplay!!.roomCode, 
                        fontSize = 17.sp, 
                        fontWeight = FontWeight.ExtraBold, 
                        letterSpacing = 2.sp, 
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .bounceClick {
                                    saveQRToGallery(context, qrPayload, qrRoomToDisplay!!.roomName)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            CustomDownloadIcon(
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .bounceClick {
                                    shareQRCode(
                                        context = context,
                                        data = qrPayload,
                                        roomName = qrRoomToDisplay!!.roomName,
                                        roomCode = qrRoomToDisplay!!.roomCode,
                                        pin = qrRoomToDisplay!!.pin
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.OpenInNew,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (autoJoinData != null) {
        AutoJoinContriDialog(
            username = username,
            roomCode = autoJoinData!!.first,
            pin = autoJoinData!!.second,
            roomName = autoJoinData!!.third,
            onDismiss = { autoJoinData = null },
            onSuccess = {
                autoJoinData = null
                onRefresh()
            }
        )
    }

    if (showCreateDialog) {
        CreateContriDialog(
            username = username,
            onDismiss = { showCreateDialog = false },
            onSuccess = { 
                showCreateDialog = false
                onRefresh()
            }
        )
    }

    if (showJoinDialog) {
        JoinContriDialog(
            username = username,
            initialScannedCode = scannedRoomCode,
            onScanClick = {
                showJoinDialog = false 
                showScanner = true     
            },
            onDismiss = { showJoinDialog = false },
            onSuccess = {
                showJoinDialog = false
                scannedRoomCode = ""
                onRefresh()
            }
        )
    }
}
