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
// HISTORY CYCLE KEY GENERATOR (0A -> ZZ)
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
// CYCLE ARCHIVE ENGINE (HISTORY/1 WRITE)
// ==========================================
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
    pastCycles: List<PastCycle>,
    currentUserId: String
) {
    if (pastCycles.isEmpty()) return

    val expandedState = remember { mutableStateMapOf<String, Boolean>() }

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

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .animateContentSize() 
                    .bounceClick {
                        expandedState[cycle.dateRange] = !isExpanded
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
                        Text(
                            text = cycle.totalAmount, 
                            fontSize = 15.sp, 
                            fontWeight = FontWeight.ExtraBold, 
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(80.dp))
}
