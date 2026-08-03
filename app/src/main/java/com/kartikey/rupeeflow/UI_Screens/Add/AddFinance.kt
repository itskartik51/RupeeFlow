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
import java.text.SimpleDateFormat
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

    fun formatForSheet(millis: Long?): String {
        return if (millis == null) "" else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis))
    }

    val submitBankAccount = {
        val bal = currentBalance.toDoubleOrNull() ?: 0.0
        val rate = bankInterestRate.toDoubleOrNull() ?: 0.0
        
        if (bankName.isBlank() || bankAccountNo.isBlank() || bal <= 0) {
            Toast.makeText(context, "Fill all details correctly", Toast.LENGTH_SHORT).show()
        } else if (!dynamicBankList.contains(bankName)) {
            Toast.makeText(context, "Select a valid bank from dropdown!", Toast.LENGTH_SHORT).show()
        } else {
            val formattedAcc = "XXXXX$bankAccountNo"
            onFinanceAdded()
            onDismiss()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = FirebaseFirestore.getInstance()
                    val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                    if (!userQuery.isEmpty) {
                        val userRef = userQuery.documents[0].reference
                        val bankMap = hashMapOf<String, Any>(
                            "bank_name" to bankName,
                            "account_no" to formattedAcc,
                            "current_bal" to bal,
                            "interest_rate" to rate
                        )
                        userRef.collection("Finances").document("Banking_Data")
                            .set(mapOf(formattedAcc to bankMap), SetOptions.merge()).await()
                    }
                } catch (e: Exception) {}
            }
        }
    }

    val submitFixedDeposit = {
        val invAmt = fdAmount.toDoubleOrNull() ?: 0.0
        val rate = fdInterestRate.toDoubleOrNull() ?: 0.0
        
        if (bankName.isBlank() || fdAccountNo.isBlank() || invAmt <= 0 || createDateMillis == null || maturityDateMillis == null) {
            Toast.makeText(context, "Check details and select both dates.", Toast.LENGTH_LONG).show()
        } else if (!dynamicBankList.contains(bankName)) {
            Toast.makeText(context, "Select a valid institution from dropdown!", Toast.LENGTH_SHORT).show()
        } else {
            onFinanceAdded()
            onDismiss()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = FirebaseFirestore.getInstance()
                    val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                    if (!userQuery.isEmpty) {
                        val userRef = userQuery.documents[0].reference
                        val fdMap = hashMapOf<String, Any>(
                            "bank_name" to bankName,
                            "account_no" to fdAccountNo,
                            "invested_amt" to invAmt,
                            "interest_rate" to rate,
                            "create_date" to formatForSheet(createDateMillis),
                            "maturity_date" to formatForSheet(maturityDateMillis)
                        )
                        userRef.collection("Finances").document("Fixed_Deposits")
                            .set(mapOf(fdAccountNo to fdMap), SetOptions.merge()).await()
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
                        val cashDocRef = userRef.collection("Finances").document("Cash")
                        val cashDoc = cashDocRef.get().await()
                        val existingCash = cashDoc.getDouble("total_cash") ?: 0.0
                        val finalAmount = existingCash + cAmt
                        
                        val cashDataMap = hashMapOf<String, Any>(
                            "total_cash" to finalAmount,
                            "last_updated" to SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                        )
                        cashDocRef.set(cashDataMap, SetOptions.merge()).await()
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
        } else if (!dynamicBankList.contains(ccIssuer)) {
            Toast.makeText(context, "Select a valid Issuer from dropdown!", Toast.LENGTH_SHORT).show()
        } else {
            val finalType = "$ccNetwork/$ccSecurity"
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
                            "card_no" to formattedCardNo,
                            "type" to finalType,
                            "limit" to limitAmt,
                            "outstanding" to 0.0,
                            "available" to limitAmt,
                            "utilization" to 0.0,
                            "billing_day" to billDay,
                            "due_day" to dueD,
                            "reminder_day" to remindD,
                            "annual_fee" to annFee,
                            "joining_fee" to joinFee,
                            "last_used" to ""
                        )
                        userRef.collection("Finances").document("Credit_Cards")
                            .set(mapOf(formattedCardNo to ccMap), SetOptions.merge()).await()
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
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = if (selectedType.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
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
                        value = bankAccountNo, onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) bankAccountNo = it },
                        label = { Text("Account No. (Last 3-4 Digits)") }, prefix = { Text("XXXXX", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
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
