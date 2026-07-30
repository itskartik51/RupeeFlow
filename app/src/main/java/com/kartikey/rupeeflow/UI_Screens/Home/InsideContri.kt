package com.kartikey.rupeeflow.UI_Screens.Home

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
import com.kartikey.rupeeflow.UI_Screens.NetworkClient
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// ==========================================
// DATA MODELS
// ==========================================
data class ContriExpense(val itemName: String, val amount: Double, val date: String)
data class MemberLedger(val memberName: String, val totalSpent: Double, val expenses: List<ContriExpense>)
data class Settlement(val from: String, val to: String, val amount: Double)
data class PastCycle(val dateRange: String, val totalAmount: String)
data class RoomDetailsData(val roomName: String, val ledgers: List<MemberLedger>, val totalExpense: Double, val isAdmin: Boolean, val pastCycles: List<PastCycle>, val myName: String)

@Composable
fun InsideContriScreen(
    username: String,
    room: ContriRoomModel,
    onBackClick: () -> Unit,
    onLeaveClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    
    var localRoomName by remember { mutableStateOf(room.roomName) }
    var localRoomPin by remember { mutableStateOf(room.pin) }
    val formattedName = if (localRoomName.length > 10) "${localRoomName.take(10)}..." else localRoomName

    val sharedPreferences = context.getSharedPreferences("RupeeFlowCache", Context.MODE_PRIVATE)
    val cacheKey = "room_data_${room.roomCode}"

    var myName by remember { mutableStateOf("") }
    var ledgers by remember { mutableStateOf<List<MemberLedger>>(emptyList()) }
    var pastCycles by remember { mutableStateOf<List<PastCycle>>(emptyList()) }
    var totalGroupExpense by remember { mutableDoubleStateOf(0.0) }
    var isAdmin by remember { mutableStateOf(false) } 
    var isLoading by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showSettleDialog by remember { mutableStateOf(false) }
    var showNewCycleDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    var memberToRemove by remember { mutableStateOf<String?>(null) }

    val expandedState = remember { mutableStateMapOf<String, Boolean>() }
    val loadingState = remember { mutableStateMapOf<String, Boolean>() }
    val fetchedCycleData = remember { mutableStateMapOf<String, List<MemberLedger>>() }

    // ROTATION ANIMATION FOR SYNC WHEEL
    var currentRotation by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isLoading) {
        if (isLoading) {
            while (true) {
                delay(16)
                currentRotation += 8f
            }
        } else {
            currentRotation = 0f
        }
    }

    LaunchedEffect(room.roomCode, refreshTrigger) {
        val cachedJson = sharedPreferences.getString(cacheKey, null)
        if (cachedJson != null) {
            try {
                val data = parseLedgerData(cachedJson)
                if (data.roomName.isNotEmpty()) localRoomName = data.roomName
                myName = data.myName
                ledgers = data.ledgers
                totalGroupExpense = data.totalExpense
                isAdmin = data.isAdmin
                pastCycles = data.pastCycles
            } catch (e: Exception) { e.printStackTrace() }
        } else {
            isLoading = true
        }

        withContext(Dispatchers.IO) {
            try {
                val jsonBody = JSONObject().apply {
                    put("action", "fetch_room_details")
                    put("room_code", room.roomCode)
                    put("username", username) 
                }
                val request = Request.Builder()
                    .url(Constants.GOOGLE_SHEET_API_URL)
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = NetworkClient.instance.newCall(request).execute()
                val resData = response.body?.string() ?: ""

                withContext(Dispatchers.Main) {
                    if (resData.contains("\"status\":\"success\"")) {
                        sharedPreferences.edit().putString(cacheKey, resData).apply()
                        val data = parseLedgerData(resData)
                        if (data.roomName.isNotEmpty()) localRoomName = data.roomName
                        myName = data.myName
                        ledgers = data.ledgers
                        totalGroupExpense = data.totalExpense
                        isAdmin = data.isAdmin
                        pastCycles = data.pastCycles
                    }
                    isLoading = false 
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).bounceClick { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Color.Black)
                }
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(text = formattedName, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.Black)
                
                // ADMIN / MEMBER BADGE
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .border(1.2.dp, Color(0xFF424242), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isAdmin) "Admin" else "Member", 
                        fontSize = 10.sp, 
                        color = Color(0xFF424242), 
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                if (isAdmin) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).bounceClick { showSettingsDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Outlined.Settings, contentDescription = "Settings", tint = Color.Black)
                    }
                }
                
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).bounceClick { showLeaveDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Outlined.ExitToApp, contentDescription = "Leave Room", tint = Color.Red)
                }
            }
        },
        floatingActionButton = {
            PremiumFloatingButton(onClick = { showAddExpenseDialog = true })
        },
        containerColor = Color(0xFFFAFAFA)
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("₹", fontSize = 22.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = totalGroupExpense.toInt().toString(), 
                                fontSize = 28.sp, 
                                fontWeight = FontWeight.ExtraBold, 
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            
                            Icon(
                                imageVector = Icons.Outlined.Sync,
                                contentDescription = "Sync",
                                tint = if (isLoading) Color(0xFF2E7D32) else Color.Gray,
                                modifier = Modifier
                                    .size(24.dp)
                                    .rotate(currentRotation)
                                    .bounceClick { if (!isLoading) refreshTrigger++ }
                                    .padding(2.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .height(28.dp)
                                    .bounceClick { showSettleDialog = true }
                                    .background(Color(0xFF2E7D32), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Settle-up", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            if (isAdmin) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .bounceClick { showNewCycleDialog = true }
                                        .border(1.2.dp, Color(0xFF424242), RoundedCornerShape(50))
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("New", fontSize = 12.sp, color = Color(0xFF424242), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .bounceClick {
                                        clipboardManager.setText(AnnotatedString("Join my RupeeFlow Contri!\nCode: ${room.roomCode}\nPin: $localRoomPin"))
                                        Toast.makeText(context, "Code Copied!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy, 
                                    contentDescription = "Copy", 
                                    tint = Color.Gray, 
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = room.roomCode, 
                                fontSize = 17.sp, 
                                fontWeight = FontWeight.ExtraBold, 
                                color = Color.Black, 
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Pin: $localRoomPin", 
                            fontSize = 13.sp, 
                            color = Color.Gray, 
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (isLoading && ledgers.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF2E7D32))
                }
            } else if (ledgers.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp).padding(top = 40.dp), contentAlignment = Alignment.TopCenter) {
                    Text("No expenses yet. Tap + to add!", color = Color.Gray, fontWeight = FontWeight.Medium)
                }
            } else {
                DynamicLedgerView(ledgers)
            }

            if (pastCycles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Past Cycles", 
                    fontSize = 15.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = Color.Gray, 
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    pastCycles.forEach { cycle ->
                        val isExpanded = expandedState[cycle.dateRange] == true
                        val isFetching = loadingState[cycle.dateRange] == true
                        val cycleLedger = fetchedCycleData[cycle.dateRange]

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .animateContentSize() 
                                .bounceClick {
                                    if (isExpanded) {
                                        expandedState[cycle.dateRange] = false
                                    } else {
                                        expandedState[cycle.dateRange] = true
                                        if (cycleLedger == null) {
                                            loadingState[cycle.dateRange] = true
                                            coroutineScope.launch(Dispatchers.IO) {
                                                try {
                                                    val reqBody = JSONObject().apply {
                                                        put("action", "fetch_past_cycle")
                                                        put("room_code", room.roomCode)
                                                        put("date_range", cycle.dateRange)
                                                    }
                                                    val request = Request.Builder()
                                                        .url(Constants.GOOGLE_SHEET_API_URL)
                                                        .post(reqBody.toString().toRequestBody("application/json".toMediaType()))
                                                        .build()
                                                    val response = NetworkClient.instance.newCall(request).execute()
                                                    val resStr = response.body?.string() ?: ""

                                                    withContext(Dispatchers.Main) {
                                                        if (resStr.contains("\"status\":\"success\"")) {
                                                            fetchedCycleData[cycle.dateRange] = parsePastCycleMembersOnly(resStr)
                                                        } else {
                                                            Toast.makeText(context, "Error fetching cycle", Toast.LENGTH_SHORT).show()
                                                        }
                                                        loadingState[cycle.dateRange] = false
                                                    }
                                                } catch (e: Exception) {
                                                    withContext(Dispatchers.Main) { 
                                                        loadingState[cycle.dateRange] = false 
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(1.5.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = cycle.dateRange, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isFetching) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF2E7D32), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text(text = cycle.totalAmount, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2E7D32))
                                    }
                                }

                                if (isExpanded) {
                                    if (cycleLedger != null && cycleLedger.isNotEmpty()) {
                                        HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        DynamicLedgerView(cycleLedger)
                                        Spacer(modifier = Modifier.height(12.dp))
                                    } else if (!isFetching) {
                                        Text("No data available.", color = Color.Gray, modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // ==========================================
        // ADMIN SETTINGS DIALOG
        // ==========================================
        if (showSettingsDialog) {
            var editName by remember { mutableStateOf(localRoomName) }
            var editPin by remember { mutableStateOf(localRoomPin) }
            var isEditingName by remember { mutableStateOf(false) }
            var isEditingPin by remember { mutableStateOf(false) }

            Dialog(onDismissRequest = { showSettingsDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Room Settings", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = editName,
                            onValueChange = { 
                                editName = it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() }
                            },
                            label = { Text("Contri Name", fontSize = 13.sp) },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            singleLine = true,
                            readOnly = !isEditingName,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if(isEditingName) Color(0xFF2E7D32) else Color.LightGray, 
                                unfocusedBorderColor = Color.LightGray
                            ),
                            trailingIcon = {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).bounceClick { isEditingName = !isEditingName },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = if (isEditingName) Color(0xFF2E7D32) else Color.Gray)
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = editPin,
                            onValueChange = { newValue -> 
                                if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                                    editPin = newValue
                                }
                            },
                            label = { Text("Contri Pin", fontSize = 13.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            readOnly = !isEditingPin,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if(isEditingPin) Color(0xFF2E7D32) else Color.LightGray, 
                                unfocusedBorderColor = Color.LightGray
                            ),
                            trailingIcon = {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).bounceClick { isEditingPin = !isEditingPin },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = if (isEditingPin) Color(0xFF2E7D32) else Color.Gray)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Manage Members", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 170.dp)
                                .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFAFAFA))
                        ) {
                            if (ledgers.isNotEmpty()) {
                                val adminName = ledgers[0].memberName
                                LazyColumn {
                                    itemsIndexed(ledgers) { index, member ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val dispName = if (member.memberName.length > 10) member.memberName.take(10) + "..." else member.memberName
                                            Text(text = dispName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                                            
                                            if (member.memberName == adminName) {
                                                Text("Admin", fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                            } else {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                                                    contentDescription = "Remove",
                                                    tint = Color.Red,
                                                    modifier = Modifier.size(24.dp).bounceClick { memberToRemove = member.memberName }
                                                )
                                            }
                                        }
                                        if (index < ledgers.size - 1) {
                                            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                                        }
                                    }
                                }
                            } else {
                                Text("No members found.", modifier = Modifier.padding(16.dp), color = Color.Gray, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Cancel", 
                                color = Color.Gray, 
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.bounceClick { showSettingsDialog = false }.padding(8.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Box(
                                modifier = Modifier
                                    .bounceClick { 
                                        if (editName.isNotBlank() && editPin.length == 6) {
                                            showSettingsDialog = false
                                            localRoomName = editName
                                            localRoomPin = editPin
                                            Toast.makeText(context, "Saving settings...", Toast.LENGTH_SHORT).show()
                                            
                                            CoroutineScope(Dispatchers.IO).launch {
                                                try {
                                                    val reqBody = JSONObject().apply {
                                                        put("action", "edit_contri_room")
                                                        put("username", username)
                                                        put("room_code", room.roomCode)
                                                        put("new_name", editName)
                                                        put("new_pin", editPin)
                                                    }
                                                    val request = Request.Builder()
                                                        .url(Constants.GOOGLE_SHEET_API_URL)
                                                        .post(reqBody.toString().toRequestBody("application/json".toMediaType()))
                                                        .build()

                                                    val response = NetworkClient.instance.newCall(request).execute()
                                                    val resStr = response.body?.string() ?: ""

                                                    withContext(Dispatchers.Main) {
                                                        if (resStr.contains("\"status\":\"success\"")) {
                                                            sharedPreferences.edit().remove(cacheKey).apply()
                                                            refreshTrigger++
                                                        }
                                                    }
                                                } catch (e: Exception) {}
                                            }
                                        } else {
                                            Toast.makeText(context, "Invalid Name or Pin (must be 6 digits)", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .background(Color(0xFF2E7D32), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) { 
                                Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold) 
                            }
                        }
                    }
                }
            }
        }

        // CONFIRMATION DIALOG FOR REMOVING A MEMBER
        if (memberToRemove != null) {
            AlertDialog(
                onDismissRequest = { memberToRemove = null },
                title = { Text("Remove Member?", fontWeight = FontWeight.ExtraBold, color = Color.Black) },
                text = { Text("Are you sure you want to kick '$memberToRemove' from this room? Unka saara individual share room se zero ho jayega.", color = Color.DarkGray) },
                containerColor = Color.White,
                confirmButton = {
                    TextButton(
                        onClick = { 
                            val target = memberToRemove!!
                            
                            // OPTIMISTIC UI
                            memberToRemove = null
                            showSettingsDialog = false
                            Toast.makeText(context, "Removing $target...", Toast.LENGTH_SHORT).show()
                            
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val jsonBody = JSONObject().apply {
                                        put("action", "leave_contri")
                                        put("username", username)
                                        put("room_code", room.roomCode)
                                        put("target_name", target) 
                                    }
                                    val request = Request.Builder()
                                        .url(Constants.GOOGLE_SHEET_API_URL)
                                        .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                                        .build()

                                    val response = NetworkClient.instance.newCall(request).execute()
                                    val resData = response.body?.string() ?: ""

                                    withContext(Dispatchers.Main) {
                                        if (resData.contains("\"status\":\"success\"")) {
                                            sharedPreferences.edit().remove(cacheKey).apply()
                                            refreshTrigger++ 
                                        }
                                    }
                                } catch (e: Exception) {}
                            }
                        }
                    ) { Text("Remove", color = Color.Red, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { memberToRemove = null }) { Text("Cancel", color = Color.Gray, fontWeight = FontWeight.Bold) }
                }
            )
        }

        // ALL OTHER POPUPS
        if (showSettleDialog) {
            SettleUpDialog(myName = myName, ledgers = ledgers, totalExpense = totalGroupExpense, onDismiss = { showSettleDialog = false })
        }

        if (showNewCycleDialog) {
            AlertDialog(
                onDismissRequest = { showNewCycleDialog = false },
                title = { Text("Start New Cycle?", fontWeight = FontWeight.ExtraBold, color = Color.Black) },
                text = { Text("Once created, users cannot change this. It will be saved and calculations will restart from zero.", color = Color.DarkGray) },
                containerColor = Color.White,
                confirmButton = {
                    TextButton(
                        onClick = { 
                            // OPTIMISTIC UI
                            showNewCycleDialog = false
                            Toast.makeText(context, "Starting new cycle...", Toast.LENGTH_SHORT).show()
                            
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val jsonBody = JSONObject().apply {
                                        put("action", "start_new_cycle")
                                        put("username", username)
                                        put("room_code", room.roomCode)
                                    }
                                    val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(jsonBody.toString().toRequestBody("application/json".toMediaType())).build()
                                    NetworkClient.instance.newCall(request).execute()
                                    withContext(Dispatchers.Main) {
                                        sharedPreferences.edit().remove(cacheKey).apply()
                                        refreshTrigger++ 
                                    }
                                } catch (e: Exception) {}
                            }
                        }
                    ) { Text("New", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold) }
                },
                dismissButton = { TextButton(onClick = { showNewCycleDialog = false }) { Text("Cancel", color = Color.Gray, fontWeight = FontWeight.Bold) } }
            )
        }

        if (showLeaveDialog) {
            AlertDialog(
                onDismissRequest = { showLeaveDialog = false },
                title = { Text("Leave Room?", fontWeight = FontWeight.ExtraBold, color = Color.Black) },
                text = { Text("Are you sure you want to leave this Contri room? This action cannot be undone.", color = Color.DarkGray) },
                containerColor = Color.White,
                confirmButton = {
                    TextButton(
                        onClick = { 
                            // OPTIMISTIC UI
                            showLeaveDialog = false
                            Toast.makeText(context, "Leaving room...", Toast.LENGTH_SHORT).show()
                            
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val jsonBody = JSONObject().apply {
                                        put("action", "leave_contri")
                                        put("username", username)
                                        put("room_code", room.roomCode)
                                    }
                                    val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(jsonBody.toString().toRequestBody("application/json".toMediaType())).build()
                                    NetworkClient.instance.newCall(request).execute()
                                    withContext(Dispatchers.Main) {
                                        sharedPreferences.edit().remove(cacheKey).apply()
                                        onLeaveClick()
                                    }
                                } catch (e: Exception) {}
                            }
                        }
                    ) { Text("Leave", color = Color.Red, fontWeight = FontWeight.Bold) }
                },
                dismissButton = { TextButton(onClick = { showLeaveDialog = false }) { Text("Cancel", color = Color.Gray, fontWeight = FontWeight.Bold) } }
            )
        }

        if (showAddExpenseDialog) {
            AddContriExpenseDialog(
                onDismiss = { showAddExpenseDialog = false },
                onAdd = { title, dateMillis, amount ->
                    
                    // OPTIMISTIC UI
                    showAddExpenseDialog = false
                    Toast.makeText(context, "Adding expense...", Toast.LENGTH_SHORT).show()
                    
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            val jsonBody = JSONObject().apply {
                                put("action", "add_contri_expense")
                                put("username", username)
                                put("room_code", room.roomCode)
                                put("date", sdf.format(Date(dateMillis)))
                                put("item_name", title)
                                put("amount", amount)
                            }
                            val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(jsonBody.toString().toRequestBody("application/json".toMediaType())).build()
                            NetworkClient.instance.newCall(request).execute()
                            withContext(Dispatchers.Main) {
                                sharedPreferences.edit().remove(cacheKey).apply()
                                refreshTrigger++ 
                            }
                        } catch (e: Exception) {}
                    }
                }
            )
        }
    }
}

// ==========================================
// REUSABLE UI COMPONENT: DYNAMIC LEDGER VIEW
// ==========================================
@Composable
fun DynamicLedgerView(ledgers: List<MemberLedger>) {
    val memberCount = ledgers.size
    val isScrollable = memberCount > 3
    val fixedColumnWidth = 110.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (isScrollable) it.horizontalScroll(rememberScrollState()) else it }
            .padding(horizontal = 16.dp)
    ) {
        Row(modifier = if (!isScrollable) Modifier.fillMaxWidth() else Modifier) {
            ledgers.forEach { ledger ->
                Column(
                    modifier = if (isScrollable) Modifier.width(fixedColumnWidth) else Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = ledger.memberName, 
                        fontSize = 15.sp, 
                        fontWeight = FontWeight.ExtraBold, 
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "₹${ledger.totalSpent.toInt()}", 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }

        val dividerModifier = if (isScrollable) Modifier.width(fixedColumnWidth * memberCount) else Modifier.fillMaxWidth()
        HorizontalDivider(modifier = dividerModifier.padding(vertical = 8.dp), thickness = 1.dp, color = Color.LightGray)

        Row(modifier = if (!isScrollable) Modifier.fillMaxWidth() else Modifier) {
            ledgers.forEach { ledger ->
                Column(
                    modifier = if (isScrollable) Modifier.width(fixedColumnWidth) else Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ledger.expenses.forEach { expense ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 10.dp)) {
                            Text(text = expense.itemName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(1.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "₹${expense.amount.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = expense.date, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// ALGORITHM & SETTLE UP DIALOG
// ==========================================
fun calculateUserSettlements(ledgers: List<MemberLedger>, totalExpense: Double): Pair<Double, List<Settlement>> {
    if (ledgers.isEmpty()) return Pair(0.0, emptyList())
    val perPerson = totalExpense / ledgers.size
    val balances = mutableMapOf<String, Double>()
    ledgers.forEach { balances[it.memberName] = it.totalSpent - perPerson }
    
    val myNetBalance = 0.0 
    val settlements = mutableListOf<Settlement>()
    val debtors = balances.filter { it.value < -0.01 }.toMutableMap()
    val creditors = balances.filter { it.value > 0.01 }.toMutableMap()
    
    while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
        val debtor = debtors.keys.first()
        val creditor = creditors.keys.first()
        val debtAmount = abs(debtors[debtor]!!)
        val creditAmount = creditors[creditor]!!
        val settledAmount = minOf(debtAmount, creditAmount)
        
        settlements.add(Settlement(from = debtor, to = creditor, amount = settledAmount))
        
        debtors[debtor] = debtors[debtor]!! + settledAmount
        if (debtors[debtor]!! > -0.01) debtors.remove(debtor)
        creditors[creditor] = creditors[creditor]!! - settledAmount
        if (creditors[creditor]!! < 0.01) creditors.remove(creditor)
    }
    return Pair(myNetBalance, settlements)
}

@Composable
fun SettleUpDialog(myName: String, ledgers: List<MemberLedger>, totalExpense: Double, onDismiss: () -> Unit) {
    val (_, allSettlements) = calculateUserSettlements(ledgers, totalExpense)
    
    val sortedSettlements = allSettlements.sortedWith(
        compareByDescending<Settlement> { it.from == myName || it.to == myName }
        .thenByDescending { it.amount }
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(8.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Total: ₹${totalExpense.toInt()}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.LightGray, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    if (sortedSettlements.isEmpty()) {
                        Text("No pending payments.", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    } else {
                        sortedSettlements.forEach { settlement ->
                            
                            val fromText = if (settlement.from == myName) "You" else settlement.from
                            val toText = if (settlement.to == myName) "You" else settlement.to

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), 
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$fromText pay $toText", 
                                    fontSize = 14.sp, 
                                    fontWeight = if (fromText == "You" || toText == "You") FontWeight.ExtraBold else FontWeight.SemiBold, 
                                    color = Color.Black
                                )
                                Text(
                                    text = "₹${settlement.amount.toInt()}", 
                                    fontSize = 15.sp, 
                                    fontWeight = FontWeight.ExtraBold, 
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.LightGray, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .bounceClick { onDismiss() }
                        .background(Color(0xFF2E7D32), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Dismiss", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

// ==========================================
// ADD EXPENSE POPUP
// ==========================================
@Composable
fun AddContriExpenseDialog(onDismiss: () -> Unit, onAdd: (String, Long, Double) -> Unit) {
    var expenseTitle by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dateMillis by remember { mutableStateOf<Long?>(System.currentTimeMillis()) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(dismissOnClickOutside = false)) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(8.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Add New Expense", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = expenseTitle,
                    onValueChange = { expenseTitle = it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } },
                    label = { Text("Expense Title", fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    com.kartikey.rupeeflow.UI_Screens.CustomDatePicker(label = "Date", selectedDateMillis = dateMillis, onDateSelected = { dateMillis = it }, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) amount = it },
                        label = { Text("Amount", fontSize = 13.sp) },
                        prefix = { Text("₹ ", color = Color.Black, fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Cancel", 
                        color = Color.Gray, 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.bounceClick { onDismiss() }.padding(8.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .bounceClick { 
                                val amt = amount.toDoubleOrNull()
                                if (expenseTitle.isNotBlank() && amt != null && dateMillis != null) onAdd(expenseTitle, dateMillis!!, amt)
                            }
                            .background(Color(0xFF2E7D32), RoundedCornerShape(10.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) { 
                        Text("Add Expense", color = Color.White, fontWeight = FontWeight.Bold) 
                    }
                }
            }
        }
    }
}

// ==========================================
// JSON PARSERS
// ==========================================
fun parseLedgerData(jsonString: String): RoomDetailsData {
    val ledgers = mutableListOf<MemberLedger>()
    val pastCycles = mutableListOf<PastCycle>()
    var totalGroupExp = 0.0
    var isAdmin = false
    var rName = ""
    var myName = ""
    try {
        val root = JSONObject(jsonString)
        totalGroupExp = root.optDouble("total_group_expense", 0.0)
        isAdmin = root.optBoolean("is_admin", false)
        rName = root.optString("room_name", "")
        myName = root.optString("my_name", "")
        
        val membersArray = root.optJSONArray("members")
        if (membersArray != null) {
            for (i in 0 until membersArray.length()) {
                val memberObj = membersArray.optJSONObject(i) ?: continue
                val name = memberObj.optString("name", "Unknown")
                val totalSpent = memberObj.optDouble("total_spent", 0.0)
                val expensesList = mutableListOf<ContriExpense>()
                val expensesArray = memberObj.optJSONArray("expenses")
                if (expensesArray != null) {
                    for (j in 0 until expensesArray.length()) {
                        val expObj = expensesArray.optJSONObject(j) ?: continue
                        expensesList.add(ContriExpense(itemName = expObj.optString("item_name", ""), amount = expObj.optDouble("amount", 0.0), date = expObj.optString("date", "")))
                    }
                }
                ledgers.add(MemberLedger(name, totalSpent, expensesList))
            }
        }
        
        val pastCyclesArray = root.optJSONArray("past_cycles")
        if (pastCyclesArray != null) {
            for (i in 0 until pastCyclesArray.length()) {
                val cycleObj = pastCyclesArray.optJSONObject(i) ?: continue
                pastCycles.add(PastCycle(dateRange = cycleObj.optString("date_range", ""), totalAmount = cycleObj.optString("total_amount", "₹0")))
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
    return RoomDetailsData(rName, ledgers, totalGroupExp, isAdmin, pastCycles, myName)
}

fun parsePastCycleMembersOnly(jsonString: String): List<MemberLedger> {
    val ledgers = mutableListOf<MemberLedger>()
    try {
        val root = JSONObject(jsonString)
        val membersArray = root.optJSONArray("members")
        if (membersArray != null) {
            for (i in 0 until membersArray.length()) {
                val memberObj = membersArray.optJSONObject(i) ?: continue
                val name = memberObj.optString("name", "Unknown")
                val totalSpent = memberObj.optDouble("total_spent", 0.0)
                val expensesList = mutableListOf<ContriExpense>()
                val expensesArray = memberObj.optJSONArray("expenses")
                if (expensesArray != null) {
                    for (j in 0 until expensesArray.length()) {
                        val expObj = expensesArray.optJSONObject(j) ?: continue
                        expensesList.add(ContriExpense(itemName = expObj.optString("item_name", ""), amount = expObj.optDouble("amount", 0.0), date = expObj.optString("date", "")))
                    }
                }
                ledgers.add(MemberLedger(name, totalSpent, expensesList))
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
    return ledgers
}

// ==========================================
// PREMIUM BOUNCE FLOATING BUTTON (+)
// ==========================================
@Composable
fun PremiumFloatingButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .bounceClick { onClick() }
            .background(Color(0xFF2E7D32), shape = CircleShape),
        contentAlignment = Alignment.Center
    ) { 
        Icon(imageVector = Icons.Outlined.Add, contentDescription = "Add Expense", tint = Color.White, modifier = Modifier.size(28.dp)) 
    }
}
