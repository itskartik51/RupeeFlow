package com.kartikey.rupeeflow.UI_Screens.Home.Contri.InContri

import android.widget.Toast
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.UI_Screens.Home.Contri.ContriRoomModel
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    var totalGroupExpense by remember { mutableDoubleStateOf(0.0) }
    var isAdmin by remember { mutableStateOf(false) } 
    var isSyncing by remember { mutableStateOf(false) } 
    var isLoading by remember { mutableStateOf(true) } 
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var actionProcessing by remember { mutableStateOf(false) }
    
    // Dialog triggers
    var showExpenseForm by remember { mutableStateOf(false) }
    var expenseFormTarget by remember { mutableStateOf<ContriExpense?>(null) }
    var expenseToDelete by remember { mutableStateOf<ContriExpense?>(null) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showSettleDialog by remember { mutableStateOf(false) }
    var showNewCycleDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var memberToRemove by remember { mutableStateOf<MemberLedger?>(null) }

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
        if (ledgers.isEmpty()) isLoading = true else isSyncing = true
        val result = fetchContriRoomData(username, room.roomCode, room.roomName, room.pin)

        withContext(Dispatchers.Main) {
            if (result != null) {
                localRoomName = result.roomName
                localRoomPin = result.roomPin
                isAdmin = result.isAdmin
                totalGroupExpense = result.totalGroupExpense
                ledgers = result.ledgers
                currentUserId = result.currentUserId
            } else {
                Toast.makeText(context, "Error: Room not found", Toast.LENGTH_SHORT).show()
            }
            isLoading = false
            isSyncing = false
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
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).bounceClick { onBackClick() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = formattedName, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.border(1.2.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = if (isAdmin) "Admin" else "Member", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.weight(1f))
                if (isAdmin) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).bounceClick { showSettingsDialog = true }, contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Outlined.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).bounceClick { showLeaveDialog = true }, contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.AutoMirrored.Outlined.ExitToApp, contentDescription = "Leave Room", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        floatingActionButton = {
            PremiumFloatingButton(onClick = { expenseFormTarget = null; showExpenseForm = true })
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
                    onEditExpense = { expense -> expenseFormTarget = expense; showExpenseForm = true },
                    onDeleteExpense = { expense -> expenseToDelete = expense }
                )
            }

            PastCyclesSection(
                roomCode = room.roomCode,
                currentUserId = currentUserId,
                refreshTrigger = refreshTrigger
            )
        }

        ContriDialogController(
            roomCode = room.roomCode,
            username = username,
            currentUserId = currentUserId,
            localRoomName = localRoomName,
            localRoomPin = localRoomPin,
            totalGroupExpense = totalGroupExpense,
            ledgers = ledgers,
            coroutineScope = coroutineScope,
            context = context,
            showExpenseForm = showExpenseForm,
            onDismissExpenseForm = { showExpenseForm = false },
            expenseFormTarget = expenseFormTarget,
            expenseToDelete = expenseToDelete,
            onDismissDeleteExpense = { expenseToDelete = null },
            showSettingsDialog = showSettingsDialog,
            onDismissSettings = { showSettingsDialog = false },
            memberToRemove = memberToRemove,
            onDismissRemoveMember = { memberToRemove = null },
            onSelectRemoveMember = { member -> memberToRemove = member },
            showSettleDialog = showSettleDialog,
            onDismissSettle = { showSettleDialog = false },
            showNewCycleDialog = showNewCycleDialog,
            onDismissNewCycle = { showNewCycleDialog = false },
            showLeaveDialog = showLeaveDialog,
            onDismissLeave = { showLeaveDialog = false },
            onActionStart = { actionProcessing = true },
            onActionEnd = { actionProcessing = false },
            onRefreshNeeded = { refreshTrigger++ },
            onLeaveSuccess = onLeaveClick
        )
    }
}
