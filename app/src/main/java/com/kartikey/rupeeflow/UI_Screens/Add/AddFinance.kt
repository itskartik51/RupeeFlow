package com.kartikey.rupeeflow.UI_Screens.Add

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.Cloud_Database.Constants
import com.kartikey.rupeeflow.UI_Screens.CustomDatePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
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

    // Bank States
    var bankName by remember { mutableStateOf("") }
    var expandedBank by remember { mutableStateOf(false) }
    var bankAccountNo by remember { mutableStateOf("") }
    var currentBalance by remember { mutableStateOf("") }
    var bankInterestRate by remember { mutableStateOf("") }
    
    // FD States
    var fdAccountNo by remember { mutableStateOf("") }
    var fdAmount by remember { mutableStateOf("") }
    var fdInterestRate by remember { mutableStateOf("") }
    var createDateMillis by remember { mutableStateOf<Long?>(null) }
    var maturityDateMillis by remember { mutableStateOf<Long?>(null) }
    
    // Cash State
    var cashAmount by remember { mutableStateOf("") }

    // --- NEW: CREDIT CARD STATES ---
    var ccIssuer by remember { mutableStateOf("") }
    var expandedCcIssuer by remember { mutableStateOf(false) }
    var ccCardNo by remember { mutableStateOf("") }
    
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
    
    var isSubmitting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isPressed by remember { mutableStateOf(false) }
    val buttonScale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "ButtonScale")

    fun formatForSheet(millis: Long?): String {
        return if (millis == null) "" else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis))
    }

    // ==========================================
    // MODULAR LOGIC BLOCKS
    // ==========================================

    val submitBankAccount = {
        val bal = currentBalance.toDoubleOrNull() ?: 0.0
        val rate = bankInterestRate.toDoubleOrNull() ?: 0.0
        
        if (bankName.isBlank() || bankAccountNo.isBlank() || bal <= 0) {
            Toast.makeText(context, "Fill all details correctly", Toast.LENGTH_SHORT).show()
        } else if (!dynamicBankList.contains(bankName)) {
            Toast.makeText(context, "Select a valid bank from dropdown!", Toast.LENGTH_SHORT).show()
        } else {
            isSubmitting = true
            val formattedAcc = "XXXXX$bankAccountNo"
            
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val client = OkHttpClient()
                    val jsonBody = JSONObject().apply {
                        put("action", "add_bank")
                        put("username", username)
                        put("bank_name", bankName) 
                        put("account_no", formattedAcc)
                        put("current_bal", bal)
                        put("interest_rate", rate)
                    }
                    val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(jsonBody.toString().toRequestBody("application/json".toMediaType())).build()
                    client.newCall(request).execute()
                    
                    withContext(Dispatchers.Main) {
                        isSubmitting = false; Toast.makeText(context, "Bank Account Added!", Toast.LENGTH_SHORT).show(); onFinanceAdded(); onDismiss()
                    }
                } catch (e: Exception) { withContext(Dispatchers.Main) { isSubmitting = false; Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show() } }
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
            isSubmitting = true
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val client = OkHttpClient()
                    val jsonBody = JSONObject().apply {
                        put("action", "add_fd")
                        put("username", username)
                        put("bank_name", bankName) 
                        put("account_no", fdAccountNo)
                        put("invested_amount", invAmt)
                        put("interest_rate", rate)
                        put("create_date", formatForSheet(createDateMillis))
                        put("maturity_date", formatForSheet(maturityDateMillis))
                    }
                    val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(jsonBody.toString().toRequestBody("application/json".toMediaType())).build()
                    client.newCall(request).execute()
                    
                    withContext(Dispatchers.Main) {
                        isSubmitting = false; Toast.makeText(context, "FD Added Successfully!", Toast.LENGTH_SHORT).show(); onFinanceAdded(); onDismiss()
                    }
                } catch (e: Exception) { withContext(Dispatchers.Main) { isSubmitting = false; Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show() } }
            }
        }
    }

    val submitCashData = {
        val cAmt = cashAmount.toDoubleOrNull()
        if (cAmt == null || cAmt <= 0) {
            Toast.makeText(context, "Enter valid cash amount", Toast.LENGTH_SHORT).show()
        } else {
            isSubmitting = true
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val client = OkHttpClient()
                    val mediaType = "application/json".toMediaType()
                    
                    val fetchJson = JSONObject().apply { put("action", "get_all_data"); put("username", username) }
                    val fetchReq = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(fetchJson.toString().toRequestBody(mediaType)).build()
                    val res = client.newCall(fetchReq).execute()
                    val resData = res.body?.string() ?: "{}"
                    
                    var existingCash = 0.0
                    try { existingCash = JSONObject(resData).optJSONObject("cash")?.optDouble("amount", 0.0) ?: 0.0 } catch (e: Exception) { }
                    
                    val finalAmount = existingCash + cAmt
                    val updateJson = JSONObject().apply { put("action", "update_cash"); put("username", username); put("amount", finalAmount) }
                    val updateReq = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(updateJson.toString().toRequestBody(mediaType)).build()
                    client.newCall(updateReq).execute()
                    
                    withContext(Dispatchers.Main) {
                        isSubmitting = false; Toast.makeText(context, "Cash Added Successfully!", Toast.LENGTH_SHORT).show(); onFinanceAdded(); onDismiss()
                    }
                } catch (e: Exception) { withContext(Dispatchers.Main) { isSubmitting = false; Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show() } }
            }
        }
    }

    // --- NEW: SUBMIT CREDIT CARD LOGIC ---
    val submitCreditCard = {
        val limitAmt = ccLimit.toDoubleOrNull() ?: 0.0
        val billDay = ccBillingDay.toIntOrNull() ?: 0
        val dueD = ccDueDay.toIntOrNull() ?: 0
        val remindD = ccReminderDay.toIntOrNull() ?: 0
        
        if (ccIssuer.isBlank() || ccCardNo.isBlank() || ccSecurity.isBlank() || ccNetwork.isBlank() || limitAmt <= 0 || billDay == 0 || dueD == 0) {
            Toast.makeText(context, "Please fill all required card details properly.", Toast.LENGTH_LONG).show()
        } else if (!dynamicBankList.contains(ccIssuer)) {
            Toast.makeText(context, "Select a valid Issuer from dropdown!", Toast.LENGTH_SHORT).show()
        } else {
            isSubmitting = true
            // Combine Network and Security as requested (e.g., "Rupay/Secured")
            val finalType = "$ccNetwork/$ccSecurity"
            
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val client = OkHttpClient()
                    val jsonBody = JSONObject().apply {
                        put("action", "add_cc")
                        put("username", username)
                        put("issuer", ccIssuer)
                        put("card_no", "XXXXX$ccCardNo") // Formatted with XXXXX
                        put("type", finalType)
                        put("limit", limitAmt)
                        put("outstanding", 0.0) // Default for new card
                        put("billing_day", billDay)
                        put("due_day", dueD)
                        put("reminder_day", remindD)
                        put("annual_fee", 0.0) // Kept default, can be edited later
                        put("joining_fee", 0.0)
                        put("last_used", "")
                    }
                    val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(jsonBody.toString().toRequestBody("application/json".toMediaType())).build()
                    client.newCall(request).execute()
                    
                    withContext(Dispatchers.Main) {
                        isSubmitting = false; Toast.makeText(context, "Credit Card Added!", Toast.LENGTH_SHORT).show(); onFinanceAdded(); onDismiss()
                    }
                } catch (e: Exception) { withContext(Dispatchers.Main) { isSubmitting = false; Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show() } }
            }
        }
    }

    // ==========================================
    // UI RENDERING BLOCK
    // ==========================================

    Card(
        modifier = Modifier.fillMaxWidth(), 
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
            
            Text(text = "Choose Finance Type", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            ExposedDropdownMenuBox(expanded = expandedType, onExpandedChange = { expandedType = it }) {
                OutlinedTextField(
                    value = if (selectedType.isEmpty()) "Select Finance Type" else selectedType,
                    onValueChange = { }, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), unfocusedBorderColor = if (selectedType.isEmpty()) Color.Gray else Color.LightGray)
                )
                ExposedDropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }, modifier = Modifier.background(Color.White)) {
                    financeTypes.forEach { selectionOption ->
                        DropdownMenuItem(text = { Text(selectionOption, color = Color.Black) }, onClick = { selectedType = selectionOption; expandedType = false })
                    }
                }
            }

            if (selectedType.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(16.dp))

                // --- BANK ACCOUNT UI ---
                if (selectedType == "Bank Account") {
                    ExposedDropdownMenuBox(expanded = expandedBank && filteredBanks.isNotEmpty(), onExpandedChange = { expandedBank = it }) {
                        OutlinedTextField(
                            value = bankName, onValueChange = { bankName = it; expandedBank = true }, label = { Text("Bank Name") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true, shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                        )
                        if (expandedBank && filteredBanks.isNotEmpty()) {
                            ExposedDropdownMenu(expanded = expandedBank, onDismissRequest = { expandedBank = false }, modifier = Modifier.background(Color.White)) {
                                filteredBanks.forEach { selectionOption -> DropdownMenuItem(text = { Text(selectionOption, color = Color.Black) }, onClick = { bankName = selectionOption; expandedBank = false }) }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = bankAccountNo, onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) bankAccountNo = it },
                        label = { Text("Account No. (Last 3-4 Digits)") }, prefix = { Text("XXXXX", color = Color.Black, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = currentBalance, onValueChange = { currentBalance = it }, label = { Text("Balance") }, prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = Color.Black) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(0.65f), singleLine = true, shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                        )
                        OutlinedTextField(
                            value = bankInterestRate, onValueChange = { bankInterestRate = it }, label = { Text("Interest") }, suffix = { Text("%", fontWeight = FontWeight.Bold, color = Color.Black) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(0.35f), singleLine = true, shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                        )
                    }
                }

                // --- FIXED DEPOSIT UI ---
                if (selectedType == "FD : Fixed Deposit") {
                    ExposedDropdownMenuBox(expanded = expandedBank && filteredBanks.isNotEmpty(), onExpandedChange = { expandedBank = it }) {
                        OutlinedTextField(
                            value = bankName, onValueChange = { bankName = it; expandedBank = true }, label = { Text("Institution / Bank Name") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true, shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                        )
                        if (expandedBank && filteredBanks.isNotEmpty()) {
                            ExposedDropdownMenu(expanded = expandedBank, onDismissRequest = { expandedBank = false }, modifier = Modifier.background(Color.White)) {
                                filteredBanks.forEach { selectionOption -> DropdownMenuItem(text = { Text(selectionOption, color = Color.Black) }, onClick = { bankName = selectionOption; expandedBank = false }) }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = fdAccountNo, onValueChange = { fdAccountNo = it }, label = { Text("FD Account No.") }, modifier = Modifier.weight(0.6f), singleLine = true, shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                        )
                        OutlinedTextField(
                            value = fdInterestRate, onValueChange = { fdInterestRate = it }, label = { Text("Interest") }, suffix = { Text("%", fontWeight = FontWeight.Bold, color = Color.Black) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(0.4f), singleLine = true, shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = fdAmount, onValueChange = { fdAmount = it }, label = { Text("Invested Amount") }, prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = Color.Black) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CustomDatePicker(label = "Start Date", selectedDateMillis = createDateMillis, onDateSelected = { createDateMillis = it }, modifier = Modifier.weight(1f))
                        CustomDatePicker(label = "End Date", selectedDateMillis = maturityDateMillis, onDateSelected = { maturityDateMillis = it }, modifier = Modifier.weight(1f))
                    }
                }

                // --- CASH UI ---
                if (selectedType == "Cash") {
                    OutlinedTextField(
                        value = cashAmount, onValueChange = { cashAmount = it }, label = { Text("Amount to Add") }, prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = Color.Black) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("This amount will be added to your current cash balance automatically.", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 4.dp))
                }

                // --- CREDIT CARD UI ---
                if (selectedType == "Credit Card") {
                    // 1. Issuer Dropdown
                    ExposedDropdownMenuBox(expanded = expandedCcIssuer && filteredCCIssuers.isNotEmpty(), onExpandedChange = { expandedCcIssuer = it }) {
                        OutlinedTextField(
                            value = ccIssuer, onValueChange = { ccIssuer = it; expandedCcIssuer = true }, label = { Text("Issuer Bank") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true, shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                        )
                        if (expandedCcIssuer && filteredCCIssuers.isNotEmpty()) {
                            ExposedDropdownMenu(expanded = expandedCcIssuer, onDismissRequest = { expandedCcIssuer = false }, modifier = Modifier.background(Color.White)) {
                                filteredCCIssuers.forEach { selectionOption -> DropdownMenuItem(text = { Text(selectionOption, color = Color.Black) }, onClick = { ccIssuer = selectionOption; expandedCcIssuer = false }) }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Card No.
                    OutlinedTextField(
                        value = ccCardNo, onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) ccCardNo = it },
                        label = { Text("Credit Card No. (Last 4 Digits)") }, prefix = { Text("XXXXX ", color = Color.Black, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Security & Network
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ExposedDropdownMenuBox(expanded = expandedSecurity, onExpandedChange = { expandedSecurity = it }, modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = ccSecurity, onValueChange = {}, readOnly = true, label = { Text("Security") },
                                modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true, shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                            )
                            ExposedDropdownMenu(expanded = expandedSecurity, onDismissRequest = { expandedSecurity = false }, modifier = Modifier.background(Color.White)) {
                                securityOptions.forEach { opt -> DropdownMenuItem(text = { Text(opt, color = Color.Black) }, onClick = { ccSecurity = opt; expandedSecurity = false }) }
                            }
                        }
                        ExposedDropdownMenuBox(expanded = expandedNetwork, onExpandedChange = { expandedNetwork = it }, modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = ccNetwork, onValueChange = {}, readOnly = true, label = { Text("Network") },
                                modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true, shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                            )
                            ExposedDropdownMenu(expanded = expandedNetwork, onDismissRequest = { expandedNetwork = false }, modifier = Modifier.background(Color.White)) {
                                networkOptions.forEach { opt -> DropdownMenuItem(text = { Text(opt, color = Color.Black) }, onClick = { ccNetwork = opt; expandedNetwork = false }) }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. Limit
                    OutlinedTextField(
                        value = ccLimit, onValueChange = { ccLimit = it }, label = { Text("Total Limit") }, prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = Color.Black) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // 5. Days Section
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = "Days", tint = Color.Gray, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Days (1-31)", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Billing
                        ExposedDropdownMenuBox(expanded = expandedBilling, onExpandedChange = { expandedBilling = it }, modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = ccBillingDay, onValueChange = {}, readOnly = true, label = { Text("Billing") },
                                modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true, shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                            )
                            ExposedDropdownMenu(expanded = expandedBilling, onDismissRequest = { expandedBilling = false }, modifier = Modifier.background(Color.White).height(200.dp)) {
                                daysList.forEach { opt -> DropdownMenuItem(text = { Text(opt, color = Color.Black) }, onClick = { ccBillingDay = opt; expandedBilling = false }) }
                            }
                        }
                        // Due
                        ExposedDropdownMenuBox(expanded = expandedDue, onExpandedChange = { expandedDue = it }, modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = ccDueDay, onValueChange = {}, readOnly = true, label = { Text("Due") },
                                modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true, shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                            )
                            ExposedDropdownMenu(expanded = expandedDue, onDismissRequest = { expandedDue = false }, modifier = Modifier.background(Color.White).height(200.dp)) {
                                daysList.forEach { opt -> DropdownMenuItem(text = { Text(opt, color = Color.Black) }, onClick = { ccDueDay = opt; expandedDue = false }) }
                            }
                        }
                        // Reminder
                        ExposedDropdownMenuBox(expanded = expandedReminder, onExpandedChange = { expandedReminder = it }, modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = ccReminderDay, onValueChange = {}, readOnly = true, label = { Text("Reminder") },
                                modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true, shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                            )
                            ExposedDropdownMenu(expanded = expandedReminder, onDismissRequest = { expandedReminder = false }, modifier = Modifier.background(Color.White).height(200.dp)) {
                                daysList.forEach { opt -> DropdownMenuItem(text = { Text(opt, color = Color.Black) }, onClick = { ccReminderDay = opt; expandedReminder = false }) }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ================== SMART SUBMIT BUTTON ==================
                Button(
                    onClick = {
                        when (selectedType) {
                            "Bank Account" -> submitBankAccount()
                            "FD : Fixed Deposit" -> submitFixedDeposit()
                            "Cash" -> submitCashData()
                            "Credit Card" -> submitCreditCard()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).scale(buttonScale).pointerInput(Unit) {
                        detectTapGestures(onPress = { isPressed = true; tryAwaitRelease(); isPressed = false })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        val btnText = when (selectedType) { 
                            "Bank Account" -> "Add to Vault"
                            "Cash" -> "Add Cash"
                            "Credit Card" -> "Add Card"
                            else -> "Create FD" 
                        }
                        Text(btnText, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
