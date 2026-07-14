package com.kartikey.rupeeflow.UI_Screens.Add

import android.widget.Toast
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFinanceForm(username: String, onFinanceAdded: () -> Unit, onDismiss: () -> Unit) { // Added onDismiss
    var bankName by remember { mutableStateOf("") }
    var accountNo by remember { mutableStateOf("") }
    var currentBalance by remember { mutableStateOf("") }
    var interestRate by remember { mutableStateOf("") }
    
    var expanded by remember { mutableStateOf(false) }
    
    val filteredBanks = if (bankName.isNotBlank()) {
        Constants.IndianBanksList.filter { 
            it.contains(bankName, ignoreCase = true) && !it.equals(bankName, ignoreCase = true) 
        }
    } else {
        emptyList()
    }
    
    val showDropdown = expanded && filteredBanks.isNotEmpty()
    
    var isSubmitting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var isPressed by remember { mutableStateOf(false) }
    val buttonScale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "ButtonScale")

    Card(
        modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        // ADDED: verticalScroll physics
        Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
            
            ExposedDropdownMenuBox(
                expanded = showDropdown,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { 
                        bankName = it
                        expanded = true 
                    },
                    label = { Text("Type Bank Name (e.g. SBI)") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2E7D32),
                        focusedLabelColor = Color(0xFF2E7D32)
                    )
                )
                if (showDropdown) {
                    ExposedDropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        filteredBanks.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption, color = Color.Black) },
                                onClick = {
                                    bankName = selectionOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = accountNo,
                onValueChange = { accountNo = it },
                label = { Text("Account No. (Last 3 or 4 digits)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2E7D32),
                    focusedLabelColor = Color(0xFF2E7D32)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = currentBalance, 
                    onValueChange = { currentBalance = it },
                    label = { Text("Current Balance") },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = Color.Black) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), 
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2E7D32),
                        focusedLabelColor = Color(0xFF2E7D32)
                    )
                )
                OutlinedTextField(
                    value = interestRate, 
                    onValueChange = { interestRate = it },
                    label = { Text("Interest Rate (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), 
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2E7D32),
                        focusedLabelColor = Color(0xFF2E7D32)
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val bal = currentBalance.toDoubleOrNull() ?: 0.0
                    val rate = interestRate.toDoubleOrNull() ?: 0.0
                    
                    if (bankName.isNotBlank() && accountNo.isNotBlank() && bal > 0) {
                        
                        if (!Constants.IndianBanksList.contains(bankName)) {
                            Toast.makeText(context, "Please select a valid bank from the dropdown!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isSubmitting = true
                        onFinanceAdded()
                        
                        val formattedAcc = if (accountNo.startsWith("X")) accountNo else "XXXXX$accountNo"
                        
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val jsonBody = JSONObject().apply {
                                    put("action", "add_bank")
                                    put("username", username)
                                    put("bank_name", bankName) 
                                    put("account_no", formattedAcc)
                                    put("current_bal", bal)
                                    put("interest_rate", rate)
                                }
                                val client = OkHttpClient()
                                val body = jsonBody.toString().toRequestBody("application/json".toMediaType())
                                val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(body).build()
                                client.newCall(request).execute()

                                withContext(Dispatchers.Main) {
                                    isSubmitting = false
                                    Toast.makeText(context, "Bank Account Added to Vault!", Toast.LENGTH_SHORT).show()
                                    bankName = ""; accountNo = ""; currentBalance = ""; interestRate = "" 
                                    onDismiss() // ADDED: Auto close sheet
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isSubmitting = false
                                    Toast.makeText(context, "Error saving account", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(context, "Please fill all details correctly", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .scale(buttonScale)
                    .pointerInput(Unit) {
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
                    Text("Save Account", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}
