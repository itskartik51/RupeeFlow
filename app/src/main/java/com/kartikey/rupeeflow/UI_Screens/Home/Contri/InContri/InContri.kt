package com.kartikey.rupeeflow.UI_Screens.Home.Contri.InContri

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.kartikey.rupeeflow.UI_Screens.Home.Contri.ContriRoomModel
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InsideContriScreen(
    username: String, 
    room: ContriRoomModel,
    onBackClick: () -> Unit,
    onLeaveClick: () -> Unit
) {
    val context = LocalContext.current
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
    
    var expenseToEdit by remember { mutableStateOf<ContriExpense?>(null) }
    var expenseToDelete by remember { mutableStateOf<ContriExpense?>(null) }
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
                currentRotation -= 8f 
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
                    val expensesData = contriDoc.get("expenses_data") as? Map<String, Any> ?: emptyMap()

                    for (mId in memberIds) {
                        val uDoc = db.collection("Users").document(mId).get().await()
                        val actualName = uDoc.getString("name") ?: uDoc.getString("username") ?: "Unknown User"
                        val userVerified = uDoc.getBoolean("verify") ?: false
                        
                        val userExpMap = expensesData[mId] as? Map<String, Any> ?: emptyMap()
                        var userSpent = 0.0
                        val expList = mutableListOf<ContriExpense>()
                        
                        val sortedKeys = userExpMap.keys.sorted()
                        
                        for (key in sortedKeys) {
                            val expObj = userExpMap[key] as? Map<String, Any> ?: continue
                            val item = expObj["itm"]?.toString() ?: "Unknown"
                            val rawAmt = expObj["amnt"]
                            val amt = if (rawAmt is Number) rawAmt.toDouble() else 0.0
                            
                            val rawDate = expObj["date"]
                            var rawMillis = System.currentTimeMillis()
                            val formattedDate = if (rawDate is com.google.firebase.Timestamp) {
                                rawMillis = rawDate.toDate().time
                                SimpleDateFormat("dd MMMM", Locale.getDefault()).format(rawDate.toDate())
                            } else if (rawDate is String) {
                                try {
                                    val parsed = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(rawDate)
                                    if (parsed != null) {
                                        rawMillis = parsed.time
                                        SimpleDateFormat("dd MMMM", Locale.getDefault()).format(parsed)
                                    } else rawDate
                                } catch (e: Exception) { rawDate.toString() }
                            } else {
                                ""
                            }
                            
                            userSpent += amt
                            expList.add(ContriExpense(key, item, amt, formattedDate, rawMillis))
                        }
                        
                        membersMap[mId] = MemberLedger(mId, actualName, userSpent, expList, userVerified)
                    }

                    val sortedLedgers = mutableListOf<MemberLedger>()
                    membersMap[currentUserId]?.let { sortedLedgers.add(it) }
                    
                    if (adminId != currentUserId && membersMap.containsKey(adminId)) {
                        membersMap[adminId]?.let { sortedLedgers.add(it) }
                    }
                    
                    for (mId in memberIds) {
                        if (mId != currentUserId && mId != adminId) {
                            membersMap[mId]?.let { sortedLedgers.add(it) }
                        }
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
                        ledgers = sortedLedgers
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
            ContriInfoCard(
                totalGroupExpense = totalGroupExpense,
                isSyncing = isSyncing,
                actionProcessing = actionProcessing,
                currentRotation = currentRotation,
                isAdmin = isAdmin,
                roomCode = room.roomCode,
                roomPin = localRoomPin,
                onSyncClick = { if (!isSyncing && !actionProcessing) refreshTrigger++ },
                onSettleClick = { showSettleDialog = true },
                onNewCycleClick = { showNewCycleDialog = true }
            )

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
                DynamicLedgerView(
                    currentUserId = currentUserId,
                    ledgers = ledgers,
                    onEditExpense = { expense -> expenseToEdit = expense },
                    onDeleteExpense = { expense -> expenseToDelete = expense }
                )
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
                                        DynamicLedgerView(
                                            currentUserId = currentUserId,
                                            ledgers = cycleLedger,
                                            onEditExpense = {},
                                            onDeleteExpense = {}
                                        )
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
            val allNames = ledgers.map { it.memberName }
            val myRealName = ledgers.find { it.userId == currentUserId }?.memberName ?: username
            SettleUpDialog(
                myName = myRealName, 
                allMemberNames = allNames, 
                ledgers = ledgers, 
                totalExpense = totalGroupExpense, 
                onDismiss = { showSettleDialog = false }
            )
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

        if (expenseToEdit != null) {
            EditContriExpenseDialog(
                expense = expenseToEdit!!,
                onDismiss = { expenseToEdit = null },
                onUpdate = { newTitle, newDateMillis, newAmount ->
                    val targetExpense = expenseToEdit!!
                    expenseToEdit = null
                    actionProcessing = true
                    Toast.makeText(context, "Updating expense...", Toast.LENGTH_SHORT).show()

                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val db = FirebaseFirestore.getInstance()
                            val q = db.collection("Contri").whereEqualTo("contri_code", room.roomCode).get().await()
                            if (!q.isEmpty) {
                                val doc = q.documents[0]
                                val ref = doc.reference
                                val amountDiff = newAmount - targetExpense.amount

                                val transData = mapOf(
                                    "itm" to newTitle,
                                    "amnt" to newAmount,
                                    "date" to com.google.firebase.Timestamp(Date(newDateMillis))
                                )

                                val updates = hashMapOf<String, Any>(
                                    "expenses_data.$currentUserId.${targetExpense.key}" to transData,
                                    "total_group_expense" to FieldValue.increment(amountDiff)
                                )

                                ref.update(updates).await()
                                withContext(Dispatchers.Main) {
                                    actionProcessing = false
                                    refreshTrigger++
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                actionProcessing = false
                                Toast.makeText(context, "Update Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            )
        }

        if (expenseToDelete != null) {
            DeleteExpenseConfirmationDialog(
                expense = expenseToDelete!!,
                onDismiss = { expenseToDelete = null },
                onConfirm = {
                    val targetExpense = expenseToDelete!!
                    expenseToDelete = null
                    actionProcessing = true
                    Toast.makeText(context, "Deleting expense...", Toast.LENGTH_SHORT).show()

                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val db = FirebaseFirestore.getInstance()
                            val q = db.collection("Contri").whereEqualTo("contri_code", room.roomCode).get().await()
                            if (!q.isEmpty) {
                                val doc = q.documents[0]
                                val ref = doc.reference

                                val updates = hashMapOf<String, Any>(
                                    "expenses_data.$currentUserId.${targetExpense.key}" to FieldValue.delete(),
                                    "total_group_expense" to FieldValue.increment(-targetExpense.amount)
                                )

                                ref.update(updates).await()
                                withContext(Dispatchers.Main) {
                                    actionProcessing = false
                                    refreshTrigger++
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                actionProcessing = false
                                Toast.makeText(context, "Delete Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            )
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
        Icon(
            imageVector = Icons.Outlined.Add, 
            contentDescription = "Add Expense", 
            tint = MaterialTheme.colorScheme.onPrimary, 
            modifier = Modifier.size(28.dp)
        ) 
    }
}
