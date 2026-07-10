package com.kartikey.rupeeflow.UI_Screens.AddExpense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

data class TransactionModel(
    val date: String,
    val amount: Double,
    val category: String,
    val detail1: String,
    val detail2: String
)

@Composable
fun ExpenseHistoryScreen(
    paddingValues: PaddingValues,
    history: List<TransactionModel>,
    onBackClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Text("Expense History", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color(0xFF2E7D32))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text("M / Y", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2E7D32))
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Filter", tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(bottom = 80.dp)) {
            if (history.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No transactions yet...", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                items(history) { txn ->
                    TransactionHistoryRow(txn)
                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseAddScreen(username: String, paddingValues: PaddingValues) {
    var amount by remember { mutableStateOf("") }
    var categoryText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var isCustomCategory by remember { mutableStateOf(false) } // Custom lock/unlock ka state
    
    val predefinedCategories = listOf("Food", "Transport", "Bills", "Shopping", "Custom")
    
    var detail1 by remember { mutableStateOf("") }
    var detail2 by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // Ekdum Professional aur Clean Labels
    val labels = when {
        isCustomCategory -> Pair("Description", "Remarks")
        categoryText == "Transport" -> Pair("From", "To")
        categoryText == "Food" -> Pair("Place", "Food Item")
        categoryText == "Bills" -> Pair("Bill Type", "Operator")
        categoryText == "Shopping" -> Pair("Item", "Shop")
        else -> Pair("Description", "Remarks")
    }

    val plusIconShape = RoundedCornerShape(14.dp)

    Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
        Text("Add Expenses", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color(0xFF2E7D32))
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(), 
            colors = CardDefaults.cardColors(containerColor = Color.White), 
            elevation = CardDefaults.cardElevation(4.dp), 
            shape = plusIconShape
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = categoryText, 
                        onValueChange = { categoryText = it },
                        label = { Text("Category") }, 
                        placeholder = { Text("Select category") },
                        readOnly = !isCustomCategory, // SIRF CUSTOM WALE MEIN TYPING ALLOWED HAI
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        shape = plusIconShape,
                        modifier = Modifier.menuAnchor().fillMaxWidth(), 
                        singleLine = true
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        predefinedCategories.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = { 
                                    if (selectionOption == "Custom") {
                                        isCustomCategory = true
                                        categoryText = ""
                                    } else {
                                        isCustomCategory = false
                                        categoryText = selectionOption
                                    }
                                    expanded = false 
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = detail1, 
                        onValueChange = { detail1 = it }, 
                        label = { Text(labels.first) }, 
                        shape = plusIconShape, 
                        modifier = Modifier.weight(1f), 
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = detail2, 
                        onValueChange = { detail2 = it }, 
                        label = { Text(labels.second) }, 
                        shape = plusIconShape, 
                        modifier = Modifier.weight(1f), 
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    OutlinedTextField(
                        value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, 
                        prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = plusIconShape,
                        modifier = Modifier.weight(1.5f), singleLine = true
                    )

                    Button(
                        onClick = {
                            if (amount.isNotEmpty() && categoryText.isNotEmpty()) {
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        statusMessage = "Adding..."
                                        val json = JSONObject().apply {
                                            put("action", "add_expense")
                                            put("username", username)
                                            put("amount", amount)
                                            put("category", categoryText)
                                            put("detail1", detail1)
                                            put("detail2", detail2)
                                        }
                                        val client = OkHttpClient()
                                        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                                        val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(body).build()
                                        val response = client.newCall(request).execute()
                                        
                                        withContext(Dispatchers.Main) {
                                            if (response.isSuccessful) {
                                                statusMessage = "Added! Check History."
                                                amount = ""; detail1 = ""; detail2 = ""; categoryText = ""; isCustomCategory = false
                                            } else statusMessage = "Failed to add!"
                                        }
                                    } catch (e: Exception) { withContext(Dispatchers.Main) { statusMessage = "Error connecting to server!" } }
                                }
                            } else { statusMessage = "Amount & Category required!" }
                        }, 
                        modifier = Modifier.weight(1f).height(56.dp), 
                        shape = plusIconShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)) 
                    ) { Text("Add", color = Color.White, fontWeight = FontWeight.Bold) }
                }
                
                if (statusMessage.isNotEmpty()) {
                    Text(statusMessage, color = if(statusMessage.contains("Check")) Color(0xFF2E7D32) else Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}
