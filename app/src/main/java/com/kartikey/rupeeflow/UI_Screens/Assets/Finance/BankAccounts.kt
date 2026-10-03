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
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
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
import java.util.Calendar

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
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Linked Banks",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.bounceClick(scaleDown = 0.94f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onRefreshClick,
                        modifier = Modifier.bounceClick(scaleDown = 0.94f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.rotate(if (isLoading) angle else 0f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (bankList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No Bank Accounts Added Yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(bankList) { bank ->
                    BankDetailCard(
                        bank = bank,
                        username = username,
                        onEditClick = onEditBankClick,
                        onRefreshRequest = onRefreshClick
                    )
                }
            }
        }
    }
}

@Composable
fun BankDetailCard(
    bank: BankAccountItem,
    username: String,
    onEditClick: (BankAccountItem) -> Unit,
    onRefreshRequest: () -> Unit
) {
    var showQuickUpdate by remember { mutableStateOf(false) }
    val logoRes = Constants.BankLogoMap[bank.bankName]

    // ========================================================
    // DYNAMIC FRONTEND INTEREST CALCULATION (ZERO DB READ LOAD)
    // ========================================================
    val bal = bank.currentBalance
    val rateYr = bank.interestRate
    val rateQtr = rateYr / 4.0

    val oneDayInt = (bal * (rateYr / 100.0)) / 365.0
    val expQtrInt = bal * (rateQtr / 100.0)
    val expYrInt = bal * (rateYr / 100.0)

    val calToday = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    val startOfQtr = remember(calToday) {
        Calendar.getInstance().apply {
            set(Calendar.MONTH, (calToday.get(Calendar.MONTH) / 3) * 3)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val daysPassedQtr = remember(calToday, startOfQtr) {
        ((calToday.timeInMillis - startOfQtr.timeInMillis) / (1000 * 60 * 60 * 24)).toInt() + 1
    }
    val accruedQtrInt = expQtrInt * (daysPassedQtr.toDouble() / 90.0)

    val startOfYear = remember(calToday) {
        Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val daysPassedYr = remember(calToday, startOfYear) {
        ((calToday.timeInMillis - startOfYear.timeInMillis) / (1000 * 60 * 60 * 24)).toInt() + 1
    }
    val accruedYrInt = expYrInt * (daysPassedYr.toDouble() / 365.0)

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
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp)),
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
                    Text(
                        text = bank.bankName.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (bank.accountNo.startsWith("XXXXX")) bank.accountNo else "XXXXX${bank.accountNo}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }

                IconButton(
                    onClick = { onEditClick(bank) },
                    modifier = Modifier.bounceClick(scaleDown = 0.94f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit Bank",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Available Balance",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatRupeeAmount(bank.currentBalance),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = { showQuickUpdate = true },
                    modifier = Modifier
                        .size(32.dp)
                        .bounceClick(scaleDown = 0.94f)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Update Balance",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem(
                        label = "Interest Rate",
                        value = "${bank.interestRate}% Yr",
                        valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        alignment = Alignment.Start
                    )
                    MetricItem(
                        label = "Exp. Qtr",
                        value = "+${formatRupeeAmount(expQtrInt)}",
                        valueColor = Color(0xFFF57C00),
                        alignment = Alignment.CenterHorizontally
                    )
                    MetricItem(
                        label = "Exp. Yearly",
                        value = "+${formatRupeeAmount(expYrInt)}",
                        valueColor = Color(0xFF1976D2),
                        alignment = Alignment.End
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem(
                        label = "1-Day Earn",
                        value = "+${formatRupeeAmount(oneDayInt)}",
                        valueColor = Color(0xFF388E3C),
                        alignment = Alignment.Start
                    )
                    MetricItem(
                        label = "Accrued Qtr",
                        value = "+${formatRupeeAmount(accruedQtrInt)}",
                        valueColor = Color(0xFF388E3C),
                        alignment = Alignment.CenterHorizontally
                    )
                    MetricItem(
                        label = "Accrued Yr",
                        value = "+${formatRupeeAmount(accruedYrInt)}",
                        valueColor = Color(0xFF388E3C),
                        alignment = Alignment.End
                    )
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
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickUpdateDialog(
    bank: BankAccountItem,
    username: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var updateAmount by remember { mutableStateOf("") }
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .imePadding(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Update Balance",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Add or deduct amount from ${bank.bankName}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )

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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .bounceClick(scaleDown = 0.94f),
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

                                onDismiss()
                                Toast.makeText(context, "Updating balance...", Toast.LENGTH_SHORT).show()

                                // Optimistic local UI state update
                                val cachedData = CacheManager.getCachedData(context, username)
                                if (cachedData != null) {
                                    val updatedList = cachedData.bankList.map {
                                        if (it.firebaseKey == bank.firebaseKey) it.copy(currentBalance = newCalculatedBalance) else it
                                    }
                                    CacheManager.updateOptimisticCache(
                                        context,
                                        username,
                                        cachedData.copy(bankList = updatedList)
                                    )
                                    onSuccess()
                                }

                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val cal = Calendar.getInstance()
                                        val day = cal.get(Calendar.DAY_OF_MONTH)
                                        val month = cal.get(Calendar.MONTH) // 0..11
                                        val currentYear = cal.get(Calendar.YEAR)

                                        val blockIndex = when {
                                            day <= 6 -> 0
                                            day <= 12 -> 1
                                            day <= 18 -> 2
                                            day <= 24 -> 3
                                            else -> 4
                                        }
                                        val dayIndexInBlock = when {
                                            day <= 6 -> day - 1
                                            day <= 12 -> day - 7
                                            day <= 18 -> day - 13
                                            day <= 24 -> day - 19
                                            else -> day - 25
                                        }
                                        val monthInQtr = month % 3
                                        val qtrIndex = month / 3

                                        val db = FirebaseFirestore.getInstance()
                                        val userQuery = db.collection("Users")
                                            .whereEqualTo("username", username)
                                            .get()
                                            .await()

                                        if (!userQuery.isEmpty) {
                                            val userRef = userQuery.documents[0].reference
                                            val bankDocRef = userRef.collection("Finances").document("Bank")
                                            val bankDoc = bankDocRef.get().await()

                                            if (bankDoc.exists()) {
                                                val bankData = bankDoc.get(bank.firebaseKey) as? Map<*, *>
                                                val existingPbook = (bankData?.get("pbook") as? Map<*, *>) ?: emptyMap<String, Any>()

                                                val lastUpdatedTs = bankDoc.get("last_updated") as? Timestamp
                                                var isNewMonth = false
                                                var isNewQtr = false
                                                var isNewYear = false
                                                var isNewBlock = false

                                                if (lastUpdatedTs != null) {
                                                    val calLast = Calendar.getInstance().apply { time = lastUpdatedTs.toDate() }
                                                    val lastYear = calLast.get(Calendar.YEAR)
                                                    val lastMonth = calLast.get(Calendar.MONTH)
                                                    val lastDay = calLast.get(Calendar.DAY_OF_MONTH)
                                                    val lastBlock = when {
                                                        lastDay <= 6 -> 0
                                                        lastDay <= 12 -> 1
                                                        lastDay <= 18 -> 2
                                                        lastDay <= 24 -> 3
                                                        else -> 4
                                                    }

                                                    if (currentYear != lastYear) {
                                                        isNewYear = true
                                                        isNewQtr = true
                                                        isNewMonth = true
                                                        isNewBlock = true
                                                    } else if (qtrIndex != (lastMonth / 3)) {
                                                        isNewQtr = true
                                                        isNewMonth = true
                                                        isNewBlock = true
                                                    } else if (month != lastMonth) {
                                                        isNewMonth = true
                                                        isNewBlock = true
                                                    } else if (blockIndex != lastBlock) {
                                                        isNewBlock = true
                                                    }
                                                }

                                                val raw6dBal = (existingPbook["6d bal"] as? List<*>)?.mapNotNull { (it as? Number)?.toDouble() } ?: emptyList()
                                                val raw6dAvg = (existingPbook["6d avg"] as? List<*>)?.mapNotNull { (it as? Number)?.toDouble() } ?: emptyList()
                                                val rawMonthAvg = (existingPbook["month avg"] as? List<*>)?.mapNotNull { (it as? Number)?.toDouble() } ?: emptyList()
                                                val rawQtrAvg = (existingPbook["qtr avg"] as? List<*>)?.mapNotNull { (it as? Number)?.toDouble() } ?: emptyList()
                                                var yrAvgVal = (existingPbook["yr avg"] as? Number)?.toDouble() ?: 0.0

                                                // 1. Current 6d Bal (Size 6, or 7 if 31st)
                                                val targetBalSize = if (day == 31) 7 else 6
                                                val list6dBal = if (isNewBlock || raw6dBal.isEmpty()) {
                                                    MutableList(targetBalSize) { 0.0 }
                                                } else {
                                                    val m = raw6dBal.toMutableList()
                                                    while (m.size < targetBalSize) m.add(0.0)
                                                    m
                                                }
                                                if (dayIndexInBlock < list6dBal.size) {
                                                    list6dBal[dayIndexInBlock] = newCalculatedBalance
                                                }

                                                // 2. 6d Avg (5 slots per month)
                                                val list6dAvg = if (isNewMonth || raw6dAvg.isEmpty()) {
                                                    MutableList(5) { 0.0 }
                                                } else {
                                                    val m = raw6dAvg.toMutableList()
                                                    while (m.size < 5) m.add(0.0)
                                                    m
                                                }
                                                val activeDaysInBlock = list6dBal.subList(0, minOf(dayIndexInBlock + 1, list6dBal.size))
                                                    .filterIndexed { idx, v -> idx == dayIndexInBlock || v > 0.0 }
                                                val blockAvg = if (activeDaysInBlock.isNotEmpty()) {
                                                    activeDaysInBlock.sum() / activeDaysInBlock.size.toDouble()
                                                } else newCalculatedBalance
                                                list6dAvg[blockIndex] = blockAvg

                                                // 3. Month Avg (3 months per quarter)
                                                val listMonthAvg = if (isNewQtr || rawMonthAvg.isEmpty()) {
                                                    MutableList(3) { 0.0 }
                                                } else {
                                                    val m = rawMonthAvg.toMutableList()
                                                    while (m.size < 3) m.add(0.0)
                                                    m
                                                }
                                                val activeBlocksInMonth = list6dAvg.subList(0, minOf(blockIndex + 1, list6dAvg.size))
                                                    .filterIndexed { idx, v -> idx == blockIndex || v > 0.0 }
                                                val monthAvgCalc = if (activeBlocksInMonth.isNotEmpty()) {
                                                    activeBlocksInMonth.sum() / activeBlocksInMonth.size.toDouble()
                                                } else blockAvg
                                                listMonthAvg[monthInQtr] = monthAvgCalc

                                                // 4. Qtr Avg (4 quarters per year)
                                                val listQtrAvg = if (isNewYear || rawQtrAvg.isEmpty()) {
                                                    MutableList(4) { 0.0 }
                                                } else {
                                                    val m = rawQtrAvg.toMutableList()
                                                    while (m.size < 4) m.add(0.0)
                                                    m
                                                }
                                                val activeMonthsInQtr = listMonthAvg.subList(0, minOf(monthInQtr + 1, listMonthAvg.size))
                                                    .filterIndexed { idx, v -> idx == monthInQtr || v > 0.0 }
                                                val qtrAvgCalc = if (activeMonthsInQtr.isNotEmpty()) {
                                                    activeMonthsInQtr.sum() / activeMonthsInQtr.size.toDouble()
                                                } else monthAvgCalc
                                                listQtrAvg[qtrIndex] = qtrAvgCalc

                                                // 5. Yr Avg (current year average)
                                                val activeQtrsInYear = listQtrAvg.subList(0, minOf(qtrIndex + 1, listQtrAvg.size))
                                                    .filterIndexed { idx, v -> idx == qtrIndex || v > 0.0 }
                                                yrAvgVal = if (activeQtrsInYear.isNotEmpty()) {
                                                    activeQtrsInYear.sum() / activeQtrsInYear.size.toDouble()
                                                } else qtrAvgCalc

                                                val updatedPbook = hashMapOf<String, Any>(
                                                    "6d bal" to list6dBal,
                                                    "6d avg" to list6dAvg,
                                                    "month avg" to listMonthAvg,
                                                    "qtr avg" to listQtrAvg,
                                                    "yr avg" to yrAvgVal
                                                )

                                                // Clean Lean Firestore Write
                                                bankDocRef.update(
                                                    "${bank.firebaseKey}.bal", newCalculatedBalance,
                                                    "${bank.firebaseKey}.pbook", updatedPbook,
                                                    "last_updated", FieldValue.serverTimestamp()
                                                ).await()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Sync Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .bounceClick(scaleDown = 0.94f),
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
