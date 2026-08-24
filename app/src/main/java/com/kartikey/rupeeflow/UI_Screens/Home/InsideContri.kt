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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// ==========================================
// DATA MODELS
// ==========================================
data class ContriExpense(val itemName: String, val amount: Double, val date: String)
data class MemberLedger(val userId: String, val memberName: String, val totalSpent: Double, val expenses: List<ContriExpense>)
data class Settlement(val from: String, val to: String, val amount: Double)
data class PastCycle(val dateRange: String, val totalAmount: String)

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

    var currentUserId by remember { mutableStateOf("") }
    var ledgers by remember { mutableStateOf<List<MemberLedger>>(emptyList()) }
    var pastCycles by remember { mutableStateOf<List<PastCycle>>(emptyList()) }
    var totalGroupExpense by remember { mutableDoubleStateOf(0.0) }
    var isAdmin by remember { mutableStateOf(false) } 
    var isSyncing by remember { mutableStateOf(false) } 
    var isLoading by remember { mutableStateOf(true) } 
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var actionProcessing by remember { mutableStateOf(false) }
    
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showSettleDialog by remember { mutableStateOf(false) }
    var showNewCycleDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    var memberToRemove by remember { mutableStateOf<MemberLedger?>(null) }

    val expandedState = remember { mutableStateMapOf<String, Boolean>() }
    val loadingState = remember { mutableStateMapOf<String, Boolean>() }
    val fetchedCycleData = remember { mutableStateMapOf<String, List<MemberLedger>>() }

    // ROTATION ANIMATION FOR SYNC WHEEL
    var currentRotation by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isSyncing || actionProcessing) {
        if (isSyncing || actionProcessing) {
            while (true) {
                delay(16)
                currentRotation += 8f
            }
        } else {
            currentRotation = 0f
        }
    }

    LaunchedEffect(room.roomCode, refreshTrigger) {
        if (ledgers.isEmpty()) {
            isLoading = true
        } else {
            isSyncing = true
        }

        withContext(Dispatchers.IO) {
            try {
                val db = FirebaseFirestore.getInstance()
                
                val myUserQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                if (!myUserQuery.isEmpty) {
                    currentUserId = myUserQuery.documents[0].id
                }

                val contriQuery = db.collection("Contri").whereEqualTo("contri_code", room.roomCode).get().await()
                
                if (!contriQuery.isEmpty) {
                    val contriDoc = contriQuery.documents[0]
                    val adminId = contriDoc.getString("admin_id") ?: ""
                    
                    val memberIds = (contriDoc.get("member_ids") as? List<*>)?.map { it.toString() } ?: emptyList()
                    
                    withContext(Dispatchers.Main) {
                        localRoomName = contriDoc.getString("contri_name") ?: room.roomName
                        localRoomPin = contriDoc.getString("passkey") ?: room.pin
                        isAdmin = adminId == currentUserId
                        
                        val totalExpRaw = contriDoc.get("total_group_expense")
                        totalGroupExpense = if (totalExpRaw is Number) totalExpRaw.toDouble() else 0.0
                    }

                    val membersMap = mutableMapOf<String, MemberLedger>()
                    
                    // Fetching entire room data from Master Map "expenses_data"
                    val expensesData = contriDoc.get("expenses_data") as? Map<String, Any> ?: emptyMap()

                    for (mId in memberIds) {
                        val uDoc = db.collection("Users").document(mId).get().await()
                        val actualName = uDoc.getString("name") ?: uDoc.getString("username") ?: "Unknown User"
                        
                        val userExpMap = expensesData[mId] as? Map<String, Any> ?: emptyMap()
                        var userSpent = 0.0
                        val expList = mutableListOf<ContriExpense>()
                        
                        // Sorting Keys (000A, 000B...) so sequence remains intact
                        val sortedKeys = userExpMap.keys.sorted()
                        
                        for (key in sortedKeys) {
                            val expObj = userExpMap[key] as? Map<String, Any> ?: continue
                            val item = expObj["itm"]?.toString() ?: "Unknown"
                            val rawAmt = expObj["amnt"]
                            val amt = if (rawAmt is Number) rawAmt.toDouble() else 0.0
                            
                            val rawDate = expObj["date"]
                            val formattedDate = if (rawDate is com.google.firebase.Timestamp) {
                                SimpleDateFormat("dd MMMM", Locale.getDefault()).format(rawDate.toDate())
                            } else if (rawDate is String) {
                                try {
                                    val parsed = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(rawDate)
                                    if (parsed != null) SimpleDateFormat("dd MMMM", Locale.getDefault()).format(parsed) else rawDate
                                } catch (e: Exception) { rawDate.toString() }
                            } else {
                                ""
                            }
                            
                            userSpent += amt
                            expList.add(ContriExpense(item, amt, formattedDate))
                        }
                        
                        membersMap[mId] = MemberLedger(mId, actualName, userSpent, expList)
                    }

                    val cyclesList = mutableListOf<PastCycle>()
                    val cycleDocs = contriDoc.reference.collection("Past_Cycles").get().await()
                    for (c in cycleDocs) {
                        cyclesList.add(
                            PastCycle(
                                dateRange = c.getString("date_range") ?: "", 
                                totalAmount = c.getString("total_amount") ?: "₹0"
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        ledgers = membersMap.values.toList()
                        pastCycles = cyclesList
                        isLoading = false
                        isSyncing = false
                    }
                } else {
                    withContext(Dispatchers.Main) { 
                        isLoading = false
                        isSyncing = false
                        Toast.makeText(context, "Error: Room details not found in database.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { 
                    isLoading = false
                    isSyncing = false
                    Toast.makeText(context, "Data Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).bounceClick { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(text = formattedName, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .border(1.2.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isAdmin) "Admin" else "Member", 
                        fontSize = 10.sp, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant, 
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                if (isAdmin) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).bounceClick { showSettingsDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Outlined.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
                
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).bounceClick { showLeaveDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Outlined.ExitToApp, contentDescription = "Leave Room", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        floatingActionButton = {
            PremiumFloatingButton(onClick = { showAddExpenseDialog = true })
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                            Text("₹", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = totalGroupExpense.toInt().toString(), 
                                fontSize = 28.sp, 
                                fontWeight = FontWeight.ExtraBold, 
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            
                            Icon(
                                imageVector = Icons.Outlined.Sync,
                                contentDescription = "Sync",
                                tint = if (isSyncing || actionProcessing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(24.dp)
                                    .rotate(currentRotation)
                                    .bounceClick { if (!isSyncing && !actionProcessing) refreshTrigger++ }
                                    .padding(2.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .bounceClick { showSettleDialog = true }
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Settle-up", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                            }

                            if (isAdmin) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .bounceClick { showNewCycleDialog = true }
                                        .clip(RoundedCornerShape(50))
                                        .border(1.2.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(50))
                                        .padding(horizontal = 12.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("New", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
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
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = room.roomCode, 
                                fontSize = 17.sp, 
                                fontWeight = FontWeight.ExtraBold, 
                                color = MaterialTheme.colorScheme.onSurface, 
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Pin: $localRoomPin", 
                            fontSize = 13.sp, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant, 
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (isLoading && ledgers.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (ledgers.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp).padding(top = 40.dp), contentAlignment = Alignment.TopCenter) {
                    Text("No members yet.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
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
                                            coroutineScope.launch(Dispatchers.Main) {
                                                delay(500)
                                                loadingState[cycle.dateRange] = false
                                                fetchedCycleData[cycle.dateRange] = emptyList()
                                            }
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(1.5.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = cycle.dateRange, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isFetching) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text(text = cycle.totalAmount, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                if (isExpanded) {
                                    if (cycleLedger != null && cycleLedger.isNotEmpty()) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), thickness = 0.5.dp)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        DynamicLedgerView(cycleLedger)
                                        Spacer(modifier = Modifier.height(12.dp))
                                    } else if (!isFetching) {
                                        Text("No data available.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center)
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
        // DIALOG TRIGGERS 
        // ==========================================

        if (showSettingsDialog) {
            AdminSettingsDialog(
                initialName = localRoomName,
                initialPin = localRoomPin,
                ledgers = ledgers,
                currentUserId = currentUserId,
                onDismiss = { showSettingsDialog = false },
                onSave = { newName, newPin ->
                    showSettingsDialog = false
                    Toast.makeText(context, "Saving settings...", Toast.LENGTH_SHORT).show()
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val db = FirebaseFirestore.getInstance()
                            val q = db.collection("Contri").whereEqualTo("contri_code", room.roomCode).get().await()
                            if (!q.isEmpty) {
                                q.documents[0].reference.update(
                                    "contri_name", newName,
                                    "passkey", newPin
                                ).await()
                                withContext(Dispatchers.Main) { refreshTrigger++ }
                            }
                        } catch (e: Exception) {}
                    }
                },
                onRemoveMemberClick = { member -> memberToRemove = member }
            )
        }

        if (memberToRemove != null) {
            RemoveMemberDialog(
                memberName = memberToRemove!!.memberName,
                onDismiss = { memberToRemove = null },
                onConfirm = {
                    val targetUserId = memberToRemove!!.userId
                    memberToRemove = null
                    showSettingsDialog = false
                    actionProcessing = true
                    Toast.makeText(context, "Removing member...", Toast.LENGTH_SHORT).show()
                    
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val db = FirebaseFirestore.getInstance()
                            val q = db.collection("Contri").whereEqualTo("contri_code", room.roomCode).get().await()
                            if (!q.isEmpty) {
                                val doc = q.documents[0]
                                
                                val memberIds = (doc.get("member_ids") as? List<*>)?.map { it.toString() } ?: emptyList()
                                val remainingMembers = memberIds.filter { it != targetUserId }

                                // Delete room permanently from Firebase if last member removed
                                if (remainingMembers.isEmpty()) {
                                    doc.reference.delete().await()
                                } else {
                                    val expensesData = doc.get("expenses_data") as? Map<String, Any> ?: emptyMap()
                                    val userExpMap = expensesData[targetUserId] as? Map<String, Any> ?: emptyMap()
                                    
                                    var amountToDeduct = 0.0
                                    for ((_, expObj) in userExpMap) {
                                        val mapObj = expObj as? Map<String, Any> ?: continue
                                        val amt = (mapObj["amnt"] as? Number)?.toDouble() ?: 0.0
                                        amountToDeduct += amt
                                    }

                                    doc.reference.update(
                                        "total_group_expense", FieldValue.increment(-amountToDeduct),
                                        "member_ids", FieldValue.arrayRemove(targetUserId),
                                        "expenses_data.$targetUserId", FieldValue.delete()
                                    ).await()
                                }
                                
                                val targetUserDocRef = db.collection("Users").document(targetUserId)
                                val targetData = targetUserDocRef.get().await().data ?: emptyMap<String, Any>()
                                val roomsArray = targetData["rooms"] as? List<String> ?: emptyList()
                                val targetRoomString = roomsArray.find { it.contains(room.roomCode, ignoreCase = true) }
                                
                                if (targetRoomString != null) {
                                    targetUserDocRef.update("rooms", FieldValue.arrayRemove(targetRoomString)).await()
                                }

                                withContext(Dispatchers.Main) { 
                                    actionProcessing = false
                                    refreshTrigger++ 
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) { actionProcessing = false }
                        }
                    }
                }
            )
        }

        if (showSettleDialog) {
            val myDisplayName = ledgers.find { it.userId == currentUserId }?.memberName ?: username
            SettleUpDialog(myName = myDisplayName, ledgers = ledgers, totalExpense = totalGroupExpense, onDismiss = { showSettleDialog = false })
        }

        if (showNewCycleDialog) {
            NewCycleDialog(
                onDismiss = { showNewCycleDialog = false },
                onConfirm = {
                    showNewCycleDialog = false
                    actionProcessing = true
                    Toast.makeText(context, "Starting new cycle...", Toast.LENGTH_SHORT).show()
                    
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val db = FirebaseFirestore.getInstance()
                            val q = db.collection("Contri").whereEqualTo("contri_code", room.roomCode).get().await()
                            if (!q.isEmpty) {
                                val doc = q.documents[0]
                                val ref = doc.reference
                                
                                ref.update(
                                    "total_group_expense", 0.0,
                                    "expenses_data", emptyMap<String, Any>()
                                ).await()
                                
                                withContext(Dispatchers.Main) { 
                                    actionProcessing = false
                                    refreshTrigger++ 
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) { actionProcessing = false }
                        }
                    }
                }
            )
        }

        if (showLeaveDialog) {
            LeaveRoomDialog(
                onDismiss = { showLeaveDialog = false },
                onConfirm = {
                    showLeaveDialog = false
                    actionProcessing = true
                    Toast.makeText(context, "Leaving room...", Toast.LENGTH_SHORT).show()
                    
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val db = FirebaseFirestore.getInstance()
                            val q = db.collection("Contri").whereEqualTo("contri_code", room.roomCode).get().await()
                            if (!q.isEmpty) {
                                val doc = q.documents[0]
                                
                                val memberIds = (doc.get("member_ids") as? List<*>)?.map { it.toString() } ?: emptyList()
                                val remainingMembers = memberIds.filter { it != currentUserId }

                                // Delete room permanently from Firebase if last member left
                                if (remainingMembers.isEmpty()) {
                                    doc.reference.delete().await()
                                } else {
                                    val expensesData = doc.get("expenses_data") as? Map<String, Any> ?: emptyMap()
                                    val userExpMap = expensesData[currentUserId] as? Map<String, Any> ?: emptyMap()
                                    
                                    var amountToDeduct = 0.0
                                    for ((_, expObj) in userExpMap) {
                                        val mapObj = expObj as? Map<String, Any> ?: continue
                                        val amt = (mapObj["amnt"] as? Number)?.toDouble() ?: 0.0
                                        amountToDeduct += amt
                                    }

                                    doc.reference.update(
                                        "total_group_expense", FieldValue.increment(-amountToDeduct),
                                        "member_ids", FieldValue.arrayRemove(currentUserId),
                                        "expenses_data.$currentUserId", FieldValue.delete()
                                    ).await()
                                }
                                
                                val currentUserDocRef = db.collection("Users").document(currentUserId)
                                val userData = currentUserDocRef.get().await().data ?: emptyMap<String, Any>()
                                val roomsArray = userData["rooms"] as? List<String> ?: emptyList()
                                val targetRoomString = roomsArray.find { it.contains(room.roomCode, ignoreCase = true) }
                                
                                if (targetRoomString != null) {
                                    currentUserDocRef.update("rooms", FieldValue.arrayRemove(targetRoomString)).await()
                                }

                                withContext(Dispatchers.Main) { 
                                    actionProcessing = false
                                    onLeaveClick() 
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) { actionProcessing = false }
                        }
                    }
                }
            )
        }

        if (showAddExpenseDialog) {
            AddContriExpenseDialog(
                onDismiss = { showAddExpenseDialog = false },
                onAdd = { title, dateMillis, amount ->
                    showAddExpenseDialog = false
                    actionProcessing = true
                    Toast.makeText(context, "Adding expense...", Toast.LENGTH_SHORT).show()
                    
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val db = FirebaseFirestore.getInstance()
                            val q = db.collection("Contri").whereEqualTo("contri_code", room.roomCode).get().await()
                            
                            if (!q.isEmpty) {
                                val doc = q.documents[0]
                                val ref = doc.reference
                                
                                val expensesData = doc.get("expenses_data") as? Map<String, Any> ?: emptyMap()
                                val userExpMap = expensesData[currentUserId] as? Map<String, Any> ?: emptyMap()
                                
                                val nextIndex = userExpMap.size
                                val expenseId = generateExpenseId(nextIndex)

                                val transData = mapOf(
                                    "itm" to title,
                                    "amnt" to amount,
                                    "date" to com.google.firebase.Timestamp(Date(dateMillis))
                                )
                                
                                val updates = hashMapOf<String, Any>(
                                    "expenses_data.$currentUserId.$expenseId" to transData,
                                    "total_group_expense" to FieldValue.increment(amount)
                                )
                                
                                ref.update(updates).await()
                                
                                withContext(Dispatchers.Main) { 
                                    actionProcessing = false
                                    refreshTrigger++ 
                                }
                            } else {
                                withContext(Dispatchers.Main) { 
                                    actionProcessing = false
                                    Toast.makeText(context, "Error: Room not found", Toast.LENGTH_LONG).show() 
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) { 
                                actionProcessing = false
                                Toast.makeText(context, "Save Error: ${e.message}", Toast.LENGTH_LONG).show() 
                            }
                        }
                    }
                }
            )
        }
    }
}

// ==========================================
// MODULAR DIALOG COMPONENTS
// ==========================================

@Composable
fun AdminSettingsDialog(
    initialName: String,
    initialPin: String,
    ledgers: List<MemberLedger>,
    currentUserId: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onRemoveMemberClick: (MemberLedger) -> Unit
) {
    var editName by remember { mutableStateOf(initialName) }
    var editPin by remember { mutableStateOf(initialPin) }
    var isEditingName by remember { mutableStateOf(false) }
    var isEditingPin by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Room Settings", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
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
                        focusedBorderColor = if(isEditingName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), 
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    trailingIcon = {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).bounceClick { isEditingName = !isEditingName },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = if (isEditingName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
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
                        focusedBorderColor = if(isEditingPin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), 
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    trailingIcon = {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).bounceClick { isEditingPin = !isEditingPin },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = if (isEditingPin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))
                Text("Manage Members", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 170.dp)
                        .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    if (ledgers.isNotEmpty()) {
                        LazyColumn {
                            itemsIndexed(ledgers) { index, member ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val dispName = if (member.memberName.length > 10) member.memberName.take(10) + "..." else member.memberName
                                    Text(text = dispName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                                    
                                    if (member.userId == currentUserId) {
                                        Text("Admin", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(24.dp).bounceClick { onRemoveMemberClick(member) }
                                        )
                                    }
                                }
                                if (index < ledgers.size - 1) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), thickness = 1.dp)
                                }
                            }
                        }
                    } else {
                        Text("No members found.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Cancel", 
                        color = MaterialTheme.colorScheme.onSurfaceVariant, 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.bounceClick { onDismiss() }.padding(8.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .bounceClick { 
                                if (editName.isNotBlank() && editPin.length == 6) {
                                    onSave(editName, editPin)
                                }
                            }
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) { 
                        Text("Save Changes", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) 
                    }
                }
            }
        }
    }
}

@Composable
fun RemoveMemberDialog(memberName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove Member?", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface) },
        text = { Text("Are you sure you want to kick '${memberName}'? This will deduct their expenses from the total.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        containerColor = MaterialTheme.colorScheme.surface,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Remove", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) }
        }
    )
}

@Composable
fun NewCycleDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start New Cycle?", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface) },
        text = { Text("Once created, calculations will restart from zero.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        containerColor = MaterialTheme.colorScheme.surface,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("New", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) } 
        }
    )
}

@Composable
fun LeaveRoomDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Leave Room?", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface) },
        text = { Text("Are you sure you want to leave this Contri room? Your expenses will be deducted from the total.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        containerColor = MaterialTheme.colorScheme.surface,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Leave", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) } 
        }
    )
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
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "₹${ledger.totalSpent.toInt()}", 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        val dividerModifier = if (isScrollable) Modifier.width(fixedColumnWidth * memberCount) else Modifier.fillMaxWidth()
        HorizontalDivider(modifier = dividerModifier.padding(vertical = 8.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))

        Row(modifier = if (!isScrollable) Modifier.fillMaxWidth() else Modifier) {
            ledgers.forEach { ledger ->
                Column(
                    modifier = if (isScrollable) Modifier.width(fixedColumnWidth) else Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ledger.expenses.forEach { expense ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 12.dp)) {
                            Text(text = expense.itemName, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "₹${expense.amount.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = expense.date, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SMART NAME FORMATTER & SETTLE UP DIALOG
// ==========================================
fun formatMemberDisplayName(fullName: String, allFullNames: List<String>): String {
    val trimmed = fullName.trim()
    if (trimmed.isEmpty()) return "Unknown"
    val parts = trimmed.split("\\s+".toRegex())
    val firstName = parts.firstOrNull() ?: trimmed
    val lastName = if (parts.size > 1) parts.last() else ""

    val sameFirstNameCount = allFullNames.count { name ->
        val otherFirst = name.trim().split("\\s+".toRegex()).firstOrNull() ?: name.trim()
        otherFirst.equals(firstName, ignoreCase = true)
    }

    return if (sameFirstNameCount > 1 && lastName.isNotEmpty()) {
        "${lastName.first().uppercaseChar()} $firstName"
    } else {
        firstName
    }
}

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
    
    val allMemberNames = ledgers.map { it.memberName }
    val formattedNameMap = ledgers.associate { ledger ->
        ledger.memberName to formatMemberDisplayName(ledger.memberName, allMemberNames)
    }

    val sortedSettlements = allSettlements.sortedWith(
        compareByDescending<Settlement> { it.from == myName || it.to == myName }
        .thenByDescending { it.amount }
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(8.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Total: ₹${totalExpense.toInt()}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    if (sortedSettlements.isEmpty()) {
                        Text("No pending payments.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    } else {
                        sortedSettlements.forEach { settlement ->
                            val fromText = if (settlement.from == myName) "You" else (formattedNameMap[settlement.from] ?: settlement.from)
                            val toText = if (settlement.to == myName) "You" else (formattedNameMap[settlement.to] ?: settlement.to)
                            val actionWord = if (fromText == "You") "Pay" else "Pays"

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), 
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$fromText $actionWord $toText", 
                                    fontSize = 14.sp, 
                                    fontWeight = if (fromText == "You" || toText == "You") FontWeight.ExtraBold else FontWeight.SemiBold, 
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "₹${settlement.amount.toInt()}", 
                                    fontSize = 15.sp, 
                                    fontWeight = FontWeight.ExtraBold, 
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick { onDismiss() }
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Dismiss", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(8.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Add New Expense", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = expenseTitle,
                    onValueChange = { expenseTitle = it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } },
                    label = { Text("Expense Title", fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true,
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

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    com.kartikey.rupeeflow.UI_Screens.CustomDatePicker(label = "Date", selectedDateMillis = dateMillis, onDateSelected = { dateMillis = it }, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) amount = it },
                        label = { Text("Amount", fontSize = 13.sp) },
                        prefix = { Text("₹ ", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary, 
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Cancel", 
                        color = MaterialTheme.colorScheme.onSurfaceVariant, 
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
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) { 
                        Text("Add Expense", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) 
                    }
                }
            }
        }
    }
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
            .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) { 
        Icon(imageVector = Icons.Outlined.Add, contentDescription = "Add Expense", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(28.dp)) 
    }
}

// ==========================================
// BASE-26 ID GENERATOR (000A, 000B... 000Z, 00AA)
// ==========================================
fun generateExpenseId(index: Int): String {
    var n = index
    var result = ""
    while (n >= 0) {
        result = ('A' + (n % 26)).toString() + result
        n = (n / 26) - 1
    }
    return result.padStart(4, '0')
}
