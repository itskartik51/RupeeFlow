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
import com.kartikey.rupeeflow.UI_Screens.Home.Contri.ContriRoomModel
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    
    var expenseFormTarget by remember { mutableStateOf<ContriExpense?>(null) }
    var showExpenseForm by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<ContriExpense?>(null) }
    
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

        val result = fetchContriRoomData(
            username = username,
            roomCode = room.roomCode,
            defaultRoomName = room.roomName,
            defaultRoomPin = room.pin
        )

        withContext(Dispatchers.Main) {
            if (result != null) {
                localRoomName = result.roomName
                localRoomPin = result.roomPin
                isAdmin = result.isAdmin
                totalGroupExpense = result.totalGroupExpense
                ledgers = result.ledgers
                pastCycles = result.pastCycles
                currentUserId = result.currentUserId
                isLoading = false
                isSyncing = false
            } else {
                isLoading = false
                isSyncing = false
                Toast.makeText(context, "Error: Room details not found in database.", Toast.LENGTH_LONG).show()
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
            PremiumFloatingButton(
                onClick = { 
                    expenseFormTarget = null
                    showExpenseForm = true 
                }
            )
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
                    onEditExpense = { expense: ContriExpense -> 
                        expenseFormTarget = expense
                        showExpenseForm = true
                    },
                    onDeleteExpense = { expense: ContriExpense -> expenseToDelete = expense }
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
        // DIALOGS & ACTIONS
        // ==========================================

        if (showExpenseForm) {
            ContriExpenseFormDialog(
                expense = expenseFormTarget,
                onDismiss = { showExpenseForm = false },
                onSave = { title: String, dateMillis: Long, amount: Double ->
                    val target = expenseFormTarget
                    showExpenseForm = false
                    actionProcessing = true

                    coroutineScope.launch {
                        val success = if (target == null) {
                            addContriExpense(room.roomCode, currentUserId, title, dateMillis, amount)
                        } else {
                            updateContriExpense(room.roomCode, currentUserId, target, title, dateMillis, amount)
                        }
                        actionProcessing = false
                        if (success) refreshTrigger++
                    }
                }
            )
        }

        if (expenseToDelete != null) {
            DeleteExpenseConfirmationDialog(
                expense = expenseToDelete!!,
                onDismiss = { expenseToDelete = null },
                onConfirm = {
                    val target = expenseToDelete!!
                    expenseToDelete = null
                    actionProcessing = true

                    coroutineScope.launch {
                        val success = deleteContriExpense(room.roomCode, currentUserId, target)
                        actionProcessing = false
                        if (success) refreshTrigger++
                    }
                }
            )
        }

        if (showSettingsDialog) {
            AdminSettingsDialog(
                initialName = localRoomName,
                initialPin = localRoomPin,
                ledgers = ledgers,
                currentUserId = currentUserId,
                onDismiss = { showSettingsDialog = false },
                onSave = { newName: String, newPin: String ->
                    showSettingsDialog = false
                    coroutineScope.launch {
                        val success = updateRoomDetails(room.roomCode, newName, newPin)
                        if (success) refreshTrigger++
                    }
                },
                onRemoveMemberClick = { member: MemberLedger -> memberToRemove = member }
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

                    coroutineScope.launch {
                        val success = removeMemberFromContri(room.roomCode, targetUserId)
                        actionProcessing = false
                        if (success) refreshTrigger++
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

                    coroutineScope.launch {
                        val success = startNewContriCycle(room.roomCode)
                        actionProcessing = false
                        if (success) refreshTrigger++
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

                    coroutineScope.launch {
                        val success = leaveContriRoom(room.roomCode, currentUserId)
                        actionProcessing = false
                        if (success) onLeaveClick()
                    }
                }
            )
        }
    }
}
