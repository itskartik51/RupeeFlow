package com.kartikey.rupeeflow.UI_Screens.Home.Contri.InContri

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import kotlin.math.abs

// ==========================================
// SETTLEMENT CALCULATION ALGORITHM
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
        val debtAmount = abs(debtors[debtor] ?: 0.0)
        val creditAmount = creditors[creditor] ?: 0.0
        val settledAmount = minOf(debtAmount, creditAmount)
        
        settlements.add(Settlement(from = debtor, to = creditor, amount = settledAmount))
        
        val newDebt = (debtors[debtor] ?: 0.0) + settledAmount
        debtors[debtor] = newDebt
        if (newDebt > -0.01) debtors.remove(debtor)

        val newCredit = (creditors[creditor] ?: 0.0) - settledAmount
        creditors[creditor] = newCredit
        if (newCredit < 0.01) creditors.remove(creditor)
    }
    return Pair(myNetBalance, settlements)
}

// ==========================================
// SETTLE UP POPUP DIALOG
// ==========================================
@Composable
fun SettleUpDialog(
    myName: String, 
    allMemberNames: List<String>, 
    ledgers: List<MemberLedger>, 
    totalExpense: Double, 
    onDismiss: () -> Unit
) {
    val (_, allSettlements) = calculateUserSettlements(ledgers, totalExpense)
    
    val formattedNameMap = ledgers.associate { ledger ->
        val isMe = ledger.memberName == myName
        val disp = if (isMe) "You" else formatMemberDisplayName(ledger.memberName, allMemberNames)
        ledger.memberName to disp
    }

    val sortedSettlements = allSettlements.sortedWith(
        compareByDescending<Settlement> { it.from == myName || it.to == myName }
            .thenByDescending { it.amount }
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Total: ₹${totalExpense.toInt()}", 
                        fontSize = 28.sp, 
                        fontWeight = FontWeight.ExtraBold, 
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), 
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    if (sortedSettlements.isEmpty()) {
                        Text(
                            text = "No pending payments.", 
                            fontSize = 14.sp, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant, 
                            textAlign = TextAlign.Center, 
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        sortedSettlements.forEach { settlement ->
                            val fromText = formattedNameMap[settlement.from] ?: settlement.from
                            val toText = formattedNameMap[settlement.to] ?: settlement.to
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
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), 
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick { onDismiss() }
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Dismiss", 
                        color = MaterialTheme.colorScheme.onPrimary, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
