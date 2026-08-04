package com.kartikey.rupeeflow.UI_Screens.Home

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kartikey.rupeeflow.UI_Screens.bounceClick 
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data Model for Contri Rooms
data class ContriRoomModel(
    val roomName: String,
    val roomCode: String,
    val lastUpdated: String,
    val pin: String = "123456" 
)

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

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Dynamic List fetched directly from Firestore
    var fetchedRooms by remember { mutableStateOf<List<ContriRoomModel>>(contriRooms) }
    var isFetchingRooms by remember { mutableStateOf(false) }

    // REFRESH ANIMATION STATES
    var isRefreshing by remember { mutableStateOf(false) }
    var currentRotation by remember { mutableFloatStateOf(0f) }

    // FETCH ROOMS DIRECTLY FROM FIRESTORE
    val fetchRoomsFromFirestore: suspend () -> Unit = {
        try {
            isFetchingRooms = true
            val db = FirebaseFirestore.getInstance()
            val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
            val roomsList = mutableListOf<ContriRoomModel>()

            if (!userQuery.isEmpty) {
                val userDoc = userQuery.documents[0]
                val currentUserId = userDoc.id
                val userData = userDoc.data ?: emptyMap<String, Any>()
                
                // 1. Fetch room_X fields safely
                val roomKeys = userData.keys.filter { it.startsWith("room_") }
                for (key in roomKeys) {
                    val roomVal = userData[key]?.toString() ?: ""
                    if (roomVal.contains("/")) {
                        val parts = roomVal.split("/")
                        val rName = parts.getOrNull(0)?.trim() ?: "Contri Room"
                        val rCode = parts.getOrNull(1)?.trim() ?: ""
                        val rPin = parts.getOrNull(2)?.trim() ?: "123456"

                        if (rCode.isNotBlank()) {
                            roomsList.add(ContriRoomModel(rName, rCode, "", rPin))
                        }
                    }
                }

                // 2. Fallback Query (Using member_ids)
                val contriQuery = db.collection("Contri").whereArrayContains("member_ids", currentUserId).get().await()
                for (doc in contriQuery.documents) {
                    val code = doc.getString("contri_code") ?: ""
                    if (code.isNotBlank() && roomsList.none { it.roomCode == code }) {
                        val name = doc.getString("contri_name") ?: "Contri Room"
                        val pin = doc.getString("passkey") ?: "123456"
                        roomsList.add(ContriRoomModel(name, code, "", pin))
                    }
                }
            }

            withContext(Dispatchers.Main) {
                fetchedRooms = roomsList
                isFetchingRooms = false
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { 
                isFetchingRooms = false 
            }
        }
    }

    LaunchedEffect(Unit) {
        // Initial Fetch
        fetchRoomsFromFirestore()
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            while (true) {
                delay(16)
                currentRotation += 8f
            }
        } else {
            currentRotation = 0f
        }
    }

    // Screen Routing Logic
    if (showScanner) {
        com.kartikey.rupeeflow.UI_Screens.QR.ScanQRScreen(
            onBackClick = { showScanner = false },
            onQrScanned = { qrValue -> 
                showScanner = false
                if (qrValue.contains("|")) {
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
        // FULL-SCREEN DIALOG OVERLAY
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
                    coroutineScope.launch { fetchRoomsFromFirestore() }
                    onRefresh()
                }
            )
        }
    } else {
        // Main Contri Hub UI
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
                                    fetchRoomsFromFirestore()
                                    delay(800)
                                    isRefreshing = false
                                }
                                onRefresh() 
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

                // Only show loading indicator if list is completely empty
                if (isFetchingRooms && fetchedRooms.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (fetchedRooms.isNotEmpty()) {
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
                        iconTint = MaterialTheme.colorScheme.primary,
                        bgColor = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { showCreateDialog = true }
                    )
                    
                    ContriGridCard(
                        title = "Join Contri",
                        icon = Icons.Outlined.GroupAdd,
                        iconTint = MaterialTheme.colorScheme.primary,
                        bgColor = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { 
                            scannedRoomCode = ""
                            showJoinDialog = true 
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (qrRoomToDisplay != null) {
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
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(qrRoomToDisplay!!.roomName, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Ask your friend to scan to join instantly", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    com.kartikey.rupeeflow.UI_Screens.QR.PremiumQRCode(
                        data = "${qrRoomToDisplay!!.roomCode}|${qrRoomToDisplay!!.pin}|${qrRoomToDisplay!!.roomName}",
                        size = 180.dp
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Room Code", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(qrRoomToDisplay!!.roomCode, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onSurface)
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
                coroutineScope.launch { fetchRoomsFromFirestore() }
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
                coroutineScope.launch { fetchRoomsFromFirestore() }
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
                coroutineScope.launch { fetchRoomsFromFirestore() }
                onRefresh()
            }
        )
    }
}

// ==========================================
// DIRECT AUTO-JOIN DIALOG 
// ==========================================
@Composable
fun AutoJoinContriDialog(
    username: String,
    roomCode: String,
    pin: String,
    roomName: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val db = FirebaseFirestore.getInstance()
                val query = db.collection("Contri")
                    .whereEqualTo("contri_code", roomCode)
                    .get()
                    .await()
                
                if (!query.isEmpty) {
                    val doc = query.documents[0]
                    val dbPasskey = doc.getString("passkey")
                    
                    if (dbPasskey == pin || dbPasskey.isNullOrEmpty()) {
                        val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                        
                        if (!userQuery.isEmpty) {
                            val userDocRef = userQuery.documents[0].reference
                            val currentUserId = userQuery.documents[0].id
                            
                            // Using member_ids for Bulletproof ID tracking
                            doc.reference.update("member_ids", FieldValue.arrayUnion(currentUserId)).await()

                            val userData = userQuery.documents[0].data ?: emptyMap<String, Any>()
                            val existingRooms = userData.keys.filter { it.startsWith("room_") }
                            val maxIndex = existingRooms.mapNotNull { it.removePrefix("room_").toIntOrNull() }.maxOrNull() ?: 0
                            val nextRoomIndex = maxIndex + 1
                            val roomVal = "$roomName / $roomCode / $pin"
                            
                            userDocRef.update("room_$nextRoomIndex", roomVal).await()
                            
                            withContext(Dispatchers.Main) { 
                                Toast.makeText(context, "Joined $roomName!", Toast.LENGTH_SHORT).show()
                                onSuccess() 
                            }
                        } else {
                            withContext(Dispatchers.Main) { 
                                Toast.makeText(context, "Error: User Profile not found!", Toast.LENGTH_LONG).show()
                                onDismiss() 
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) { 
                            Toast.makeText(context, "Invalid Pin!", Toast.LENGTH_LONG).show()
                            onDismiss() 
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) { 
                        Toast.makeText(context, "Room not found!", Toast.LENGTH_LONG).show()
                        onDismiss() 
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                    onDismiss()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = { /* Lock back press */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp), 
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Joining Room...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(roomName, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun ActiveRoomCard(room: ContriRoomModel, onClick: () -> Unit, onQrClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = room.roomName, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Update, contentDescription = "Last Updated", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = formatToDayMonth(room.lastUpdated), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                }
            }
            
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .bounceClick { onQrClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.QrCode2, contentDescription = "Show QR", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
        }
    }
}

fun formatToDayMonth(dateStr: String): String {
    if (dateStr.isEmpty()) return "Newly Created"
    return try {
        val inputFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMMM", Locale.getDefault())
        val date = inputFormat.parse(dateStr)
        if (date != null) outputFormat.format(date) else dateStr
    } catch (e: Exception) {
        dateStr
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateContriDialog(username: String, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var contriName by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Create Room", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    if (!isSubmitting) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                
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
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary, 
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) pin = it },
                    label = { Text("Create Pin") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary, 
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                AnimatedVisibility(visible = pin.isNotEmpty() && pin.length < 6) {
                    Text(
                        text = "Enter 6 digits", 
                        color = MaterialTheme.colorScheme.error, 
                        fontSize = 11.sp, 
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 2.dp),
                        textAlign = TextAlign.Start
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (contriName.isNotBlank() && pin.length == 6) {
                            isSubmitting = true
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val db = FirebaseFirestore.getInstance()
                                    val metaRef = db.collection("System").document("Metadata")
                                    val metaDoc = metaRef.get().await()
                                    
                                    val lastCounter = metaDoc.getLong("last_contri_id") ?: 0L
                                    val nextCounter = lastCounter + 1
                                    metaRef.set(mapOf("last_contri_id" to nextCounter), SetOptions.merge()).await()

                                    val counterStr = String.format(Locale.US, "%03d", nextCounter)
                                    val randomChars = (1..6).map { ('A'..'Z').random() }.joinToString("")
                                    val randomCode = "${randomChars.take(3)}-${(100..999).random()}-${randomChars.takeLast(3)}"
                                    
                                    val docId = "${counterStr}_${contriName.replace(" ", "")}_$randomCode"
                                    val todayStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

                                    val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                                    
                                    if (!userQuery.isEmpty) {
                                        val currentUserId = userQuery.documents[0].id
                                        
                                        val contriData = hashMapOf(
                                            "contri_code" to randomCode,
                                            "contri_date" to todayStr,
                                            "contri_name" to contriName,
                                            "member_ids" to listOf(currentUserId), // Clean ID Based Tracking
                                            "admin_id" to currentUserId,
                                            "passkey" to pin,
                                            "total_group_expense" to 0.0
                                        )
                                        
                                        db.collection("Contri").document(docId).set(contriData).await()
                                        
                                        val userDocRef = userQuery.documents[0].reference
                                        val userData = userQuery.documents[0].data ?: emptyMap<String, Any>()
                                        val existingRooms = userData.keys.filter { it.startsWith("room_") }
                                        val maxIndex = existingRooms.mapNotNull { it.removePrefix("room_").toIntOrNull() }.maxOrNull() ?: 0
                                        val nextRoomIndex = maxIndex + 1
                                        val roomVal = "$contriName / $randomCode / $pin"
                                        userDocRef.update("room_$nextRoomIndex", roomVal).await()
                                        
                                        withContext(Dispatchers.Main) {
                                            isSubmitting = false
                                            Toast.makeText(context, "Room Created!", Toast.LENGTH_SHORT).show()
                                            onSuccess()
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isSubmitting = false
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(context, "Fill details correctly", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    else Text("Create Contri", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinContriDialog(
    username: String, 
    initialScannedCode: String = "", 
    onScanClick: () -> Unit, 
    onDismiss: () -> Unit, 
    onSuccess: () -> Unit
) {
    var viewState by remember { mutableIntStateOf(if (initialScannedCode.isNotBlank()) 1 else 0) }
    
    var roomCode by remember(initialScannedCode) { 
        mutableStateOf(initialScannedCode.replace("-", "").filter { it.isLetterOrDigit() }.uppercase().take(9)) 
    }
    var pin by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            if (viewState == 0) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Join Contri", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().height(80.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                                .bounceClick { onScanClick() },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Scan QR", modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Scan QR", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }

                        Box(modifier = Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                                .bounceClick { viewState = 1 },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Outlined.Keyboard, contentDescription = "Manual", modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Enter Manually", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (!isSubmitting) {
                            IconButton(onClick = { viewState = 0 }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        } else {
                            Spacer(modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text("Enter Details", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.size(24.dp)) 
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = roomCode,
                        onValueChange = { raw ->
                            roomCode = raw.replace("-", "").filter { it.isLetterOrDigit() }.uppercase().take(9)
                        },
                        label = { Text("Enter Contri Code") },
                        placeholder = { Text("ABC-123-XYZ") },
                        singleLine = true,
                        visualTransformation = RoomCodeVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary, 
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) pin = it },
                        label = { Text("Enter Pin") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary, 
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    AnimatedVisibility(visible = pin.isNotEmpty() && pin.length < 6) {
                        Text(
                            text = "Enter 6 digits", 
                            color = MaterialTheme.colorScheme.error, 
                            fontSize = 11.sp, 
                            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 2.dp),
                            textAlign = TextAlign.Start
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (roomCode.length == 9 && pin.length == 6) {
                                isSubmitting = true
                                val formattedCode = roomCode.chunked(3).joinToString("-")
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val db = FirebaseFirestore.getInstance()
                                        val query = db.collection("Contri").whereEqualTo("contri_code", formattedCode).get().await()
                                        
                                        if (!query.isEmpty) {
                                            val doc = query.documents[0]
                                            val dbPasskey = doc.getString("passkey")
                                            
                                            if (dbPasskey == pin || dbPasskey.isNullOrEmpty()) {
                                                val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                                                val roomName = doc.getString("contri_name") ?: "Contri Room"
                                                
                                                if (!userQuery.isEmpty) {
                                                    val currentUserId = userQuery.documents[0].id
                                                    val userDocRef = userQuery.documents[0].reference
                                                    
                                                    doc.reference.update("member_ids", FieldValue.arrayUnion(currentUserId)).await()
                                                    
                                                    val userData = userQuery.documents[0].data ?: emptyMap<String, Any>()
                                                    val existingRooms = userData.keys.filter { it.startsWith("room_") }
                                                    val maxIndex = existingRooms.mapNotNull { it.removePrefix("room_").toIntOrNull() }.maxOrNull() ?: 0
                                                    val nextRoomIndex = maxIndex + 1
                                                    val roomVal = "$roomName / $formattedCode / $pin"
                                                    userDocRef.update("room_$nextRoomIndex", roomVal).await()
                                                    
                                                    withContext(Dispatchers.Main) {
                                                        isSubmitting = false
                                                        Toast.makeText(context, "Joined Successfully!", Toast.LENGTH_SHORT).show()
                                                        onSuccess()
                                                    }
                                                }
                                            } else {
                                                withContext(Dispatchers.Main) {
                                                    isSubmitting = false
                                                    Toast.makeText(context, "Invalid Pin", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                isSubmitting = false
                                                Toast.makeText(context, "Room not found", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isSubmitting = false
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Enter Valid Code & Pin", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        enabled = !isSubmitting
                    ) {
                        if (isSubmitting) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        else Text("Join Contri", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

class RoomCodeVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.take(9)
        val formatted = buildString {
            for (i in raw.indices) {
                append(raw[i])
                if ((i == 2 || i == 5) && i != raw.lastIndex) {
                    append("-")
                }
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset <= 3) return offset
                if (offset <= 6) return offset + 1
                if (offset <= 9) return offset + 2
                return formatted.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset <= 3) return offset
                if (offset <= 7) return (offset - 1).coerceAtLeast(0)
                if (offset <= 11) return (offset - 2).coerceAtLeast(0)
                return raw.length
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

@Composable
fun ContriGridCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.bounceClick { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(bgColor, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(10.dp))
            
            Text(
                text = title, 
                fontSize = 14.sp, 
                fontWeight = FontWeight.ExtraBold, 
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,          
                softWrap = false,      
                overflow = TextOverflow.Ellipsis 
            )
        }
    }
}
