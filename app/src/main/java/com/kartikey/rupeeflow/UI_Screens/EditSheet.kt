package com.kartikey.rupeeflow.UI_Screens

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
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
import com.kartikey.rupeeflow.UI_Screens.Assets.BankAccountItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.CreditCardItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.FDItem
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
fun EditBankDialog(bank: BankAccountItem, username: String, onDismiss: () -> Unit, onUpdateSuccess: () -> Unit) {
    var bankName by remember { mutableStateOf(bank.bankName) }
    var bankBalance by remember { mutableStateOf(bank.currentBalance.toString()) }
    var interestRate by remember { mutableStateOf(bank.interestRate.toString()) }
    var expanded by remember { mutableStateOf(false) }
    val filteredBanks = if (bankName.isNotBlank()) Constants.IndianBanksList.filter { it.contains(bankName, ignoreCase = true) && !it.equals(bankName, ignoreCase = true) } else emptyList()
    val showDropdown = expanded && filteredBanks.isNotEmpty()
    var isSubmitting by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isUpdatePressed by remember { mutableStateOf(false) }
    val updateButtonScale by animateFloatAsState(targetValue = if (isUpdatePressed) 0.95f else 1f, label = "UpdateAnim")
    var isCancelPressed by remember { mutableStateOf(false) }
    val cancelButtonScale by animateFloatAsState(targetValue = if (isCancelPressed) 0.95f else 1f, label = "CancelAnim")

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteConfirm = false },
            title = { Text("Delete Bank Account", fontWeight = FontWeight.Bold, color = Color.Black) },
            text = { Text("Are you sure you want to permanently remove this bank account? This action cannot be undone.", color = Color.DarkGray) },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val jsonBody = JSONObject().apply { put("action", "delete_data"); put("username", username); put("data_type", "bank"); put("identifier", bank.accountNo) }
                                val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(jsonBody.toString().toRequestBody("application/json".toMediaType())).build()
                                val response = OkHttpClient().newCall(request).execute()
                                val resData = response.body?.string() ?: ""
                                withContext(Dispatchers.Main) {
                                    isDeleting = false
                                    if (resData.contains("success")) { Toast.makeText(context, "Bank Account Removed!", Toast.LENGTH_SHORT).show(); showDeleteConfirm = false; onUpdateSuccess() } 
                                    else { Toast.makeText(context, "Failed to delete account!", Toast.LENGTH_SHORT).show() }
                                }
                            } catch (e: Exception) { withContext(Dispatchers.Main) { isDeleting = false; Toast.makeText(context, "Error deleting bank", Toast.LENGTH_SHORT).show() } }
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) { if (isDeleting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Delete", color = Color.White, fontWeight = FontWeight.Bold) }
            }, dismissButton = { TextButton(onClick = { if (!isDeleting) showDeleteConfirm = false }) { Text("Cancel", color = Color.Gray, fontWeight = FontWeight.Bold) } }
        )
    }

    Dialog(onDismissRequest = { if (!isSubmitting && !isDeleting) onDismiss() }, properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)) {
        Card(modifier = Modifier.fillMaxWidth().padding(8.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(8.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Edit Bank Details", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                    IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Outlined.Delete, contentDescription = "Delete Bank", tint = Color(0xFFD32F2F)) }
                }
                Spacer(modifier = Modifier.height(16.dp))
                ExposedDropdownMenuBox(expanded = showDropdown, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(value = bankName, onValueChange = { bankName = it; expanded = true }, label = { Text("Bank Name") }, modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32)))
                    if (showDropdown) { ExposedDropdownMenu(expanded = showDropdown, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color.White)) { filteredBanks.forEach { selectionOption -> DropdownMenuItem(text = { Text(selectionOption, color = Color.Black) }, onClick = { bankName = selectionOption; expanded = false }) } } }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = bankBalance, onValueChange = { bankBalance = it }, label = { Text("Balance") }, prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = Color.Black) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32)))
                    OutlinedTextField(value = interestRate, onValueChange = { interestRate = it }, label = { Text("Interest (Yr)") }, suffix = { Text("%") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32)))
                }
                Spacer(modifier = Modifier.height(28.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { if (!isSubmitting) onDismiss() }, modifier = Modifier.weight(1f).height(50.dp).scale(cancelButtonScale).pointerInput(Unit) { detectTapGestures(onPress = { isCancelPressed = true; tryAwaitRelease(); isCancelPressed = false }) }, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.LightGray), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)) { Text("Cancel", fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = {
                            val newBal = bankBalance.toDoubleOrNull()
                            val newRate = interestRate.toDoubleOrNull()
                            if (bankName.isNotBlank() && newBal != null && newRate != null) {
                                if (!Constants.IndianBanksList.contains(bankName)) { Toast.makeText(context, "Please select a valid bank from the dropdown!", Toast.LENGTH_SHORT).show(); return@Button }
                                isSubmitting = true
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val jsonBody = JSONObject().apply { put("action", "edit_bank"); put("username", username); put("original_account_no", bank.accountNo); put("new_bank_name", bankName); put("new_account_no", bank.accountNo); put("new_current_bal", newBal); put("new_interest_rate", newRate) }
                                        val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(jsonBody.toString().toRequestBody("application/json".toMediaType())).build()
                                        val response = OkHttpClient().newCall(request).execute()
                                        val resData = response.body?.string() ?: ""
                                        withContext(Dispatchers.Main) {
                                            isSubmitting = false
                                            if (resData.contains("success")) { Toast.makeText(context, "Bank Details Updated!", Toast.LENGTH_SHORT).show(); onUpdateSuccess() } 
                                            else { Toast.makeText(context, "Update Failed!", Toast.LENGTH_SHORT).show() }
                                        }
                                    } catch (e: Exception) { withContext(Dispatchers.Main) { isSubmitting = false; Toast.makeText(context, "Error updating bank", Toast.LENGTH_SHORT).show() } }
                                }
                            } else { Toast.makeText(context, "Please enter valid details", Toast.LENGTH_SHORT).show() }
                        }, modifier = Modifier.weight(1f).height(50.dp).scale(updateButtonScale).pointerInput(Unit) { detectTapGestures(onPress = { isUpdatePressed = true; tryAwaitRelease(); isUpdatePressed = false }) }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)), enabled = !isSubmitting
                    ) { if (isSubmitting) { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 3.dp) } else { Text("Update", fontWeight = FontWeight.Bold, color = Color.White) } }
                }
            }
        }
    }
}

// THE NEW POP-UP (Same logic as BankAccounts.kt but controlled here)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickUpdateCCDialog(cc: CreditCardItem, username: String, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var updateAmount by remember { mutableStateOf("") }
    var isUpdating by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    Dialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f).imePadding(), 
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Update Outstanding", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black)
                Text("Add spend (+) or pay bill (-) on ${cc.issuer}", color = Color.Gray, fontSize = 13.sp)
                
                Spacer(modifier = Modifier.height(20.dp))
                
                OutlinedTextField(
                    value = updateAmount,
                    onValueChange = { updateAmount = it },
                    label = { Text("Amount (+ or -)") },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = Color.Black) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { if (!isUpdating) onDismiss() },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = {
                            val amountEntered = updateAmount.toDoubleOrNull()
                            if (amountEntered != null && amountEntered != 0.0) {
                                isUpdating = true
                                val newCalculatedOutstanding = cc.outstanding + amountEntered 
                                
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val jsonBody = JSONObject().apply {
                                            put("action", "edit_cc")
                                            put("username", username)
                                            put("original_card_no", cc.cardNo)
                                            put("new_outstanding", newCalculatedOutstanding)
                                        }
                                        val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(jsonBody.toString().toRequestBody("application/json".toMediaType())).build()
                                        val response = OkHttpClient().newCall(request).execute()
                                        val resData = response.body?.string() ?: ""

                                        withContext(Dispatchers.Main) {
                                            isUpdating = false
                                            if (resData.contains("success")) { Toast.makeText(context, "Card Updated!", Toast.LENGTH_SHORT).show(); onSuccess() } 
                                            else { Toast.makeText(context, "Update Failed!", Toast.LENGTH_SHORT).show() }
                                        }
                                    } catch (e: Exception) { withContext(Dispatchers.Main) { isUpdating = false; Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show() } }
                                }
                            } else { Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show() }
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                        enabled = !isUpdating
                    ) {
                        if (isUpdating) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                        else Text("Update", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// THE NEW FULL EDIT UI FOR CREDIT CARD
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCreditCardDialog(cc: CreditCardItem, username: String, onDismiss: () -> Unit, onUpdateSuccess: () -> Unit) {
    var limit by remember { mutableStateOf(cc.limit.toString()) }
    var billingDay by remember { mutableStateOf(cc.billingDay.toString()) }
    var dueDay by remember { mutableStateOf(cc.dueDay.toString()) }
    var annualFee by remember { mutableStateOf(cc.annualFee.toString()) }
    
    var isSubmitting by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteConfirm = false },
            title = { Text("Remove Card", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete this Credit Card record?", color = Color.DarkGray) },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val jsonBody = JSONObject().apply { put("action", "delete_data"); put("username", username); put("data_type", "cc"); put("identifier", cc.cardNo) }
                                val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(jsonBody.toString().toRequestBody("application/json".toMediaType())).build()
                                val response = OkHttpClient().newCall(request).execute()
                                val resData = response.body?.string() ?: ""
                                withContext(Dispatchers.Main) {
                                    isDeleting = false
                                    if (resData.contains("success")) { Toast.makeText(context, "Card Removed!", Toast.LENGTH_SHORT).show(); showDeleteConfirm = false; onUpdateSuccess() } 
                                    else { Toast.makeText(context, "Failed to delete card!", Toast.LENGTH_SHORT).show() }
                                }
                            } catch (e: Exception) { withContext(Dispatchers.Main) { isDeleting = false; Toast.makeText(context, "Error deleting", Toast.LENGTH_SHORT).show() } }
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) { if (isDeleting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Delete", color = Color.White) }
            }, dismissButton = { TextButton(onClick = { if (!isDeleting) showDeleteConfirm = false }) { Text("Cancel", color = Color.Gray) } }
        )
    }

    Dialog(onDismissRequest = { if (!isSubmitting && !isDeleting) onDismiss() }, properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)) {
        Card(modifier = Modifier.fillMaxWidth().padding(8.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(8.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Edit Credit Card", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                    IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Outlined.Delete, contentDescription = "Delete CC", tint = Color(0xFFD32F2F)) }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(value = limit, onValueChange = { limit = it }, label = { Text("Total Limit") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = billingDay, onValueChange = { billingDay = it }, label = { Text("Bill Day (1-31)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = dueDay, onValueChange = { dueDay = it }, label = { Text("Due Day (1-31)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(value = annualFee, onValueChange = { annualFee = it }, label = { Text("Annual Fee") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                
                Spacer(modifier = Modifier.height(28.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { if (!isSubmitting) onDismiss() }, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp)) { Text("Cancel", color = Color.Black, fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = {
                            val newLimit = limit.toDoubleOrNull()
                            if (newLimit != null) {
                                isSubmitting = true
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val jsonBody = JSONObject().apply { 
                                            put("action", "edit_cc"); put("username", username); put("original_card_no", cc.cardNo)
                                            put("new_limit", newLimit); put("new_billing_day", billingDay.toIntOrNull()?:0); put("new_due_day", dueDay.toIntOrNull()?:0); put("new_annual_fee", annualFee.toDoubleOrNull()?:0.0) 
                                        }
                                        val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(jsonBody.toString().toRequestBody("application/json".toMediaType())).build()
                                        val response = OkHttpClient().newCall(request).execute()
                                        val resData = response.body?.string() ?: ""
                                        withContext(Dispatchers.Main) {
                                            isSubmitting = false
                                            if (resData.contains("success")) { Toast.makeText(context, "Card Updated!", Toast.LENGTH_SHORT).show(); onUpdateSuccess() } 
                                            else { Toast.makeText(context, "Update Failed!", Toast.LENGTH_SHORT).show() }
                                        }
                                    } catch (e: Exception) { withContext(Dispatchers.Main) { isSubmitting = false; Toast.makeText(context, "Error updating", Toast.LENGTH_SHORT).show() } }
                                }
                            } else { Toast.makeText(context, "Check inputs!", Toast.LENGTH_SHORT).show() }
                        }, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)), enabled = !isSubmitting
                    ) { if (isSubmitting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text("Save", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// THE NEW FULL EDIT UI FOR FIXED DEPOSIT
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFDDialog(fd: FDItem, username: String, onDismiss: () -> Unit, onUpdateSuccess: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteConfirm = false },
            title = { Text("Break / Delete FD", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this Fixed Deposit record?", color = Color.DarkGray) },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val jsonBody = JSONObject().apply { put("action", "delete_data"); put("username", username); put("data_type", "fd"); put("identifier", fd.accountNo) }
                                val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(jsonBody.toString().toRequestBody("application/json".toMediaType())).build()
                                val response = OkHttpClient().newCall(request).execute()
                                val resData = response.body?.string() ?: ""
                                withContext(Dispatchers.Main) {
                                    isDeleting = false
                                    if (resData.contains("success")) { Toast.makeText(context, "FD Removed!", Toast.LENGTH_SHORT).show(); showDeleteConfirm = false; onUpdateSuccess() } 
                                    else { Toast.makeText(context, "Failed to delete FD!", Toast.LENGTH_SHORT).show() }
                                }
                            } catch (e: Exception) { withContext(Dispatchers.Main) { isDeleting = false; Toast.makeText(context, "Error deleting", Toast.LENGTH_SHORT).show() } }
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) { if (isDeleting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp)) else Text("Delete", color = Color.White) }
            }, dismissButton = { TextButton(onClick = { if (!isDeleting) showDeleteConfirm = false }) { Text("Cancel", color = Color.Gray) } }
        )
    }

    Dialog(onDismissRequest = { if (!isDeleting) onDismiss() }, properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)) {
        Card(modifier = Modifier.fillMaxWidth().padding(8.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(8.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("FD Settings", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                    IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Outlined.Delete, contentDescription = "Delete FD", tint = Color(0xFFD32F2F)) }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Fixed Deposit records cannot be freely edited to maintain interest accuracy. If you need to make changes, please Delete this record and recreate a new FD.", color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(28.dp))
                OutlinedButton(onClick = { if (!isDeleting) onDismiss() }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) { Text("Close", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

suspend fun updateUserProfile(
    oldUsername: String, newName: String, newUsername: String, newMobile: String, newEmail: String, newPassword: String, newDob: String,
    onSuccess: () -> Unit, onError: (String) -> Unit 
) {
    withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("action", "edit_profile"); put("username", oldUsername)
                if (newName.isNotBlank()) put("new_name", newName)
                if (newUsername.isNotBlank()) put("new_username", newUsername)
                if (newMobile.isNotBlank()) put("new_mobile", newMobile)
                if (newEmail.isNotBlank()) put("new_email", newEmail)
                if (newPassword.isNotBlank()) put("new_password", newPassword)
                if (newDob.isNotBlank()) put("new_dob", newDob)
            }
            val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(jsonBody.toString().toRequestBody("application/json".toMediaType())).build()
            val response = OkHttpClient().newCall(request).execute()
            val resData = response.body?.string() ?: ""

            withContext(Dispatchers.Main) {
                if (resData.contains("success")) { onSuccess() } else {
                    var errorType = "unknown"
                    try { errorType = JSONObject(resData).optString("error_type", "unknown") } catch (e: Exception) { }
                    onError(errorType)
                }
            }
        } catch (e: Exception) { withContext(Dispatchers.Main) { onError("network_error") } }
    }
}
