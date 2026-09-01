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
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ==========================================
// CYCLE BACKEND ENGINE (ARCHIVE & RESTORE)
// ==========================================

suspend fun startNewContriCycle(roomCode: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val db = FirebaseFirestore.getInstance()
        val q = db.collection("Contri").whereEqualTo("contri_code", roomCode).get().await()
        if (!q.isEmpty) {
            val doc = q.documents[0]
            val startDate = doc.getString("contri_date") ?: "Start"
            val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
            val totalExp = (doc.get("total_group_expense") as? Number)?.toDouble() ?: 0.0
            val expensesData = doc.get("expenses_data") as? Map<String, Any> ?: emptyMap()
            val memberIds = (doc.get("member_ids") as? List<*>)?.map { it.toString() } ?: emptyList()

            val dateRangeStr = "$startDate - $currentDate"
            val totalAmtStr = "₹${totalExp.toInt()}"

            // 1. Archive current cycle snapshot to Past_Cycles subcollection
            val archiveData = hashMapOf(
                "date_range" to dateRangeStr,
                "total_amount" to totalAmtStr,
                "expenses_data" to expensesData,
                "member_ids" to memberIds,
                "archived_at" to Timestamp.now()
            )
            doc.reference.collection("Past_Cycles").add(archiveData).await()

            // 2. Reset live cycle data to zero
            doc.reference.update(
                "total_group_expense", 0.0,
                "expenses_data", emptyMap<String, Any>(),
                "contri_date", currentDate
            ).await()

            true
        } else false
    } catch (e: Exception) {
        false
    }
}

suspend fun fetchPastCycleLedgers(roomCode: String, cycleDateRange: String): List<MemberLedger> = withContext(Dispatchers.IO) {
    try {
        val db = FirebaseFirestore.getInstance()
        val q = db.collection("Contri").whereEqualTo("contri_code", roomCode).get().await()
        if (q.isEmpty) return@withContext emptyList()

        val contriDoc = q.documents[0]
        val cycleDocs = contriDoc.reference.collection("Past_Cycles")
            .whereEqualTo("date_range", cycleDateRange)
            .get().await()
        if (cycleDocs.isEmpty) return@withContext emptyList()

        val cycleDoc = cycleDocs.documents[0]
        val memberIds = (cycleDoc.get("member_ids") as? List<*>)?.map { it.toString() } ?: emptyList()
        val expensesData = cycleDoc.get("expenses_data") as? Map<String, Any> ?: emptyMap()

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
// PAST CYCLES EXPANDABLE ACCORDION UI
// ==========================================

@Composable
fun PastCyclesSection(
    roomCode: String,
    pastCycles: List<PastCycle>,
    currentUserId: String
) {
    if (pastCycles.isEmpty()) return

    val coroutineScope = rememberCoroutineScope()
    val expandedState = remember { mutableStateMapOf<String, Boolean>() }
    val loadingState = remember { mutableStateMapOf<String, Boolean>() }
    val fetchedCycleData = remember { mutableStateMapOf<String, List<MemberLedger>>() }

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
                                coroutineScope.launch {
                                    val data = fetchPastCycleLedgers(roomCode, cycle.dateRange)
                                    withContext(Dispatchers.Main) {
                                        loadingState[cycle.dateRange] = false
                                        fetchedCycleData[cycle.dateRange] = data
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
