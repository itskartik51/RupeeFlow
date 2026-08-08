package com.kartikey.rupeeflow.UI_Screens.Add

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kartikey.rupeeflow.Cloud_Database.Constants
import com.kartikey.rupeeflow.UI_Screens.CustomDatePicker
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext 
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFinanceForm(username: String, onFinanceAdded: () -> Unit, onDismiss: () -> Unit) { 
    val financeTypes = listOf("Cash", "Bank Account", "FD : Fixed Deposit", "Credit Card")
    
    var selectedType by remember { mutableStateOf("") }
    var expandedType by remember { mutableStateOf(false) }

    val dynamicBankList = remember { 
        (Constants.IndianBanksList + "Utkarsh Small Finance Bank").distinct().sorted() 
    }

    var bankName by remember { mutableStateOf("") }
    var expandedBank by remember { mutableStateOf(false) }
    var bankAccountNo by remember { mutableStateOf("") }
    var currentBalance by remember { mutableStateOf("") }
    var bankInterestRate by remember { mutableStateOf("") }
    
    var fdAccountNo by remember { mutableStateOf("") }
    var fdAmount by remember { mutableStateOf("") }
    var fdInterestRate by remember { mutableStateOf("") }
    var createDateMillis by remember { mutableStateOf<Long?>(null) }
    var maturityDateMillis by remember { mutableStateOf<Long?>(null) }
    
    var cashAmount by remember { mutableStateOf("") }

    var ccIssuer by remember { mutableStateOf("") }
    var expandedCcIssuer by remember { mutableStateOf(false) }
    var ccCardNo by remember { mutableStateOf("") }
    var ccAnnualFee by remember { mutableStateOf("") }
    var ccJoiningFee by remember { mutableStateOf("") }
    
    var ccSecurity by remember { mutableStateOf("") }
    var expandedSecurity by remember { mutableStateOf(false) }
    val securityOptions = listOf("Secured", "Unsecured")
    
    var ccNetwork by remember { mutableStateOf("") }
    var expandedNetwork by remember { mutableStateOf(false) }
    val networkOptions = listOf("RuPay", "Visa", "Mastercard")
    
    var ccLimit by remember { mutableStateOf("") }
    
    val daysList = (1..31).map { it.toString() }
    var ccBillingDay by remember { mutableStateOf("") }
    var expandedBilling by remember { mutableStateOf(false) }
    var ccDueDay by remember { mutableStateOf("") }
    var expandedDue by remember { mutableStateOf(false) }
    var ccReminderDay by remember { mutableStateOf("") }
    var expandedReminder by remember { mutableStateOf(false) }

    val filteredBanks = if (bankName.isNotBlank()) {
        dynamicBankList.filter { it.contains(bankName, ignoreCase = true) && !it.equals(bankName, ignoreCase = true) }
    } else emptyList()

    val filteredCCIssuers = if (ccIssuer.isNotBlank()) {
        dynamicBankList.filter { it.contains(ccIssuer, ignoreCase = true) && !it.equals(ccIssuer, ignoreCase = true) }
    } else emptyList()
    
    val context = LocalContext.current

    val submitBankAccount = {
        val bal = currentBalance.toDoubleOrNull() ?: 0.0
        val rateYr = bankInterestRate.toDoubleOrNull() ?: 0.0
        
        if (bankName.isBlank() || bankAccountNo.length != 3 || bal <= 0) {
            Toast.makeText(context, "Fill details correctly (Acc No. must be 3 digits)", Toast.LENGTH_SHORT).show()
        } else {
            // Strict Validation removed here. User can add ANY bank name.
            val formattedAcc = "XXXXX$bankAccountNo"
            onFinanceAdded()
            onDismiss()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = FirebaseFirestore.getInstance()
                    val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                    if (!userQuery.isEmpty) {
                        val userRef = userQuery.documents[0].reference
                        
                        val rateQtr = rateYr / 4.0
                        val oneDayInt = (bal * (rateYr / 100.0)) / 365.0
                        
                        val expQtrInt = bal * (rateQtr / 100.0)
                        val expYrInt = bal * (rateYr / 100.0)
                        
                        val cal = Calendar.getInstance()
                        val todayReset = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }

                        val startOfQtr = Calendar.getInstance().apply { set(Calendar.MONTH, (cal.get(Calendar.MONTH) / 3) * 3); set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                        val diffQtr = todayReset.timeInMillis - startOfQtr.timeInMillis
                        val daysPassedQtr = (diffQtr / (1000 * 60 * 60 * 24)).toInt() + 1
                        val accruedQtr = expQtrInt * (daysPassedQtr / 90.0)

                        val startOfYear = Calendar.getInstance().apply { set(Calendar.MONTH, Calendar.JANUARY); set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                        val diffYr = todayReset.timeInMillis - startOfYear.timeInMillis
                        val daysPassedYr = (diffYr / (1000 * 60 * 60 * 24)).toInt() + 1
                        val accruedYr = expYrInt * (daysPassedYr / 365.0)

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

                        val avg6D = hashMapOf(avg6dKey to bal)
                        val balBlock6D = hashMapOf(dayKey to bal)
                        val monthlyAvg = hashMapOf(monthKey to bal)
                        val qtrAvg = hashMapOf("q$qtr" to bal)
                        val yrAvg = hashMapOf("cur" to bal)

                        val bankMap = hashMapOf<String, Any>(
                            "1d int" to oneDayInt,
                            "6D avg." to avg6D,
                            "6D bal. Block" to balBlock6D,
                            "account no." to formattedAcc,
                            "accrued qtr" to accruedQtr,
                            "accrued yr" to accruedYr,
                            "bank" to bankName,
                            "current bal." to bal,
                            "exp qtr int" to expQtrInt,
                            "exp yr int" to expYrInt,
                            "intrest % (qtr)" to rateQtr,
                            "intrest % (yr)" to rateYr,
                            "monthly avg." to monthlyAvg,
                            "qtr. avg." to qtrAvg,
                            "yr avg" to yrAvg
                        )
                        
                        val bankDocRef = userRef.collection("Finances").document("Bank")
                        val bankDoc = bankDocRef.get().await()
                        
                        var nextId = 1
                        var needsLastUpdated = true
                        
                        if (bankDoc.exists()) {
                            val data = bankDoc.data ?: emptyMap()
                            if (data.containsKey("last_updated")) {
                                needsLastUpdated = false
                            }
                            val existingIds = data.keys.mapNotNull { it.toIntOrNull() }
                            if (existingIds.isNotEmpty()) {
                                nextId = existingIds.maxOrNull()!! + 1
                            }
                        }
                        
                        val updateData = hashMapOf<String, Any>(
                            nextId.toString() to bankMap
                        )
                        
                        if (needsLastUpdated) {
                            updateData["last_updated"] = com.google.firebase.Timestamp.now()
                        }
                        
                        bankDocRef.set(updateData, SetOptions.merge()).await()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show() }
                }
            }
        }
    }

    val submitFixedDeposit = {
        val invAmt = fdAmount.toDoubleOrNull() ?: 0.0
        val rate = fdInterestRate.toDoubleOrNull() ?: 0.0
        
        if (bankName.isBlank() || fdAccountNo.isBlank() || invAmt <= 0 || createDateMillis == null || maturityDateMillis == null) {
            Toast.makeText(context, "Check details and select both dates.", Toast.LENGTH_LONG).show()
        } else {
            // Strict Validation removed here too.
            onFinanceAdded()
            onDismiss()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = FirebaseFirestore.getInstance()
                    val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                    if (!userQuery.isEmpty) {
                        val userRef = userQuery.documents[0].reference
                        
                        val fdMap = hashMapOf<String, Any>(
                            "bank" to bankName,
                            "fd ac" to fdAccountNo,
                            "amnt" to invAmt,
                            "int % yr" to rate,
                            "create" to com.google.firebase.Timestamp(Date(createDateMillis!!)),
                            "matur" to com.google.firebase.Timestamp(Date(maturityDateMillis!!))
                        )
                        
                        userRef.collection("Finances").document("CC FD").set(mapOf("FD" to mapOf(fdAccountNo to fdMap)), SetOptions.merge()).await()
                    }
                } catch (e: Exception) {}
            }
        }
    }

    val submitCashData = {
        val cAmt = cashAmount.toDoubleOrNull()
        if (cAmt == null || cAmt <= 0) {
            Toast.makeText(context, "Enter valid cash amount", Toast.LENGTH_SHORT).show()
        } else {
            onFinanceAdded()
            onDismiss()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = FirebaseFirestore.getInstance()
                    val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                    if (!userQuery.isEmpty) {
                        val userRef = userQuery.documents[0].reference
                        val bankDocRef = userRef.collection("Finances").document("Bank")
                        val bankDoc = bankDocRef.get().await()
                        
                        var existingCash = 0.0
                        if (bankDoc.exists()) {
                            val cashMap = bankDoc.get("cash") as? Map<*, *>
                            existingCash = (cashMap?.get("amnt") as? Number)?.toDouble() ?: 0.0
                        }
                        
                        val updateMap = hashMapOf<String, Any>(
                            "cash" to hashMapOf(
                                "amnt" to existingCash + cAmt,
                                "last update" to com.google.firebase.Timestamp.now()
                            )
                        )
                        bankDocRef.set(updateMap, SetOptions.merge()).await()
                    }
                } catch (e: Exception) {}
            }
        }
    }

    val submitCreditCard = {
        val limitAmt = ccLimit.toDoubleOrNull() ?: 0.0
        val billDay = ccBillingDay.toIntOrNull() ?: 0
        val dueD = ccDueDay.toIntOrNull() ?: 0
        val remindD = ccReminderDay.toIntOrNull() ?: 0
        val annFee = ccAnnualFee.toDoubleOrNull() ?: 0.0
        val joinFee = ccJoiningFee.toDoubleOrNull() ?: 0.0
        
        if (ccIssuer.isBlank() || ccCardNo.isBlank() || ccSecurity.isBlank() || ccNetwork.isBlank() || limitAmt <= 0 || billDay == 0 || dueD == 0) {
            Toast.makeText(context, "Please fill all required card details properly.", Toast.LENGTH_LONG).show()
        } else {
            val finalType = "$ccNetwork | $ccSecurity"
            val formattedCardNo = "XXXXX$ccCardNo"
            onFinanceAdded()
            onDismiss()
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = FirebaseFirestore.getInstance()
                    val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                    if (!userQuery.isEmpty) {
                        val userRef = userQuery.documents[0].reference
                        
                        val ccMap = hashMapOf<String, Any>(
                            "issuer" to ccIssuer,
                            "card no." to formattedCardNo,
                            "type" to finalType,
                            "limit" to limitAmt,
                            "outstanding" to 0.0,
                            "billing" to billDay,
                            "due" to dueD,
                            "rmndr" to remindD,
                            "last use" to com.google.firebase.Timestamp.now()
                        )
                        if (annFee > 0.0) ccMap["yr fee"] = annFee
                        if (joinFee > 0.0) ccMap["join fee"] = joinFee
                        
                        userRef.collection("Finances").document("CC FD").set(mapOf("CC" to mapOf(formattedCardNo to ccMap)), SetOptions.merge()).await()
                    }
                } catch (e: Exception) {}
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(), 
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
            
            Text(text = "Choose Finance Type", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            ExposedDropdownMenuBox(expanded = expandedType, onExpandedChange = { expandedType = it }) {
                OutlinedTextField(
                    value = if (selectedType.isEmpty()) "Select Finance Type" else selectedType,
                    onValueChange = { }, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary, 
                        unfocusedBorderColor = if (selectedType.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), 
                        focusedTextColor = MaterialTheme.colorScheme.onSurface, 
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                ExposedDropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                    financeTypes.forEach { selectionOption ->
                        DropdownMenuItem(text = { Text(selectionOption, color = MaterialTheme.colorScheme.onSurface) }, onClick = { selectedType = selectionOption; expandedType = false })
                    }
                }
            }

            if (selectedType.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                if (selectedType == "Bank Account") {
                    ExposedDropdownMenuBox(expanded = expandedBank && filteredBanks.isNotEmpty(), onExpandedChange = { expandedBank = it }) {
                        OutlinedTextField(
                            value = bankName, onValueChange = { bankName = it; expandedBank = true }, label = { Text("Bank Name") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true, shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary, focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                        )
                        if (expandedBank && filteredBanks.isNotEmpty()) {
                            ExposedDropdownMenu(expanded = expandedBank, onDismissRequest = { expandedBank = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                                filteredBanks.forEach { selectionOption -> DropdownMenuItem(text = { Text(selectionOption, color = MaterialTheme.colorScheme.onSurface) }, onClick = { bankName = selectionOption; expandedBank = false }) }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    OutlinedTextField(
                        value = bankAccountNo, 
                        onValueChange = { if (it.length <= 3 && it.all { char -> char.isDigit() }) bankAccountNo = it },
                        label = { Text("Account No. (Last 3 Digits)") }, 
                        prefix = { Text("XXXXX", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                        modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary, focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = currentBalance, onValueChange = { currentBalance = it }, label = { Text("Balance") }, prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(0.65f), singleLine = true, shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary, focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                        )
                        OutlinedTextField(
                            value = bankInterestRate, onValueChange = { bankInterestRate = it }, label = { Text("Interest") }, suffix = { Text("%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(0.35f), singleLine = true, shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary, focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                        )
                    }
                }

                if (selectedType == "FD : Fixed Deposit") {
                    ExposedDropdownMenuBox(expanded = expandedBank && filteredBanks.isNotEmpty(), onExpandedChange = { expandedBank = it }) {
                        OutlinedTextField(
                            value = bankName, onValueChange = { bankName = it; expandedBank = true }, label = { Text("Institution / Bank Name") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true, shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary, focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                        )
                        if (expandedBank && filteredBanks.isNotEmpty()) {
                            ExposedDropdownMenu(expanded = expandedBank, onDismissRequest = { expandedBank = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                                filteredBanks.forEach { selectionOption -> DropdownMenuItem(text = { Text(selectionOption, color = MaterialTheme.colorScheme.onSurface) }, onClick = { bankName = selectionOption; expandedBank = false }) }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = fdAccountNo, onValueChange = { fdAccountNo = it }, label = { Text("FD Account No.") }, modifier = Modifier.weight(0.6f), singleLine = true, shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary, focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                        )
                        OutlinedTextField(
                            value = fdInterestRate, onValueChange = { fdInterestRate = it }, label = { Text("Interest") }, suffix = { Text("%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(0.4f), singleLine = true, shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary, focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = fdAmount, onValueChange = { fdAmount = it }, label = { Text("Invested Amount") }, prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary, focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CustomDatePicker(label = "Start Date", selectedDateMillis = createDateMillis, onDateSelected = { createDateMillis = it }, modifier = Modifier.weight(1f))
                        CustomDatePicker(label = "End Date", selectedDateMillis = maturityDateMillis, onDateSelected = { maturityDateMillis = it }, modifier = Modifier.weight(1f))
                    }
                }

                if (selectedType == "Cash") {
                    OutlinedTextField(
                        value = cashAmount, onValueChange = { cashAmount = it }, label = { Text("Amount to Add") }, prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary, focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("This amount will be added to your current cash balance automatically.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 4.dp))
                }

                if (selectedType == "Credit Card") {
                    ExposedDropdownMenuBox(expanded = expandedCcIssuer && filteredCCIssuers.isNotEmpty(), onExpandedChange = { expandedCcIssuer = it }) {
                        OutlinedTextField(
                            value = ccIssuer, onValueChange = { ccIssuer = it; expandedCcIssuer = true }, label = { Text("Issuer Bank") },
                            leadingIcon = { Icon(Icons.Outlined.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true, shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary, focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                        )
                        if (expandedCcIssuer && filteredCCIssuers.isNotEmpty()) {
                            ExposedDropdownMenu(expanded = expandedCcIssuer, onDismissRequest = { expandedCcIssuer = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                                filteredCCIssuers.forEach { selectionOption -> DropdownMenuItem(text = { Text(selectionOption, color = MaterialTheme.colorScheme.onSurface) }, onClick = { ccIssuer = selectionOption; expandedCcIssuer = false }) }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = ccCardNo, onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) ccCardNo = it },
                        label = { Text("Credit Card No. (Last 4 Digits)") }, prefix = { Text("XXXXX ", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary, focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ccJoiningFee, onValueChange = { ccJoiningFee = it }, label = { Text("Joining Fee") },
                            prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary, focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                        )
                        OutlinedTextField(
                            value = ccAnnualFee, onValueChange = { ccAnnualFee = it }, label = { Text("Annual Fee") },
                            prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary, focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExposedDropdownMenuBox(expanded = expandedSecurity, onExpandedChange = { expandedSecurity = it }, modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = ccSecurity, onValueChange = {}, readOnly = true, label = { Text("Security") },
                                modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true, shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary, focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                            )
                            ExposedDropdownMenu(expanded = expandedSecurity, onDismissRequest = { expandedSecurity = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                                securityOptions.forEach { opt -> DropdownMenuItem(text = { Text(opt, color = MaterialTheme.colorScheme.onSurface) }, onClick = { ccSecurity = opt; expandedSecurity = false }) }
                            }
                        }
                        ExposedDropdownMenuBox(expanded = expandedNetwork, onExpandedChange = { expandedNetwork = it }, modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = ccNetwork, onValueChange = {}, readOnly = true, label = { Text("Network") },
                                modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true, shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary, focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                            )
                            ExposedDropdownMenu(expanded = expandedNetwork, onDismissRequest = { expandedNetwork = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                                networkOptions.forEach { opt -> DropdownMenuItem(text = { Text(opt, color = MaterialTheme.colorScheme.onSurface) }, onClick = { ccNetwork = opt; expandedNetwork = false }) }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = ccLimit, onValueChange = { ccLimit = it }, label = { Text("Total Limit") }, prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary, focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = "Days", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Days (1-31)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExposedDropdownMenuBox(expanded = expandedBilling, onExpandedChange = { expandedBilling = it }, modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = ccBillingDay, onValueChange = {}, readOnly = true, label = { Text("Billing") },
                                modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true, shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary, focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                            )
                            ExposedDropdownMenu(expanded = expandedBilling, onDismissRequest = { expandedBilling = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface).height(200.dp)) {
                                daysList.forEach { opt -> DropdownMenuItem(text = { Text(opt, color = MaterialTheme.colorScheme.onSurface) }, onClick = { ccBillingDay = opt; expandedBilling = false }) }
                            }
                        }
                        ExposedDropdownMenuBox(expanded = expandedDue, onExpandedChange = { expandedDue = it }, modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = ccDueDay, onValueChange = {}, readOnly = true, label = { Text("Due") },
                                modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true, shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary, focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                            )
                            ExposedDropdownMenu(expanded = expandedDue, onDismissRequest = { expandedDue = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface).height(200.dp)) {
                                daysList.forEach { opt -> DropdownMenuItem(text = { Text(opt, color = MaterialTheme.colorScheme.onSurface) }, onClick = { ccDueDay = opt; expandedDue = false }) }
                            }
                        }
                        ExposedDropdownMenuBox(expanded = expandedReminder, onExpandedChange = { expandedReminder = it }, modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = ccReminderDay, onValueChange = {}, readOnly = true, label = { Text("Remind") }, 
                                modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true, shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary, focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                            )
                            ExposedDropdownMenu(expanded = expandedReminder, onDismissRequest = { expandedReminder = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface).height(200.dp)) {
                                daysList.forEach { opt -> DropdownMenuItem(text = { Text(opt, color = MaterialTheme.colorScheme.onSurface) }, onClick = { ccReminderDay = opt; expandedReminder = false }) }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        when (selectedType) {
                            "Bank Account" -> submitBankAccount()
                            "FD : Fixed Deposit" -> submitFixedDeposit()
                            "Cash" -> submitCashData()
                            "Credit Card" -> submitCreditCard()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).bounceClick(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    val btnText = when (selectedType) { 
                        "Bank Account" -> "Add to Vault"
                        "Cash" -> "Add Cash"
                        "Credit Card" -> "Add Card"
                        else -> "Create FD" 
                    }
                    Text(btnText, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
