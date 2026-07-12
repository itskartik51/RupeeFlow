package com.kartikey.rupeeflow.UI_Screens.Add

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
    val categories = listOf("🍔 Food", "🚕 Transport", "🛍️ Shopping", "🧾 Bills", "✍️ Custom")
    val paymentOptions = listOf("💵 Cash", "📱 UPI", "🏦 NEFT", "💳 Credit Card", "💳 Debit Card", "🔄 Other")
    
    var category by remember { mutableStateOf("") }
    var remark1 by remember { mutableStateOf("") }
    var remark2 by remember { mutableStateOf("") }
    
    // Payment Method variables
    var paymentMethod by remember { mutableStateOf("") }
    var paymentExpanded by remember { mutableStateOf(false) }
    
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
            
            // 1. SMART EDITABLE CATEGORY DROPDOWN
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { 
                        category = it
                        expanded = true 
                    },
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption, fontSize = 16.sp) },
                            onClick = {
                                if (selectionOption == "✍️ Custom") {
                                    category = "" 
                                } else {
                                    category = selectionOption
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

            // 3. PAYMENT METHOD & VISUAL ONLY BOX
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                
                // Payment Method Dropdown
                ExposedDropdownMenuBox(
                    expanded = paymentExpanded,
                    onExpandedChange = { paymentExpanded = !paymentExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = {},
                        readOnly = true, // User sirf select kar sakta hai
                        label = { Text("Payment Mode") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = paymentExpanded,
                        onDismissRequest = { paymentExpanded = false }
                    ) {
                        paymentOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, fontSize = 14.sp) },
                                onClick = {
                                    paymentMethod = option
                                    paymentExpanded = false
                                }
                            )
                        }
                    }
                }

                // Visual Only Box (Read-Only & Disabled look)
                OutlinedTextField(
                    value = "Completed ✅", // Sirf visually dikhane ke liye
                    onValueChange = {},
                    readOnly = true,
                    enabled = false, // Edit nahi ho sakta
                    label = { Text("Status") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = Color.DarkGray,
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

            // 5. ANIMATED SAVE BUTTON
            Button(
                onClick = {
                    val finalCategory = when(category.trim()) {
                        "🍔 Food" -> "Food"
                        "🚕 Transport" -> "Transport"
                        "🛍️ Shopping" -> "Shopping"
                        "🧾 Bills" -> "Bills"
                        "✍️ Custom", "" -> "" 
                        else -> category.trim() 
                    }
                    
                    val finalPayment = paymentMethod.replace(Regex("[^a-zA-Z ]"), "").trim() // Emoji hatane ke liye database hit se pehle

                    if (amount.isNotBlank() && finalCategory.isNotBlank() && expenseDate.isNotBlank() && paymentMethod.isNotBlank()) {
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
                                    put("payment_method", finalPayment) // Payment data sheet ke liye bhej diya
                                }
                                val client = OkHttpClient()
                                val body = json.toString().toRequestBody("application/json".toMediaType())
                                val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(body).build()
                                client.newCall(request).execute()

                                withContext(Dispatchers.Main) {
                                    isSubmitting = false
                                    Toast.makeText(context, "Saved Successfully! ✅", Toast.LENGTH_LONG).show()
                                    amount = ""; remark1 = ""; remark2 = ""; category = ""; paymentMethod = ""
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
                        Toast.makeText(context, "Please fill Amount, Category, Payment & Date", Toast.LENGTH_SHORT).show()
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B39AB)), 
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
