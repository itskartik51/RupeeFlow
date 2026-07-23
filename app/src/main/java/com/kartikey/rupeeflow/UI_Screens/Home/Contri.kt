package com.kartikey.rupeeflow.UI_Screens.Home

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kartikey.rupeeflow.Cloud_Database.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

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
    
    var showScanner by remember { mutableStateOf(false) }
    var scannedRoomCode by remember { mutableStateOf("") }
    var qrRoomToDisplay by remember { mutableStateOf<ContriRoomModel?>(null) }
    var openedRoom by remember { mutableStateOf<ContriRoomModel?>(null) }

    val context = LocalContext.current

    if (showScanner) {
        com.kartikey.rupeeflow.UI_Screens.QR.ScanQRScreen(
            onBackClick = { showScanner = false },
            onQrScanned = { code -> 
                scannedRoomCode = code
                showScanner = false
                showJoinDialog = true
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
                username = username, // <-- Now passing username here securely
                room = openedRoom!!,
                onBackClick = { openedRoom = null },
                onLeaveClick = { 
                    Toast.makeText(context, "Balance check logic coming soon!", Toast.LENGTH_SHORT).show()
                    openedRoom = null 
                }
            )
        }
    } else {
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

                if (contriRooms.isNotEmpty()) {
                    contriRooms.forEach { room ->
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(qrRoomToDisplay!!.roomName, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                    Text("Ask your friend to scan this QR", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(24.dp))
                    com.kartikey.rupeeflow.UI_Screens.QR.PremiumQRCode(data = qrRoomToDisplay!!.roomCode, size = 180.dp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Room Code", fontSize = 11.sp, color = Color.Gray)
                    Text(qrRoomToDisplay!!.roomCode, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, color = Color.Black)
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateContriDialog(
            username = username,
            onDismiss = { showCreateDialog = false },
            onSuccess = { showCreateDialog = false; onRefresh() }
        )
    }

    if (showJoinDialog) {
        JoinContriDialog(
            username = username,
            initialScannedCode = scannedRoomCode,
            onScanClick = { showJoinDialog = false; showScanner = true },
            onDismiss = { showJoinDialog = false },
            onSuccess = { showJoinDialog = false; scannedRoomCode = ""; onRefresh() }
        )
    }
}

@Composable
fun ActiveRoomCard(room: ContriRoomModel, onClick: () -> Unit, onQrClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "cardScale")

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = room.roomName, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Update, contentDescription = "Last Updated", tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = formatToDayMonth(room.lastUpdated), fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                }
            }
            
            IconButton(
                onClick = onQrClick,
                modifier = Modifier.size(44.dp).background(Color(0xFFE8F5E9), shape = CircleShape)
            ) {
                Icon(Icons.Outlined.QrCode2, contentDescription = "Show QR", tint = Color(0xFF2E7D32), modifier = Modifier.size(24.dp))
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Create Room", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                    if (!isSubmitting) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = contriName,
                    onValueChange = { contriName = it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } },
                    label = { Text("Contri Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
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
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                )

                AnimatedVisibility(visible = pin.isNotEmpty() && pin.length < 6) {
                    Text("Enter 6 digits", color = Color.Red, fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 2.dp), textAlign = TextAlign.Start)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (contriName.isNotBlank() && pin.length == 6) {
                            isSubmitting = true
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val jsonBody = JSONObject().apply {
                                        put("action", "create_contri")
                                        put("username", username)
                                        put("room_name", contriName.trim())
                                        put("passkey", pin)
                                    }
                                    val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL)
                                        .post(jsonBody.toString().toRequestBody("application/json".toMediaType())).build()
                                    
                                    val response = OkHttpClient().newCall(request).execute()
                                    val resData = response.body?.string() ?: ""
                                    
                                    withContext(Dispatchers.Main) {
                                        isSubmitting = false
                                        if (resData.contains("success")) {
                                            Toast.makeText(context, "Room Created!", Toast.LENGTH_SHORT).show()
                                            onSuccess()
                                        } else {
                                            val errorMsg = try { JSONObject(resData).optString("message", "Limit Reached") } catch(e:Exception){"Error"}
                                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) { isSubmitting = false; Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show() }
                                }
                            }
                        } else {
                            Toast.makeText(context, "Fill details correctly", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    else Text("Create Contri", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinContriDialog(username: String, initialScannedCode: String = "", onScanClick: () -> Unit, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var viewState by remember { mutableIntStateOf(if (initialScannedCode.isNotBlank()) 1 else 0) }
    var roomCode by remember { mutableStateOf(initialScannedCode) }
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            if (viewState == 0) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Join Contri", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().height(80.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)).clickable { onScanClick() },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Scan QR", modifier = Modifier.size(28.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Scan QR", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                        Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.LightGray))
                        Column(
                            modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)).clickable { viewState = 1 },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Outlined.Keyboard, contentDescription = "Manual", modifier = Modifier.size(28.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Enter Manually", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (!isSubmitting) {
                            IconButton(onClick = { viewState = 0 }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Color.Black)
                            }
                        } else { Spacer(modifier = Modifier.size(24.dp)) }
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
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
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
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                    )

                    AnimatedVisibility(visible = pin.isNotEmpty() && pin.length < 6) {
                        Text("Enter 6 digits", color = Color.Red, fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 2.dp), textAlign = TextAlign.Start)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (roomCode.length >= 11 && pin.length == 6) {
                                isSubmitting = true
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val jsonBody = JSONObject().apply {
                                            put("action", "join_contri")
                                            put("username", username)
                                            put("room_code", roomCode)
                                            put("passkey", pin)
                                        }
                                        val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL)
                                            .post(jsonBody.toString().toRequestBody("application/json".toMediaType())).build()
                                        
                                        val response = OkHttpClient().newCall(request).execute()
                                        val resData = response.body?.string() ?: ""
                                        
                                        withContext(Dispatchers.Main) {
                                            isSubmitting = false
                                            if (resData.contains("success")) {
                                                Toast.makeText(context, "Joined Successfully!", Toast.LENGTH_SHORT).show()
                                                onSuccess()
                                            } else {
                                                val errorMsg = try { JSONObject(resData).optString("message", "Invalid Code/Pin") } catch(e:Exception){"Error"}
                                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) { isSubmitting = false; Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show() }
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Enter Valid Code & Pin", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        enabled = !isSubmitting
                    ) {
                        if (isSubmitting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        else Text("Join Contri", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ContriGridCard(title: String, icon: ImageVector, iconTint: Color, bgColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(42.dp).background(bgColor, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title, 
                fontSize = 14.sp, 
                fontWeight = FontWeight.ExtraBold, 
                color = Color.Black,
                maxLines = 1,          
                softWrap = false,      
                overflow = TextOverflow.Ellipsis 
            )
        }
    }
}
