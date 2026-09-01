package com.kartikey.rupeeflow.UI_Screens.Home.Contri.InContri

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ==========================================
// CORE DATA MODELS
// ==========================================
data class ContriExpense(
    val key: String,
    val itemName: String,
    val amount: Double,
    val date: String,
    val rawDateMillis: Long
)

data class MemberLedger(
    val userId: String,
    val memberName: String,
    val totalSpent: Double,
    val expenses: List<ContriExpense>,
    val isVerified: Boolean = false
)

data class Settlement(val from: String, val to: String, val amount: Double)
data class PastCycle(val dateRange: String, val totalAmount: String)

data class RoomDataResult(
    val roomName: String,
    val roomPin: String,
    val isAdmin: Boolean,
    val totalGroupExpense: Double,
    val ledgers: List<MemberLedger>,
    val pastCycles: List<PastCycle>,
    val currentUserId: String
)

// ==========================================
// BASE-26 EXPENSE ID GENERATOR
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

// ==========================================
// FIRESTORE ENGINE & BACKEND OPERATIONS
// ==========================================
suspend fun fetchContriRoomData(
    username: String,
    roomCode: String,
    defaultRoomName: String,
    defaultRoomPin: String
): RoomDataResult? = withContext(Dispatchers.IO) {
    try {
        val db = FirebaseFirestore.getInstance()
        var currentUserId = ""

        val myUserQuery = db.collection("Users").whereEqualTo("username", username).get().await()
        if (!myUserQuery.isEmpty) {
            currentUserId = myUserQuery.documents[0].id
        }

        val contriQuery = db.collection("Contri").whereEqualTo("contri_code", roomCode).get().await()
        if (contriQuery.isEmpty) return@withContext null

        val contriDoc = contriQuery.documents[0]
        val adminId = contriDoc.getString("admin_id") ?: ""
        val roomName = contriDoc.getString("contri_name") ?: defaultRoomName
        val roomPin = contriDoc.getString("passkey") ?: defaultRoomPin
        val isAdmin = adminId == currentUserId

        val totalExpRaw = contriDoc.get("total_group_expense")
        val totalGroupExpense = if (totalExpRaw is Number) totalExpRaw.toDouble() else 0.0

        val memberIds = (contriDoc.get("member_ids") as? List<*>)?.map { it.toString() } ?: emptyList()
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
                val formattedDate = if (rawDate is Timestamp) {
                    rawMillis = rawDate.toDate().time
                    SimpleDateFormat("dd MMMM", Locale.getDefault()).format(rawDate.toDate())
                } else if (rawDate is String) {
                    try {
                        val parsed = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(rawDate)
                        if (parsed != null) {
                            rawMillis = parsed.time
                            SimpleDateFormat("dd MMMM", Locale.getDefault()).format(parsed)
                        } else rawDate
                    } catch (e: Exception) {
                        rawDate.toString()
                    }
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

        RoomDataResult(
            roomName = roomName,
            roomPin = roomPin,
            isAdmin = isAdmin,
            totalGroupExpense = totalGroupExpense,
            ledgers = sortedLedgers,
            pastCycles = cyclesList,
            currentUserId = currentUserId
        )
    } catch (e: Exception) {
        null
    }
}

suspend fun updateRoomDetails(roomCode: String, newName: String, newPin: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val db = FirebaseFirestore.getInstance()
        val q = db.collection("Contri").whereEqualTo("contri_code", roomCode).get().await()
        if (!q.isEmpty) {
            val doc = q.documents[0]
            doc.reference.update(
                "contri_name", newName,
                "passkey", newPin
            ).await()

            val memberIds = (doc.get("member_ids") as? List<*>)?.map { it.toString() } ?: emptyList()
            for (mId in memberIds) {
                try {
                    val userRef = db.collection("Users").document(mId)
                    val userDoc = userRef.get().await()
                    if (userDoc.exists()) {
                        val roomsList = (userDoc.get("rooms") as? List<*>)?.map { it.toString() }?.toMutableList() ?: mutableListOf()
                        val targetIndex = roomsList.indexOfFirst { it.contains(roomCode, ignoreCase = true) }
                        
                        if (targetIndex != -1) {
                            val oldRoomStr = roomsList[targetIndex]
                            val parts = oldRoomStr.split("_")
                            val prefix = if (parts.isNotEmpty()) parts[0] else ""
                            val newRoomStr = if (prefix.isNotBlank()) {
                                "${prefix}_${roomCode}_${newName}_${newPin}"
                            } else {
                                "${roomCode}_${newName}_${newPin}"
                            }
                            
                            roomsList[targetIndex] = newRoomStr
                            userRef.update("rooms", roomsList).await()
                        }
                    }
                } catch (e: Exception) {}
            }
            true
        } else false
    } catch (e: Exception) {
        false
    }
}

suspend fun removeMemberFromContri(roomCode: String, targetUserId: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val db = FirebaseFirestore.getInstance()
        val q = db.collection("Contri").whereEqualTo("contri_code", roomCode).get().await()
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
            val targetRoomString = roomsArray.find { it.contains(roomCode, ignoreCase = true) }

            if (targetRoomString != null) {
                targetUserDocRef.update("rooms", FieldValue.arrayRemove(targetRoomString)).await()
            }
            true
        } else false
    } catch (e: Exception) {
        false
    }
}

suspend fun leaveContriRoom(roomCode: String, currentUserId: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val db = FirebaseFirestore.getInstance()
        val q = db.collection("Contri").whereEqualTo("contri_code", roomCode).get().await()
        if (!q.isEmpty) {
            val doc = q.documents[0]
            val adminId = doc.getString("admin_id") ?: ""
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

                val updates = hashMapOf<String, Any>(
                    "total_group_expense" to FieldValue.increment(-amountToDeduct),
                    "member_ids" to FieldValue.arrayRemove(currentUserId),
                    "expenses_data.$currentUserId" to FieldValue.delete()
                )

                if (adminId == currentUserId && remainingMembers.isNotEmpty()) {
                    updates["admin_id"] = remainingMembers.first()
                }

                doc.reference.update(updates as Map<String, Any>).await()
            }

            val currentUserDocRef = db.collection("Users").document(currentUserId)
            val userData = currentUserDocRef.get().await().data ?: emptyMap<String, Any>()
            val roomsArray = (userData["rooms"] as? List<*>)?.map { it.toString() } ?: emptyList()
            val targetRoomString = roomsArray.find { it.contains(roomCode, ignoreCase = true) }

            if (targetRoomString != null) {
                currentUserDocRef.update("rooms", FieldValue.arrayRemove(targetRoomString)).await()
            }
            true
        } else false
    } catch (e: Exception) {
        false
    }
}

suspend fun addContriExpense(
    roomCode: String,
    currentUserId: String,
    title: String,
    dateMillis: Long,
    amount: Double
): Boolean = withContext(Dispatchers.IO) {
    try {
        val db = FirebaseFirestore.getInstance()
        val q = db.collection("Contri").whereEqualTo("contri_code", roomCode).get().await()
        if (!q.isEmpty) {
            val doc = q.documents[0]
            val expensesData = doc.get("expenses_data") as? Map<String, Any> ?: emptyMap()
            val userExpMap = expensesData[currentUserId] as? Map<String, Any> ?: emptyMap()

            val nextIndex = userExpMap.size
            val expenseId = generateExpenseId(nextIndex)

            val transData = mapOf<String, Any>(
                "itm" to title,
                "amnt" to amount,
                "date" to Timestamp(Date(dateMillis))
            )

            val updates: Map<String, Any> = mapOf(
                "expenses_data.$currentUserId.$expenseId" to transData,
                "total_group_expense" to FieldValue.increment(amount)
            )

            doc.reference.update(updates).await()
            true
        } else false
    } catch (e: Exception) {
        false
    }
}

suspend fun updateContriExpense(
    roomCode: String,
    currentUserId: String,
    targetExpense: ContriExpense,
    newTitle: String,
    newDateMillis: Long,
    newAmount: Double
): Boolean = withContext(Dispatchers.IO) {
    try {
        val db = FirebaseFirestore.getInstance()
        val q = db.collection("Contri").whereEqualTo("contri_code", roomCode).get().await()
        if (!q.isEmpty) {
            val doc = q.documents[0]
            val amountDiff = newAmount - targetExpense.amount

            val transData = mapOf<String, Any>(
                "itm" to newTitle,
                "amnt" to newAmount,
                "date" to Timestamp(Date(newDateMillis))
            )

            val updates: Map<String, Any> = mapOf(
                "expenses_data.$currentUserId.${targetExpense.key}" to transData,
                "total_group_expense" to FieldValue.increment(amountDiff)
            )

            doc.reference.update(updates).await()
            true
        } else false
    } catch (e: Exception) {
        false
    }
}

suspend fun deleteContriExpense(
    roomCode: String,
    currentUserId: String,
    targetExpense: ContriExpense
): Boolean = withContext(Dispatchers.IO) {
    try {
        val db = FirebaseFirestore.getInstance()
        val q = db.collection("Contri").whereEqualTo("contri_code", roomCode).get().await()
        if (!q.isEmpty) {
            val doc = q.documents[0]
            val updates: Map<String, Any> = mapOf(
                "expenses_data.$currentUserId.${targetExpense.key}" to FieldValue.delete(),
                "total_group_expense" to FieldValue.increment(-targetExpense.amount)
            )

            doc.reference.update(updates).await()
            true
        } else false
    } catch (e: Exception) {
        false
    }
}

// ==========================================
// UNIFIED DIALOG CONTROLLER & HOST
// ==========================================
@Composable
fun ContriDialogController(
    roomCode: String,
    username: String,
    currentUserId: String,
    localRoomName: String,
    localRoomPin: String,
    totalGroupExpense: Double,
    ledgers: List<MemberLedger>,
    coroutineScope: CoroutineScope,
    context: Context,
    // Dialog States
    showExpenseForm: Boolean,
    onDismissExpenseForm: () -> Unit,
    expenseFormTarget: ContriExpense?,
    expenseToDelete: ContriExpense?,
    onDismissDeleteExpense: () -> Unit,
    showSettingsDialog: Boolean,
    onDismissSettings: () -> Unit,
    memberToRemove: MemberLedger?,
    onDismissRemoveMember: () -> Unit,
    onSelectRemoveMember: (MemberLedger) -> Unit,
    showSettleDialog: Boolean,
    onDismissSettle: () -> Unit,
    showNewCycleDialog: Boolean,
    onDismissNewCycle: () -> Unit,
    showLeaveDialog: Boolean,
    onDismissLeave: () -> Unit,
    onActionStart: () -> Unit,
    onActionEnd: () -> Unit,
    onRefreshNeeded: () -> Unit,
    onLeaveSuccess: () -> Unit
) {
    if (showExpenseForm) {
        ContriExpenseFormDialog(
            expense = expenseFormTarget,
            onDismiss = onDismissExpenseForm,
            onSave = { title: String, dateMillis: Long, amount: Double ->
                onDismissExpenseForm()
                onActionStart()
                coroutineScope.launch {
                    val success = if (expenseFormTarget == null) {
                        addContriExpense(roomCode, currentUserId, title, dateMillis, amount)
                    } else {
                        updateContriExpense(roomCode, currentUserId, expenseFormTarget, title, dateMillis, amount)
                    }
                    onActionEnd()
                    if (success) onRefreshNeeded()
                }
            }
        )
    }

    if (expenseToDelete != null) {
        DeleteExpenseConfirmationDialog(
            expense = expenseToDelete,
            onDismiss = onDismissDeleteExpense,
            onConfirm = {
                val target = expenseToDelete
                onDismissDeleteExpense()
                onActionStart()
                coroutineScope.launch {
                    val success = deleteContriExpense(roomCode, currentUserId, target)
                    onActionEnd()
                    if (success) onRefreshNeeded()
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
            onDismiss = onDismissSettings,
            onSave = { newName: String, newPin: String ->
                onDismissSettings()
                coroutineScope.launch {
                    val success = updateRoomDetails(roomCode, newName, newPin)
                    if (success) onRefreshNeeded()
                }
            },
            onRemoveMemberClick = onSelectRemoveMember
        )
    }

    if (memberToRemove != null) {
        RemoveMemberDialog(
            memberName = memberToRemove.memberName,
            onDismiss = onDismissRemoveMember,
            onConfirm = {
                val targetUserId = memberToRemove.userId
                onDismissRemoveMember()
                onDismissSettings()
                onActionStart()
                coroutineScope.launch {
                    val success = removeMemberFromContri(roomCode, targetUserId)
                    onActionEnd()
                    if (success) onRefreshNeeded()
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
            onDismiss = onDismissSettle
        )
    }

    if (showNewCycleDialog) {
        NewCycleDialog(
            onDismiss = onDismissNewCycle,
            onConfirm = {
                onDismissNewCycle()
                onActionStart()
                coroutineScope.launch {
                    val success = startNewContriCycle(roomCode)
                    onActionEnd()
                    if (success) onRefreshNeeded()
                }
            }
        )
    }

    if (showLeaveDialog) {
        LeaveRoomDialog(
            onDismiss = onDismissLeave,
            onConfirm = {
                onDismissLeave()
                onActionStart()
                coroutineScope.launch {
                    val success = leaveContriRoom(roomCode, currentUserId)
                    onActionEnd()
                    if (success) onLeaveSuccess()
                }
            }
        )
    }
}

// ==========================================
// ADMIN & LIFECYCLE MANAGEMENT DIALOGS
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
                        focusedBorderColor = if (isEditingName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), 
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
                        focusedBorderColor = if (isEditingPin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), 
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
                    val allNames = ledgers.map { it.memberName }
                    if (ledgers.isNotEmpty()) {
                        LazyColumn {
                            itemsIndexed(ledgers) { index, member ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val rawName = if (member.userId == currentUserId) "You" else formatMemberDisplayName(member.memberName, allNames)
                                    val dispName = if (rawName.length > 12) rawName.take(12) + "..." else rawName
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = dispName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                                        if (member.isVerified) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Filled.Verified,
                                                contentDescription = "Verified",
                                                tint = Color(0xFF1DA1F2),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                    
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
        text = { Text("Are you sure you want to kick '$memberName'? This will deduct their expenses from the total.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
