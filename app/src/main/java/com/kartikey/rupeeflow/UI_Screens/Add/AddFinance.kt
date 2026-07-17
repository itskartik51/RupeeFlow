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
import com.kartikey.rupeeflow.UI_Screens.CustomDatePicker // Linked our new component
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

    var bankName by remember { mutableStateOf("") }
    var expandedBank by remember { mutableStateOf(false) }
    var bankAccountNo by remember { mutableStateOf("") }
    var currentBalance by remember { mutableStateOf("") }
    var bankInterestRate by remember { mutableStateOf("") }
    
    var fdAccountNo by remember { mutableStateOf("") }
    var fdAmount by remember { mutableStateOf("") }
    var fdInterestRate by remember { mutableStateOf("") }
    
    // Modern Date State linked to CustomDatePicker
    var createDateMillis by remember { mutableStateOf<Long?>(null) }
    var maturityDateMillis by remember { mutableStateOf<Long?>(null) }
    
    var cashAmount by remember { mutableStateOf("") }

    val filteredBanks = if (bankName.isNotBlank()) {
        dynamicBankList.filter { it.contains(bankName, ignoreCase = true) && !it.equals(bankName, ignoreCase = true) }
    } else emptyList()
    
    var isSubmitting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isPressed by remember { mutableStateOf(false) }
    val buttonScale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "ButtonScale")

    // Helper to format date for Sheet Backend (YYYY-MM-DD)
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
                    val jsonBody = JSONObject()
                    jsonBody.put("action", "add_bank")
                    jsonBody.put("username", username)
                    jsonBody.put("bank_name", bankName) 
                    jsonBody.put("account_no", formattedAcc)
                    jsonBody.put("current_bal", bal)
                    jsonBody.put("interest_rate", rate)
                    
                    val mediaType = "application/json".toMediaType()
                    val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(jsonBody.toString().toRequestBody(mediaType)).build()
                    client.newCall(request).execute()
                    
                    withContext(Dispatchers.Main) {
                        isSubmitting = false
                        Toast.makeText(context, "Bank Account Added!", Toast.LENGTH_SHORT).show()
                        onFinanceAdded()
                        onDismiss()
                    }
                } catch (e: Exception) { 
                    withContext(Dispatchers.Main) { 
                        isSubmitting = false
                        Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show() 
                    } 
                }
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
                    val jsonBody = JSONObject()
                    jsonBody.put("action", "add_fd")
                    jsonBody.put("username", username)
                    jsonBody.put("bank_name", bankName) 
                    jsonBody.put("account_no", fdAccountNo)
                    jsonBody.put("invested_amount", invAmt)
                    jsonBody.put("interest_rate", rate)
                    jsonBody.put("create_date", formatForSheet(createDateMillis))
                    jsonBody.put("maturity_date", formatForSheet(maturityDateMillis))
                    
                    val mediaType = "application/json".toMediaType()
                    val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(jsonBody.toString().toRequestBody(mediaType)).build()
                    client.newCall(request).execute()
                    
                    withContext(Dispatchers.Main) {
                        isSubmitting = false
                        Toast.makeText(context, "FD Added Successfully!", Toast.LENGTH_SHORT).show()
                        onFinanceAdded()
                        onDismiss()
                    }
                } catch (e: Exception) { 
                    withContext(Dispatchers.Main) { 
                        isSubmitting = false
                        Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show() 
                    } 
                }
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
                    
                    val fetchJson = JSONObject()
                    fetchJson.put("action", "get_all_data")
                    fetchJson.put("username", username)
                    
                    val fetchReq = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(fetchJson.toString().toRequestBody(mediaType)).build()
                    val res = client.newCall(fetchReq).execute()
                    val resData = res.body?.string() ?: "{}"
                    
                    var existingCash = 0.0
                    try {
                        val parsed = JSONObject(resData)
                        existingCash = parsed.optJSONObject("cash")?.optDouble("amount", 0.0) ?: 0.0
                    } catch (e: Exception) { 
                        existingCash = 0.0
                    }
                    
                    val finalAmount = existingCash + cAmt
                    
                    val updateJson = JSONObject()
                    updateJson.put("action", "update_cash")
                    updateJson.put("username", username)
                    updateJson.put("amount", finalAmount)
                    
                    val updateReq = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(updateJson.toString().toRequestBody(mediaType)).build()
                    client.newCall(updateReq).execute()
                    
                    withContext(Dispatchers.Main) {
                        isSubmitting = false
                        Toast.makeText(context, "Cash Added Successfully!", Toast.LENGTH_SHORT).show()
                        onFinanceAdded()
                        onDismiss()
                    }
                } catch (e: Exception) { 
                    withContext(Dispatchers.Main) { 
                        isSubmitting = false
                        Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show() 
                    } 
                }
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
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2E7D32),
                        unfocusedBorderColor = if (selectedType.isEmpty()) Color.Gray else Color.LightGray
                    )
                )
                ExposedDropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }, modifier = Modifier.background(Color.White)) {
                    financeTypes.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption, color = Color.Black) },
                            onClick = { selectedType = selectionOption; expandedType = false }
                        )
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
                            value = bankName,
                            onValueChange = { bankName = it; expandedBank = true },
                            label = { Text("Bank Name") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                        )
                        if (expandedBank && filteredBanks.isNotEmpty()) {
                            ExposedDropdownMenu(expanded = expandedBank, onDismissRequest = { expandedBank = false }, modifier = Modifier.background(Color.White)) {
                                filteredBanks.forEach { selectionOption ->
                                    DropdownMenuItem(text = { Text(selectionOption, color = Color.Black) }, onClick = { bankName = selectionOption; expandedBank = false })
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = bankAccountNo,
                        onValueChange = { if (it.length <= 3 && it.all { char -> char.isDigit() }) bankAccountNo = it },
                        label = { Text("Account No. (Last 3 Digits)") },
                        prefix = { Text("XXXXX", color = Color.Black, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = currentBalance, 
                            onValueChange = { currentBalance = it },
                            label = { Text("Balance") },
                            prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = Color.Black) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.65f), 
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                        )
                        OutlinedTextField(
                            value = bankInterestRate, 
                            onValueChange = { bankInterestRate = it },
                            label = { Text("Interest") },
                            suffix = { Text("%", fontWeight = FontWeight.Bold, color = Color.Black) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.35f), 
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                        )
                    }
                }

                // --- FIXED DEPOSIT UI ---
                if (selectedType == "FD : Fixed Deposit") {
                    ExposedDropdownMenuBox(expanded = expandedBank && filteredBanks.isNotEmpty(), onExpandedChange = { expandedBank = it }) {
                        OutlinedTextField(
                            value = bankName,
                            onValueChange = { bankName = it; expandedBank = true },
                            label = { Text("Institution / Bank Name") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                        )
                        if (expandedBank && filteredBanks.isNotEmpty()) {
                            ExposedDropdownMenu(expanded = expandedBank, onDismissRequest = { expandedBank = false }, modifier = Modifier.background(Color.White)) {
                                filteredBanks.forEach { selectionOption ->
                                    DropdownMenuItem(text = { Text(selectionOption, color = Color.Black) }, onClick = { bankName = selectionOption; expandedBank = false })
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = fdAccountNo,
                            onValueChange = { fdAccountNo = it },
                            label = { Text("FD Account No.") },
                            modifier = Modifier.weight(0.6f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                        )
                        OutlinedTextField(
                            value = fdInterestRate, 
                            onValueChange = { fdInterestRate = it },
                            label = { Text("Interest") },
                            suffix = { Text("%", fontWeight = FontWeight.Bold, color = Color.Black) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.4f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = fdAmount,
                        onValueChange = { fdAmount = it },
                        label = { Text("Invested Amount") },
                        prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = Color.Black) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // --- MAGIC HAPPENS HERE: Linking the CustomDatePicker ---
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CustomDatePicker(
                            label = "Start Date",
                            selectedDateMillis = createDateMillis,
                            onDateSelected = { createDateMillis = it },
                            modifier = Modifier.weight(1f)
                        )
                        CustomDatePicker(
                            label = "End Date",
                            selectedDateMillis = maturityDateMillis,
                            onDateSelected = { maturityDateMillis = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // --- CASH UI ---
                if (selectedType == "Cash") {
                    OutlinedTextField(
                        value = cashAmount,
                        onValueChange = { cashAmount = it },
                        label = { Text("Amount to Add") },
                        prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = Color.Black) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("This amount will be added to your current cash balance automatically.", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 4.dp))
                }

                // --- CREDIT CARD UI ---
                if (selectedType == "Credit Card") {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(Color(0xFFF8F9FA), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Text("Credit Card Integration\nComing Soon...", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ================== SMART SUBMIT BUTTON ==================
                AnimatedVisibility(visible = selectedType != "Credit Card") {
                    Button(
                        onClick = {
                            if (selectedType == "Bank Account") {
                                submitBankAccount()
                            } else if (selectedType == "FD : Fixed Deposit") {
                                submitFixedDeposit()
                            } else if (selectedType == "Cash") {
                                submitCashData()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp).scale(buttonScale).pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isPressed = true
                                    tryAwaitRelease()
                                    isPressed = false
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            val btnText = when (selectedType) { "Bank Account" -> "Add to Vault"; "Cash" -> "Add Cash"; else -> "Create FD" }
                            Text(btnText, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
