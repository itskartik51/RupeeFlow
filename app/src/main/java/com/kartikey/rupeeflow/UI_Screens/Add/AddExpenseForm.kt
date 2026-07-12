package com.kartikey.rupeeflow.UI_Screens.Add

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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
import com.kartikey.rupeeflow.UI_Screens.AddExpense.TransactionModel
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
fun AddExpenseForm(username: String, onExpenseAdded: (TransactionModel) -> Unit) {
    
    // Premium Material Icons Mapping
    val categories = listOf(
        "Food" to Icons.Outlined.Restaurant,
        "Transport" to Icons.Outlined.DirectionsCar,
        "Shopping" to Icons.Outlined.ShoppingBag,
        "Bills" to Icons.Outlined.Receipt,
        "Custom" to Icons.Outlined.Edit
    )
    
    val paymentModes = listOf(
        "Cash" to Icons.Outlined.Payments,
        "UPI" to Icons.Outlined.QrCodeScanner,
        "NEFT" to Icons.Outlined.AccountBalance,
        "Credit Card" to Icons.Outlined.CreditCard,
        "Debit Card" to Icons.Outlined.CreditCard,
        "Net Banking" to Icons.Outlined.Computer
    )
    
    // SMART CURSOR LOGIC VARIABLES
    var categoryText by remember { mutableStateOf("") }
    var isCategoryEditable by remember { mutableStateOf(false) } // Lock/Unlock Cursor
    
    var remark1 by remember { mutableStateOf("") }
    var remark2 by remember { mutableStateOf("") }
    
    var modeText by remember { mutableStateOf("") }
    var modeExpanded by remember { mutableStateOf(false) }
    
    var amount by remember { mutableStateOf("") }
    
    val todayDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    var expenseDate by remember { mutableStateOf(todayDate) }
    
    var expanded by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var isPressed by remember { mutableStateOf(false) }
    val buttonScale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "ButtonScale")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            
            // 1. SMART EDITABLE CATEGORY DROPDOWN (Premium Material Icons)
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = categoryText,
                    onValueChange = { categoryText = it },
                    readOnly = !isCategoryEditable, // Logic: Custom chunoge tabhi type hoga
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { (name, icon) ->
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = icon, contentDescription = name, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(name, fontSize = 16.sp)
                                }
                            },
                            onClick = {
                                if (name == "Custom") {
                                    categoryText = "" 
                                    isCategoryEditable = true // Cursor Unlock
                                } else {
                                    categoryText = name
                                    isCategoryEditable = false // Cursor Lock
                                }
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. REMARK 1 & REMARK 2
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = remark1, onValueChange = { remark1 = it },
                    label = { Text("Remark 1") },
                    modifier = Modifier.weight(1f), singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = remark2, onValueChange = { remark2 = it },
                    label = { Text("Remark 2") },
                    modifier = Modifier.weight(1f), singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. PAYMENT MODE & STATUS
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                
                // Mode Dropdown
                ExposedDropdownMenuBox(
                    expanded = modeExpanded,
                    onExpandedChange = { modeExpanded = !modeExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = modeText,
                        onValueChange = {},
                        readOnly = true, // Strictly Read-Only
                        label = { Text("Mode") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = modeExpanded,
                        onDismissRequest = { modeExpanded = false }
                    ) {
                        paymentModes.forEach { (name, icon) ->
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = icon, contentDescription = name, tint = Color.DarkGray, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(name, fontSize = 14.sp)
                                    }
                                },
                                onClick = {
                                    modeText = name
                                    modeExpanded = false
                                }
                            )
                        }
                    }
                }

                // Visual Status Box (Non-editable)
                OutlinedTextField(
                    value = "Completed ✅", 
                    onValueChange = {},
                    readOnly = true,
                    enabled = false, 
                    label = { Text("Status") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = Color(0xFF2E7D32), // Green text to look successful
                        disabledBorderColor = Color.LightGray,
                        disabledLabelColor = Color.Gray
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. DATE & AMOUNT
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = expenseDate, 
                    onValueChange = { expenseDate = it },
                    label = { Text("Date (DD/MM/YYYY)") },
                    modifier = Modifier.weight(1f), 
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = amount, 
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = Color.Black) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), 
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))

            // 5. THEME GREEN ANIMATED SAVE BUTTON
            Button(
                onClick = {
                    val finalCategory = categoryText.trim()
                    val finalMode = modeText.trim()

                    if (amount.isNotBlank() && finalCategory.isNotBlank() && expenseDate.isNotBlank() && finalMode.isNotBlank()) {
                        isSubmitting = true
                        
                        val expenseAmt = amount.toDoubleOrNull() ?: 0.0
                        val newEntry = TransactionModel(expenseDate, expenseAmt, finalCategory, remark1, remark2)
                        onExpenseAdded(newEntry)

                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val json = JSONObject().apply {
                                    put("action", "add_expense")
                                    put("username", username)
                                    put("amount", amount)
                                    put("category", finalCategory)
                                    put("date", expenseDate) 
                                    put("detail1", remark1)
                                    put("detail2", remark2)
                                    put("payment_method", finalMode) 
                                }
                                val client = OkHttpClient()
                                val body = json.toString().toRequestBody("application/json".toMediaType())
                                val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(body).build()
                                client.newCall(request).execute()

                                withContext(Dispatchers.Main) {
                                    isSubmitting = false
                                    Toast.makeText(context, "Saved Successfully! ✅", Toast.LENGTH_LONG).show()
                                    // Resetting fields
                                    amount = ""; remark1 = ""; remark2 = ""; categoryText = ""; modeText = ""
                                    isCategoryEditable = false // Wapas lock
                                    expenseDate = todayDate
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isSubmitting = false
                                    Toast.makeText(context, "Failed to connect to sheet ❌", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(context, "Please fill Amount, Category, Mode & Date", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), // App Theme Green
                shape = RoundedCornerShape(12.dp),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(26.dp), strokeWidth = 3.dp)
                } else {
                    Text("Add Expense", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}
