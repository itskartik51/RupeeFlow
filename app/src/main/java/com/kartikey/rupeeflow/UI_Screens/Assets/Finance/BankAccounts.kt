package com.kartikey.rupeeflow.UI_Screens.Assets.Finance

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.firestore.FirebaseFirestore
import com.kartikey.rupeeflow.Cloud_Database.Constants
import com.kartikey.rupeeflow.UI_Screens.Assets.BankAccountItem
import com.kartikey.rupeeflow.UI_Screens.CacheManager
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

// HELPER: Indian Rupee formatting
fun formatRupeeAmount(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return formatter.format(amount).replace("₹", "").trim()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankAccountsScreen(
    onBackClick: () -> Unit,
    username: String, 
    bankList: List<BankAccountItem>,
    isLoading: Boolean,
    onRefreshClick: () -> Unit,
    onEditBankClick: (BankAccountItem) -> Unit 
) {
    val infiniteTransition = rememberInfiniteTransition(label = "refresh")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "spin"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Linked Banks", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = { 
                    IconButton(onClick = onBackClick, modifier = Modifier.bounceClick()) { 
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface) 
                    } 
                },
                actions = { 
                    IconButton(onClick = onRefreshClick, modifier = Modifier.bounceClick()) { 
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.rotate(if (isLoading) angle else 0f)) 
                    } 
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (bankList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No Bank Accounts Added Yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp), 
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(bankList) { bank ->
                    BankDetailCard(bank = bank, username = username, onEditClick = onEditBankClick, onRefreshRequest = onRefreshClick)
                }
            }
        }
    }
}

@Composable
fun BankDetailCard(bank: BankAccountItem, username: String, onEditClick: (BankAccountItem) -> Unit, onRefreshRequest: () -> Unit) {
    var showQuickUpdate by remember { mutableStateOf(false) }
    val logoRes = Constants.BankLogoMap[bank.bankName]
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF1976D2).copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (logoRes != null) {
                        Image(
                            painter = painterResource(id = logoRes),
                            contentDescription = bank.bankName,
                            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalance, 
                            contentDescription = "Bank Fallback", 
                            tint = Color(0xFF1976D2), 
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = bank.bankName.uppercase(), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = bank.accountNo, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, letterSpacing = 1.sp)
                }
                
                IconButton(onClick = { onEditClick(bank) }, modifier = Modifier.bounceClick()) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit Bank", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(text = "Available Balance", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = formatRupeeAmount(bank.currentBalance), fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurface)
                
                IconButton(
                    onClick = { showQuickUpdate = true },
                    modifier = Modifier.size(32.dp).bounceClick().background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Update Balance", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MetricItem(label = "Interest Rate", value = "${bank.interestRate}% Yr", valueColor = MaterialTheme.colorScheme.onSurfaceVariant, alignment = Alignment.Start)
                    MetricItem(label = "Exp. Qtr", value = "+${formatRupeeAmount(bank.expQtrInt)}", valueColor = Color(0xFFF57C00), alignment = Alignment.CenterHorizontally)
                    MetricItem(label = "Exp. Yearly", value = "+${formatRupeeAmount(bank.expYrInt)}", valueColor = Color(0xFF1976D2), alignment = Alignment.End)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MetricItem(label = "1-Day Earn", value = "+${formatRupeeAmount(bank.oneDayInt)}", valueColor = Color(0xFF388E3C), alignment = Alignment.Start)
                    MetricItem(label = "Accrued Qtr", value = "+${formatRupeeAmount(bank.accruedQtrInt)}", valueColor = Color(0xFF388E3C), alignment = Alignment.CenterHorizontally)
                    MetricItem(label = "Accrued Yr", value = "+${formatRupeeAmount(bank.accruedYrInt)}", valueColor = Color(0xFF388E3C), alignment = Alignment.End)
                }
            }
        }
    }
    
    if (showQuickUpdate) {
        QuickUpdateDialog(
            bank = bank,
            username = username,
            onDismiss = { showQuickUpdate = false },
            onSuccess = {
                showQuickUpdate = false
                onRefreshRequest()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickUpdateDialog(bank: BankAccountItem, username: String, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var updateAmount by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f).imePadding(), 
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Update Balance", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("Add or deduct amount from ${bank.bankName}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                
                Spacer(modifier = Modifier.height(20.dp))
                
                OutlinedTextField(
                    value = updateAmount,
                    onValueChange = { updateAmount = it },
                    label = { Text("Amount (+ or -)") },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary, 
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(50.dp).bounceClick(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = {
                            val amountEntered = updateAmount.toDoubleOrNull()
                            if (amountEntered != null && amountEntered != 0.0) {
                                val newCalculatedBalance = bank.currentBalance + amountEntered 
                                
                                val cal = Calendar.getInstance()
                                val day = cal.get(Calendar.DAY_OF_MONTH)
                                val month = cal.get(Calendar.MONTH) 
                                val qtr = (month / 3) + 1

                                val dayKey = if (day == 31) "31" else {
                                    when (day % 6) {
                                        1 -> "01, 07, 13, 19, 25"
                                        2 -> "02, 08, 14, 20, 26"
                                        3 -> "03, 09, 15, 21, 27"
                                        4 -> "04, 10, 16, 22, 28"
                                        5 -> "05, 11, 17, 23, 29"
                                        else -> "06, 12, 18, 24, 30"
                                    }
                                }
                                val avg6dKey = when {
                                    day <= 6 -> "01-06"
                                    day <= 12 -> "07-12"
                                    day <= 18 -> "13-18"
                                    day <= 24 -> "19-24"
                                    else -> "25-31"
                                }
                                val monthKey = when (month) {
                                    0, 3, 6, 9 -> "jan, april, july, oct"
                                    1, 4, 7, 10 -> "feb, may, aug, nov"
                                    else -> "march, june, sep, dec"
                                }
                                val qtrKey = "q$qtr"

                                val rateYr = bank.interestRate
                                val rateQtr = rateYr / 4.0
                                val oneDayInt = (newCalculatedBalance * (rateYr / 100.0)) / 365.0
                                
                                val expQtrInt = newCalculatedBalance * (rateQtr / 100.0)
                                val expYrInt = newCalculatedBalance * (rateYr / 100.0)
                                
                                val todayReset = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }

                                val startOfQtr = Calendar.getInstance().apply { set(Calendar.MONTH, (cal.get(Calendar.MONTH) / 3) * 3); set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                                val diffQtr = todayReset.timeInMillis - startOfQtr.timeInMillis
                                val daysPassedQtr = (diffQtr / (1000 * 60 * 60 * 24)).toInt() + 1
                                val accruedQtr = expQtrInt * (daysPassedQtr / 90.0)

                                val startOfYear = Calendar.getInstance().apply { set(Calendar.MONTH, Calendar.JANUARY); set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                                val diffYr = todayReset.timeInMillis - startOfYear.timeInMillis
                                val daysPassedYr = (diffYr / (1000 * 60 * 60 * 24)).toInt() + 1
                                val accruedYr = expYrInt * (daysPassedYr / 365.0)

                                onDismiss()
                                Toast.makeText(context, "Updating balance...", Toast.LENGTH_SHORT).show()
                                
                                val cachedData = CacheManager.getCachedData(context, username)
                                if (cachedData != null) {
                                    val updatedList = cachedData.bankList.map { if (it.firebaseKey == bank.firebaseKey) it.copy(currentBalance = newCalculatedBalance) else it }
                                    CacheManager.updateOptimisticCache(context, username, cachedData.copy(bankList = updatedList))
                                    onSuccess() 
                                }

                                // MASTERSTROKE: NO SEARCHING! DIRECT FIREBASE PATH
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val db = FirebaseFirestore.getInstance()
                                        val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                                        
                                        if (!userQuery.isEmpty) {
                                            val userRef = userQuery.documents[0].reference
                                            val bankDocRef = userRef.collection("Finances").document("Bank")
                                            
                                            val updates = hashMapOf<String, Any>(
                                                "${bank.firebaseKey}.current bal." to newCalculatedBalance,
                                                "${bank.firebaseKey}.6D bal. Block.$dayKey" to newCalculatedBalance,
                                                "${bank.firebaseKey}.6D avg.$avg6dKey" to newCalculatedBalance,
                                                "${bank.firebaseKey}.monthly avg.$monthKey" to newCalculatedBalance,
                                                "${bank.firebaseKey}.qtr. avg.$qtrKey" to newCalculatedBalance,
                                                "${bank.firebaseKey}.yr avg.cur" to newCalculatedBalance,
                                                "${bank.firebaseKey}.1d int" to oneDayInt,
                                                "${bank.firebaseKey}.exp qtr int" to expQtrInt,
                                                "${bank.firebaseKey}.accrued qtr" to accruedQtr,
                                                "${bank.firebaseKey}.exp yr int" to expYrInt,
                                                "${bank.firebaseKey}.accrued yr" to accruedYr
                                            )
                                            
                                            bankDocRef.update(updates).await()
                                        }
                                    } catch (e: Exception) {}
                                }
                            } else {
                                Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).height(50.dp).bounceClick(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Update", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, valueColor: Color, alignment: Alignment.Horizontal) {
    Column(horizontalAlignment = alignment) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = valueColor)
    }
}
