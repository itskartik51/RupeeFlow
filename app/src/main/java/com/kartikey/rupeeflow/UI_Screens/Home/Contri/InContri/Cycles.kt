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
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ==========================================
// PAST CYCLES EXPANDABLE SECTION
// ==========================================
@Composable
fun PastCyclesSection(
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
                                text = "No data available.", 
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
