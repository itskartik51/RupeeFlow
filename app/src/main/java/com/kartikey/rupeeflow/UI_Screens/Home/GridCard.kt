package com.kartikey.rupeeflow.UI_Screens.Home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.firestore.FirebaseFirestore
import com.kartikey.rupeeflow.UI_Screens.RupeeFlowCard
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeQuickCardsGrid(
    contriCount: Int,
    totalInvestment: Double,
    totalBankBalance: Double,
    budgetLimit: Double,
    thisMonthExpenses: Double,
    username: String,
    onContriClick: () -> Unit,
    onInvestmentClick: () -> Unit,
    onBankClick: () -> Unit,
    onBudgetSaved: () -> Unit
) {
    var showBudgetDialog by remember { mutableStateOf(false) }[cite: 3]

    fun getAnnotatedAmount(amount: Double): AnnotatedString {
        val format = NumberFormat.getNumberInstance(Locale("en", "IN"))[cite: 3]
        format.maximumFractionDigits = 2[cite: 3]
        val formattedNum = format.format(amount)[cite: 3]
        
        return buildAnnotatedString {
            append("₹ ")[cite: 3]
            append(formattedNum)[cite: 3]
        }
    }
    
    fun formatNumberOnly(amount: Double): String {
        val format = NumberFormat.getNumberInstance(Locale("en", "IN"))[cite: 3]
        format.maximumFractionDigits = 0[cite: 3]
        return format.format(amount)[cite: 3]
    }

    val invDisplayValue = if (totalInvestment > 0) getAnnotatedAmount(totalInvestment) else AnnotatedString("Add Details")[cite: 3]
    val bankDisplayValue = if (totalBankBalance > 0) getAnnotatedAmount(totalBankBalance) else AnnotatedString("Add Details")[cite: 3]
    
    val availAmount = maxOf(0.0, budgetLimit - thisMonthExpenses)[cite: 3]
    val availPct = if (budgetLimit > 0) (availAmount / budgetLimit) * 100 else 0.0[cite: 3]

    val budgetDisplayValue = if (budgetLimit > 0) {
        buildAnnotatedString {
            append("₹ ")[cite: 3]
            append(formatNumberOnly(availAmount))[cite: 3]
            withStyle(style = SpanStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)) { append(" (${String.format("%.1f", availPct)}%)") }[cite: 3]
        }
    } else {
        AnnotatedString("Add Details")[cite: 3]
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ContriDashboardCard(
                contriCount = contriCount,
                modifier = Modifier.weight(1f).bounceClick { onContriClick() }[cite: 3]
            ) 
            GridCard(
                title = "TOTAL INVESTMENT", 
                value = invDisplayValue, 
                lineColor = Color.Transparent, 
                modifier = Modifier.weight(1f),
                onClick = onInvestmentClick 
            )[cite: 3]
        }
        Spacer(modifier = Modifier.height(12.dp))[cite: 3]
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GridCard(
                title = "BANK ACCOUNTS", 
                value = bankDisplayValue, 
                lineColor = Color.Transparent, 
                modifier = Modifier.weight(1f),
                onClick = onBankClick 
            )[cite: 3]
            GridCard(
                title = "BUDGET REMAINING", 
                value = budgetDisplayValue, 
                lineColor = Color.Transparent, 
                modifier = Modifier.weight(1f),
                onClick = { showBudgetDialog = true }
            )[cite: 3]
        }
    }

    if (showBudgetDialog) {
        BudgetDialog(
            username = username,
            currentLimit = budgetLimit,
            thisMonthUsed = thisMonthExpenses,
            onDismiss = { showBudgetDialog = false },
            onSuccess = { 
                showBudgetDialog = false[cite: 3]
                onBudgetSaved()[cite: 3]
            }
        )[cite: 3]
    }
}

@Composable
fun GridCard(
    title: String, 
    value: AnnotatedString, 
    lineColor: Color, 
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {} 
) {
    RupeeFlowCard(
        modifier = modifier.bounceClick { onClick() }[cite: 4]
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)[cite: 4]
            Spacer(modifier = Modifier.height(8.dp))[cite: 4]
            
            val isAddDetails = value.text.equals("Add Details", ignoreCase = true)[cite: 4]
            Text(
                text = value, 
                fontSize = if (isAddDetails) 14.sp else 18.sp, 
                fontWeight = if (isAddDetails) FontWeight.Bold else FontWeight.ExtraBold,
                color = if (isAddDetails) Color(0xFF1976D2) else MaterialTheme.colorScheme.onSurface
            )[cite: 4]
            
            Spacer(modifier = Modifier.height(8.dp))[cite: 4]
            if (lineColor != Color.Transparent) {
                Box(modifier = Modifier.fillMaxWidth(0.6f).height(3.dp).background(lineColor, RoundedCornerShape(50)))[cite: 4]
            }
        }
    }
}

@Composable
fun ContriDashboardCard(
    contriCount: Int,
    modifier: Modifier = Modifier
) {
    RupeeFlowCard(
        modifier = modifier[cite: 3]
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)[cite: 3]
        ) {
            Text(
                text = "CONTRI",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )[cite: 3]
            Spacer(modifier = Modifier.height(8.dp))[cite: 3]
            Text(
                text = "$contriCount Contri",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )[cite: 3]
            Spacer(modifier = Modifier.height(8.dp))[cite: 3]

            Row(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val safeCount = contriCount.coerceIn(0, 5)[cite: 3]
                for (i in 1..5) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                color = if (i <= safeCount) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(50)
                            )
                    )[cite: 3]
                }
            }
        }
    }
}

@Composable
fun BudgetDialog(
    username: String,
    currentLimit: Double,
    thisMonthUsed: Double,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var limitInput by remember { mutableStateOf(if (currentLimit > 0) currentLimit.toInt().toString() else "") }[cite: 3]
    var isSaving by remember { mutableStateOf(false) }[cite: 3]
    
    val coroutineScope = rememberCoroutineScope()[cite: 3]
    val context = LocalContext.current[cite: 3]

    val displayLimit = currentLimit[cite: 3]
    val usedPct = if (displayLimit > 0) (thisMonthUsed / displayLimit) * 100 else 0.0[cite: 3]
    val availAmount = maxOf(0.0, displayLimit - thisMonthUsed) [cite: 3]
    val availPct = if (displayLimit > 0) maxOf(0.0, (availAmount / displayLimit) * 100) else 0.0[cite: 3]

    fun formatRupee(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))[cite: 3]
        format.maximumFractionDigits = 0[cite: 3]
        return format.format(amount).replace("-₹", "-₹ ")[cite: 3]
    }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },[cite: 3]
        properties = DialogProperties(dismissOnClickOutside = false)[cite: 3]
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Set Monthly Budget", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)[cite: 3]
                Spacer(modifier = Modifier.height(16.dp))[cite: 3]

                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) limitInput = it },[cite: 3]
                    label = { Text("Add Monthly Limit", color = MaterialTheme.colorScheme.onSurfaceVariant) },[cite: 3]
                    prefix = { Text("₹ ", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },[cite: 3]
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),[cite: 3]
                    singleLine = true,[cite: 3]
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary, 
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    )[cite: 3]
                )

                if (displayLimit > 0) {
                    Spacer(modifier = Modifier.height(20.dp))[cite: 3]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Used (${String.format("%.1f", usedPct)}%)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)[cite: 3]
                            Text(formatRupee(thisMonthUsed), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)[cite: 3]
                        }
                        Spacer(modifier = Modifier.height(8.dp))[cite: 3]
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Available (${String.format("%.1f", availPct)}%)", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)[cite: 3]
                            Text(formatRupee(availAmount), fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)[cite: 3]
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))[cite: 3]

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) }[cite: 3]
                    Spacer(modifier = Modifier.width(8.dp))[cite: 3]
                    Button(
                        onClick = {
                            val limitVal = limitInput.toDoubleOrNull()[cite: 3]
                            if (limitVal != null && limitVal >= 0) {
                                isSaving = true[cite: 3]
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val db = FirebaseFirestore.getInstance()[cite: 3]
                                        val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()[cite: 3]
                                        
                                        if (!userQuery.isEmpty) {
                                            val userRef = userQuery.documents[0].reference[cite: 3]
                                            
                                            userRef.update("budget_limit", limitVal).await()[cite: 3]
                                            
                                            withContext(Dispatchers.Main) {
                                                isSaving = false[cite: 3]
                                                Toast.makeText(context, "Budget Saved!", Toast.LENGTH_SHORT).show()[cite: 3]
                                                onSuccess()[cite: 3]
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                isSaving = false[cite: 3]
                                                Toast.makeText(context, "User Error!", Toast.LENGTH_SHORT).show()[cite: 3]
                                            }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isSaving = false[cite: 3]
                                            Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()[cite: 3]
                                        }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isSaving[cite: 3]
                    ) {
                        if (isSaving) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)[cite: 3]
                        else Text("Save", fontWeight = FontWeight.Bold)[cite: 3]
                    }
                }
            }
        }
    }
}
