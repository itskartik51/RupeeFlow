package com.kartikey.rupeeflow.UI_Screens.Home

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.kartikey.rupeeflow.Cloud_Database.Constants
import com.kartikey.rupeeflow.R
import com.kartikey.rupeeflow.UI_Screens.bounceClick 
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeDashboardDesign(
    username: String, 
    userFullName: String, 
    paddingValues: PaddingValues, 
    thisMonthExpenses: Double, 
    thisYearExpenses: Double, 
    budgetLimit: Double,
    isLoadingExpenses: Boolean,
    dNavState: String, 
    dBackPresses: Int, 
    onLogout: () -> Unit,
    onRefreshExpenses: () -> Unit = {}, 
    onExpenseCardClick: () -> Unit,
    onContriClick: () -> Unit,
    onAvatarClick: () -> Unit, 
    contriCount: Int = 0,
    totalInvestment: Double,
    totalBankBalance: Double,
    onInvestmentClick: () -> Unit,
    onBankClick: () -> Unit,
    onBudgetSaved: () -> Unit
) {
    val context = LocalContext.current
    var showBudgetDialog by remember { mutableStateOf(false) }

    fun getAnnotatedAmount(amount: Double): AnnotatedString {
        val format = NumberFormat.getNumberInstance(Locale("en", "IN"))
        format.maximumFractionDigits = 2
        val formattedNum = format.format(amount)
        
        return buildAnnotatedString {
            append("₹ ") 
            append(formattedNum)
        }
    }
    
    fun formatNumberOnly(amount: Double): String {
        val format = NumberFormat.getNumberInstance(Locale("en", "IN"))
        format.maximumFractionDigits = 0
        return format.format(amount)
    }

    Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher), 
                    contentDescription = "App Logo", 
                    modifier = Modifier.size(46.dp).clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text("RupeeFlow", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Hi, $username", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                
                val displayLetter = if (userFullName.isNotBlank()) userFullName.take(1).uppercase() else username.take(1).uppercase()
                
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .bounceClick { onAvatarClick() }, 
                    contentAlignment = Alignment.Center
                ) {
                    Text(displayLetter, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            
            ExpenseSummaryCard(
                thisMonthExpenses = thisMonthExpenses, 
                thisYearExpenses = thisYearExpenses, 
                budgetLimit = budgetLimit,
                isLoadingExpenses = isLoadingExpenses,
                onRefreshExpenses = onRefreshExpenses, 
                onExpenseCardClick = onExpenseCardClick,
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            val invDisplayValue = if (totalInvestment > 0) getAnnotatedAmount(totalInvestment) else AnnotatedString("Add Details")
            val bankDisplayValue = if (totalBankBalance > 0) getAnnotatedAmount(totalBankBalance) else AnnotatedString("Add Details")
            
            val availAmount = maxOf(0.0, budgetLimit - thisMonthExpenses)
            val availPct = if (budgetLimit > 0) (availAmount / budgetLimit) * 100 else 0.0

            val budgetDisplayValue = if (budgetLimit > 0) {
                buildAnnotatedString {
                    append("₹ ") 
                    append(formatNumberOnly(availAmount))
                    withStyle(style = SpanStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)) { append(" (${String.format("%.1f", availPct)}%)") }
                }
            } else {
                AnnotatedString("Add Details")
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ContriDashboardCard(
                    contriCount = contriCount,
                    modifier = Modifier.weight(1f).bounceClick { onContriClick() }
                ) 
                GridCard(
                    title = "TOTAL INVESTMENT", 
                    value = invDisplayValue, 
                    lineColor = Color.Transparent, 
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
                GridCard(
                    title = "BUDGET REMAINING", 
                    value = budgetDisplayValue, 
                    lineColor = Color.Transparent, 
                    modifier = Modifier.weight(1f),
                    onClick = { showBudgetDialog = true }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            
            SpendingTrackerCard()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ReminderBanner()
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Recent Transactions", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                TextButton(onClick = onLogout) { Text("Logout", color = MaterialTheme.colorScheme.error) }
            }
            Spacer(modifier = Modifier.height(60.dp)) 
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

// ==========================================
// BUDGET DIALOG POPUP
// ==========================================
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
                            Text("Used (${String.format("%.1f", usedPct)}%)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                            Text(formatRupee(thisMonthUsed), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Available (${String.format("%.1f", availPct)}%)", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
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
                                        val jsonBody = JSONObject().apply {
                                            put("action", "update_budget")
                                            put("username", username)
                                            put("limit", limitVal)
                                        }
                                        val request = Request.Builder()
                                            .url(Constants.GOOGLE_SHEET_API_URL)
                                            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                                            .build()
                                        
                                        val response = OkHttpClient().newCall(request).execute()
                                        val resData = response.body?.string() ?: ""
                                        
                                        withContext(Dispatchers.Main) {
                                            isSaving = false
                                            if (resData.contains("success")) {
                                                Toast.makeText(context, "Budget Saved!", Toast.LENGTH_SHORT).show()
                                                onSuccess()
                                            } else {
                                                Toast.makeText(context, "Error saving budget", Toast.LENGTH_SHORT).show()
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

@Composable
fun ContriDashboardCard(
    contriCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "CONTRI",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$contriCount Contri",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val safeCount = contriCount.coerceIn(0, 5)
                for (i in 1..5) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                color = if (i <= safeCount) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(50)
                            )
                    )
                }
            }
        }
    }
}
