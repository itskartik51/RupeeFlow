package com.kartikey.rupeeflow.UI_Screens

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kartikey.rupeeflow.Cloud_Database.Constants
import com.kartikey.rupeeflow.UI_Screens.Add.IndianBanksList // Master List Import ki
import com.kartikey.rupeeflow.UI_Screens.Assets.BankAccountItem
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
fun EditBankDialog(
    bank: BankAccountItem,
    username: String,
    onDismiss: () -> Unit,
    onUpdateSuccess: () -> Unit
) {
    var bankName by remember { mutableStateOf(bank.bankName) }
    var bankBalance by remember { mutableStateOf(bank.currentBalance.toString()) }
    var interestRate by remember { mutableStateOf(bank.interestRate.toString()) }
    
    // Dropdown state for Edit Pop-up
    var expanded by remember { mutableStateOf(false) }
    val filteredBanks = IndianBanksList.filter { it.contains(bankName, ignoreCase = true) }
    
    var isSubmitting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var isUpdatePressed by remember { mutableStateOf(false) }
    val updateButtonScale by animateFloatAsState(targetValue = if (isUpdatePressed) 0.95f else 1f, label = "UpdateAnim")
    
    var isCancelPressed by remember { mutableStateOf(false) }
    val cancelButtonScale by animateFloatAsState(targetValue = if (isCancelPressed) 0.95f else 1f, label = "CancelAnim")

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Edit Bank Details",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Searchable Premium Dropdown in Edit Pop-up
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { 
                            bankName = it
                            expanded = true 
                        },
                        label = { Text("Bank Name") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2E7D32),
                            focusedLabelColor = Color(0xFF2E7D32)
                        )
                    )
                    if (filteredBanks.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = expanded,
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
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = bankBalance,
                        onValueChange = { bankBalance = it },
                        label = { Text("Balance") },
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
                        label = { Text("Interest (Yr)") },
                        suffix = { Text("%") },
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

                Spacer(modifier = Modifier.height(28.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { if (!isSubmitting) onDismiss() },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .scale(cancelButtonScale)
                            .pointerInput(Unit) {
                                detectTapGestures(onPress = { isCancelPressed = true; tryAwaitRelease(); isCancelPressed = false })
                            },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.LightGray),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val newBal = bankBalance.toDoubleOrNull()
                            val newRate = interestRate.toDoubleOrNull()
                            
                            if (bankName.isNotBlank() && newBal != null && newRate != null) {
                                
                                // STRICT VALIDATION in Edit Mode
                                if (!IndianBanksList.contains(bankName)) {
                                    Toast.makeText(context, "Please select a valid bank from the dropdown!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                isSubmitting = true
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val jsonBody = JSONObject().apply {
                                            put("action", "edit_bank")
                                            put("username", username)
                                            put("original_account_no", bank.accountNo)
                                            put("new_bank_name", bankName)
                                            put("new_account_no", bank.accountNo)
                                            put("new_current_bal", newBal)
                                            put("new_interest_rate", newRate)
                                        }
                                        
                                        val client = OkHttpClient()
                                        val body = jsonBody.toString().toRequestBody("application/json".toMediaType())
                                        val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(body).build()
                                        val response = client.newCall(request).execute()
                                        val resData = response.body?.string() ?: ""

                                        withContext(Dispatchers.Main) {
                                            isSubmitting = false
                                            if (resData.contains("success")) {
                                                Toast.makeText(context, "Bank Details Updated!", Toast.LENGTH_SHORT).show()
                                                onUpdateSuccess()
                                            } else {
                                                Toast.makeText(context, "Update Failed!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isSubmitting = false
                                            Toast.makeText(context, "Error updating bank", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Please enter valid details", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .scale(updateButtonScale)
                            .pointerInput(Unit) {
                                detectTapGestures(onPress = { isUpdatePressed = true; tryAwaitRelease(); isUpdatePressed = false })
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                        enabled = !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                        } else {
                            Text("Update", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
