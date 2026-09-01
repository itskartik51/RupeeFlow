package com.kartikey.rupeeflow.UI_Screens.Home.Contri.InContri

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ==========================================
// HISTORY DATA MODELS
// ==========================================
data class HistoryCycleModel(
    val key: String,
    val dateRange: String,
    val totalAmount: String
)

// ==========================================
// HISTORY KEY GENERATOR (0A -> ZZ)
// ==========================================
fun generateHistoryCycleKey(index: Int): String {
    return if (index < 26) {
        "0" + ('A' + index)
    } else {
        val adjusted = index - 26
        val firstChar = 'A' + (adjusted / 26)
        val secondChar = 'A' + (adjusted % 26)
        "$firstChar$secondChar"
    }
}

// ==========================================
// CYCLE BACKEND ENGINE (READ & WRITE)
// ==========================================

// WRITE: ARCHIVE TO HISTORY/1 AND RESET LIVE ROOM
suspend fun startNewContriCycle(roomCode: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val db = FirebaseFirestore.getInstance()
        val q = db.collection("Contri").whereEqualTo("contri_code", roomCode).get().await()
        if (q.isEmpty) return@withContext false

        val doc = q.documents[0]
        val startDate = doc.getString("contri_date") ?: "Start"
        val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        val totalExp = (doc.get("total_group_expense") as? Number)?.toDouble() ?: 0.0
        val expensesData = doc.get("expenses_data") as? Map<String, Any> ?: emptyMap()

        val historyDocRef = doc.reference.collection("History").document("1")
        val historySnap = historyDocRef.get().await()
        val existingMap = historySnap.data ?: emptyMap<String, Any>()
        val nextKey = generateHistoryCycleKey(existingMap.size)

        val cyclePayload = mapOf<String, Any>(
            "name" to "$startDate - $currentDate",
            "date" to Timestamp.now(),
            "ttl" to totalExp,
            "expenses_data" to expensesData
        )

        historyDocRef.set(mapOf(nextKey to cyclePayload), SetOptions.merge()).await()

        doc.reference.update(
            "total_group_expense", 0.0,
            "expenses_data", emptyMap<String, Any>(),
            "contri_date", currentDate
        ).await()

        true
    } catch (e: Exception) {
        false
    }
}

// READ: FETCH SUMMARY LIST OF ALL PAST CYCLES FROM HISTORY/1
suspend fun fetchPastCyclesList(roomCode: String): List<HistoryCycleModel> = withContext(Dispatchers.IO) {
    try {
        val db = FirebaseFirestore.getInstance()
        val q = db.collection("Contri").whereEqualTo("contri_code", roomCode).get().await()
        if (q.isEmpty) return@withContext emptyList()

        val contriDoc = q.documents[0]
        val historyDoc = contriDoc.reference.collection("History").document("1").get().await()
        if (!historyDoc.exists()) return@withContext emptyList()

        val historyMap = historyDoc.data ?: emptyMap<String, Any>()
        val sortedKeys = historyMap.keys.sorted()
        val list = mutableListOf<HistoryCycleModel>()

        for (key in sortedKeys) {
            val cycleObj = historyMap[key] as? Map<String, Any> ?: continue
            val name = cycleObj["name"]?.toString() ?: "Past Cycle"
            val ttl = (cycleObj["ttl"] as? Number)?.toDouble() ?: 0.0
            list.add(HistoryCycleModel(key = key, dateRange = name, totalAmount = "₹${ttl.toInt()}"))
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}

// READ: FETCH DETAILED MEMBER EXPENSES FOR A SPECIFIC CYCLE KEY
suspend fun fetchPastCycleLedgersByKey(roomCode: String, cycleKey: String): List<MemberLedger> = withContext(Dispatchers.IO) {
    try {
        val db = FirebaseFirestore.getInstance()
        val q = db.collection("Contri").whereEqualTo("contri_code", roomCode).get().await()
        if (q.isEmpty) return@withContext emptyList()

        val contriDoc = q.documents[0]
        val memberIds = (contriDoc.get("member_ids") as? List<*>)?.map { it.toString() } ?: emptyList()

        val historyDoc = contriDoc.reference.collection("History").document("1").get().await()
        if (!historyDoc.exists()) return@withContext emptyList()

        val historyMap = historyDoc.data ?: emptyMap<String, Any>()
        val targetCycle = historyMap[cycleKey] as? Map<String, Any> ?: return@withContext emptyList()
        val expensesData = targetCycle["expenses_data"] as? Map<String, Any> ?: emptyMap()

        val ledgers = mutableListOf<MemberLedger>()
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

            ledgers.add(MemberLedger(mId, actualName, userSpent, expList, userVerified))
        }
        ledgers
    } catch (e: Exception) {
        emptyList()
    }
}

// ==========================================
// START NEW CYCLE WARNING DIALOG
// ==========================================
@Composable
fun NewCycleDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Start New Cycle?", 
                fontWeight = FontWeight.ExtraBold, 
                color = MaterialTheme.colorScheme.onSurface
            ) 
        },
        text = { 
            Text(
                text = "Once a new cycle is started, all current expenses will be permanently archived to History and can no longer be edited or deleted. Active balances and room calculations will reset to zero.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                lineHeight = 20.sp
            ) 
        },
        containerColor = MaterialTheme.colorScheme.surface,
        confirmButton = {
            TextButton(onClick = onConfirm) { 
                Text("Start New", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) 
            }
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { 
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) 
            } 
        }
    )
}

// ==========================================
// PAST CYCLES ACCORDION SECTION UI
// ==========================================
@Composable
fun PastCyclesSection(
    roomCode: String,
    currentUserId: String,
    refreshTrigger: Int
) {
    val coroutineScope = rememberCoroutineScope()
    var cyclesList by remember { mutableStateOf<List<HistoryCycleModel>>(emptyList()) }
    val expandedState = remember { mutableStateMapOf<String, Boolean>() }
    val loadingState = remember { mutableStateMapOf<String, Boolean>() }
    val fetchedCycleData = remember { mutableStateMapOf<String, List<MemberLedger>>() }

    LaunchedEffect(roomCode, refreshTrigger) {
        cyclesList = fetchPastCyclesList(roomCode)
    }

    if (cyclesList.isEmpty()) return

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
        cyclesList.forEach { cycle ->
            val isExpanded = expandedState[cycle.key] == true
            val isFetching = loadingState[cycle.key] == true
            val cycleLedger = fetchedCycleData[cycle.key]

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .animateContentSize() 
                    .bounceClick {
                        if (isExpanded) {
                            expandedState[cycle.key] = false
                        } else {
                            expandedState[cycle.key] = true
                            if (cycleLedger == null) {
                                loadingState[cycle.key] = true
                                coroutineScope.launch {
                                    val data = fetchPastCycleLedgersByKey(roomCode, cycle.key)
                                    withContext(Dispatchers.Main) {
                                        loadingState[cycle.key] = false
                                        fetchedCycleData[cycle.key] = data
                                    }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = cycle.dateRange, 
                            fontSize = 13.sp, 
                            fontWeight = FontWeight.Medium, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isFetching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp), 
                                    color = MaterialTheme.colorScheme.primary, 
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = cycle.totalAmount, 
                                fontSize = 15.sp, 
                                fontWeight = FontWeight.ExtraBold, 
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (isExpanded) {
                        if (cycleLedger != null && cycleLedger.isNotEmpty()) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), 
                                thickness = 0.5.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            DynamicLedgerView(
                                currentUserId = currentUserId,
                                ledgers = cycleLedger,
                                onEditExpense = {},
                                onDeleteExpense = {}
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        } else if (!isFetching) {
                            Text(
                                text = "No details available for this cycle.", 
                                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp), 
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(80.dp))
}
