package com.kartikey.rupeeflow.UI_Screens.Home

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
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
    investmentReturnPct: Double = 0.0,
    onContriClick: () -> Unit,
    onInvestmentClick: () -> Unit,
    onBankClick: () -> Unit,
    onBudgetSaved: () -> Unit
) {
    var showBudgetDialog by remember { mutableStateOf(false) }

    fun formatSmartScaledAmount(amount: Double): String {
        return when {
            amount < 100_000.0 -> {
                val format = NumberFormat.getNumberInstance(Locale("en", "IN"))
                format.maximumFractionDigits = 2
                format.minimumFractionDigits = 0
                format.format(amount)
            }
            amount < 1_000_000.0 -> {
                String.format(Locale.US, "%.2fK", amount / 1_000.0).replace(".00K", "K")
            }
            amount < 1_000_000_000.0 -> {
                String.format(Locale.US, "%.2fM", amount / 1_000_000.0).replace(".00M", "M")
            }
            amount < 1_000_000_000_000.0 -> {
                String.format(Locale.US, "%.2fB", amount / 1_000_000_000.0).replace(".00B", "B")
            }
            else -> {
                String.format(Locale.US, "%.2fT", amount / 1_000_000_000_000.0).replace(".00T", "T")
            }
        }
    }

    val bankDisplayValue = if (totalBankBalance > 0) "₹ ${formatSmartScaledAmount(totalBankBalance)}" else "Add Details"
    
    val availAmount = maxOf(0.0, budgetLimit - thisMonthExpenses)
    val availPct = if (budgetLimit > 0) (availAmount / budgetLimit) * 100 else 0.0

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ContriDashboardCard(
                contriCount = contriCount,
                modifier = Modifier.weight(1f).bounceClick { onContriClick() }
            ) 
            InvestmentGridCard(
                totalInvestment = totalInvestment,
                investmentReturnPct = investmentReturnPct,
                modifier = Modifier.weight(1f),
                onClick = onInvestmentClick
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GridCard(
                title = "BANK ACCOUNTS", 
                value = bankDisplayValue, 
                lineColor = Color.Transparent, 
                modifier = Modifier.weight(1f),
                onClick = onBankClick 
            )
            BudgetRemainingGridCard(
                availAmount = availAmount,
                availPct = availPct,
                isLimitSet = budgetLimit > 0,
                modifier = Modifier.weight(1f),
                onClick = { showBudgetDialog = true }
            )
        }
    }

    if (showBudgetDialog) {
        BudgetDialog(
            username = username,
            currentLimit = budgetLimit,
            thisMonthUsed = thisMonthExpenses,
            onDismiss = { showBudgetDialog = false },
            onSuccess = { 
                showBudgetDialog = false
                onBudgetSaved()
            }
        )
    }
}

@Composable
fun InvestmentGridCard(
    totalInvestment: Double,
    investmentReturnPct: Double = 0.0,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    fun formatSmartScaledAmount(amount: Double): String {
        return when {
            amount < 100_000.0 -> {
                val format = NumberFormat.getNumberInstance(Locale("en", "IN"))
                format.maximumFractionDigits = 2
                format.minimumFractionDigits = 0
                format.format(amount)
            }
            amount < 1_000_000.0 -> {
                String.format(Locale.US, "%.2fK", amount / 1_000.0).replace(".00K", "K")
            }
            amount < 1_000_000_000.0 -> {
                String.format(Locale.US, "%.2fM", amount / 1_000_000.0).replace(".00M", "M")
            }
            amount < 1_000_000_000_000.0 -> {
                String.format(Locale.US, "%.2fB", amount / 1_000_000_000.0).replace(".00B", "B")
            }
            else -> {
                String.format(Locale.US, "%.2fT", amount / 1_000_000_000_000.0).replace(".00T", "T")
            }
        }
    }

    RupeeFlowCard(
        modifier = modifier.bounceClick { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "INVESTMENT",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (totalInvestment > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "₹ ${formatSmartScaledAmount(totalInvestment)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    val returnColor = when {
                        investmentReturnPct > 0 -> Color(0xFF00E676)
                        investmentReturnPct < 0 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val returnPrefix = if (investmentReturnPct > 0) "+" else ""

                    Text(
                        text = "(${returnPrefix}${String.format(Locale.US, "%.1f", investmentReturnPct)}%)",
                        fontSize = 12.sp,
                        color = returnColor,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            } else {
                Text(
                    text = "Add Details",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun GridCard(
    title: String, 
    value: String, 
    lineColor: Color = Color.Transparent, 
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {} 
) {
    RupeeFlowCard(
        modifier = modifier.bounceClick { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title, 
                fontSize = 10.sp, 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            val isAddDetails = value.equals("Add Details", ignoreCase = true)
            Text(
                text = value, 
                fontSize = if (isAddDetails) 14.sp else 18.sp, 
                fontWeight = if (isAddDetails) FontWeight.Bold else FontWeight.ExtraBold,
                color = if (isAddDetails) Color(0xFF1976D2) else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            if (lineColor != Color.Transparent) {
                Box(modifier = Modifier.fillMaxWidth(0.6f).height(3.dp).background(lineColor, RoundedCornerShape(50)))
            }
        }
    }
}

@Composable
fun BudgetRemainingGridCard(
    availAmount: Double,
    availPct: Double,
    isLimitSet: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    fun formatNumberOnly(amount: Double): String {
        val format = NumberFormat.getNumberInstance(Locale("en", "IN"))
        format.maximumFractionDigits = 0
        return format.format(amount)
    }

    RupeeFlowCard(
        modifier = modifier.bounceClick { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "BUDGET REMAINING",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isLimitSet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "₹ ${formatNumberOnly(availAmount)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(${String.format(Locale.US, "%.1f", availPct)}%)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            } else {
                Text(
                    text = "Add Details",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ContriDashboardCard(
    contriCount: Int,
    modifier: Modifier = Modifier
) {
    val safeCount = contriCount.coerceIn(0, 5)
    val targetProgress = (safeCount / 5f).coerceIn(0f, 1f)

    var animatedProgress by remember { mutableFloatStateOf(0f) }
    val progress by animateFloatAsState(
        targetValue = animatedProgress,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "ContriProgressAnim"
    )

    LaunchedEffect(targetProgress) {
        animatedProgress = targetProgress
    }

    val headerTitle = if (contriCount > 0) "CONTRI ($safeCount/5)" else "CONTRI"

    RupeeFlowCard(
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = headerTitle,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$contriCount Contri",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(Color(0xFF00E676), RoundedCornerShape(50))
                )
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
    var limitInput by remember { mutableStateOf(if (currentLimit > 0) currentLimit.toInt().toString() else "") }
    var isSaving by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val displayLimit = currentLimit
    val usedPct = if (displayLimit > 0) (thisMonthUsed / displayLimit) * 100 else 0.0
    val availAmount = maxOf(0.0, displayLimit - thisMonthUsed) 
    val availPct = if (displayLimit > 0) maxOf(0.0, (availAmount / displayLimit) * 100) else 0.0

    fun formatRupee(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        format.maximumFractionDigits = 0
        return format.format(amount).replace("-₹", "-₹ ")
    }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Set Monthly Budget", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) limitInput = it },
                    label = { Text("Add Monthly Limit", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    prefix = { Text("₹ ", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary, 
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                if (displayLimit > 0) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Used (${String.format(Locale.US, "%.1f", usedPct)}%)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                            Text(formatRupee(thisMonthUsed), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Available (${String.format(Locale.US, "%.1f", availPct)}%)", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                            Text(formatRupee(availAmount), fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val limitVal = limitInput.toDoubleOrNull()
                            if (limitVal != null && limitVal >= 0) {
                                isSaving = true
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val db = FirebaseFirestore.getInstance()
                                        val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                                        
                                        if (!userQuery.isEmpty) {
                                            val userRef = userQuery.documents[0].reference
                                            
                                            userRef.update("budget_limit", limitVal).await()
                                            
                                            withContext(Dispatchers.Main) {
                                                isSaving = false
                                                Toast.makeText(context, "Budget Saved!", Toast.LENGTH_SHORT).show()
                                                onSuccess()
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                isSaving = false
                                                Toast.makeText(context, "User Error!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isSaving = false
                                            Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
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
                        enabled = !isSaving
                    ) {
                        if (isSaving) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
