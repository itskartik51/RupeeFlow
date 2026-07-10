package com.kartikey.rupeeflow.UI_Screens.Home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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

// Data Model Transaction History ke liye
data class TransactionModel(
    val date: String,
    val amount: Double,
    val category: String,
    val detail1: String,
    val detail2: String
)

// 1. RED CARD (Home Screen ke liye)
@Composable
fun ExpenseSummaryCard(
    thisMonthTotal: Double = 0.0,
    thisYearTotal: Double = 0.0,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("This Month") }

    val displayTotal = if (selectedFilter == "This Month") thisMonthTotal else thisYearTotal
    val formattedTotal = if (isLoading) "Loading..." else "₹${displayTotal.toInt()}"

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), 
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "EXPENSES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                Box {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { expanded = true }) {
                        Text(text = selectedFilter, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Select", tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color.White)) {
                        DropdownMenuItem(text = { Text("This Month", fontWeight = FontWeight.Medium) }, onClick = { selectedFilter = "This Month"; expanded = false })
                        DropdownMenuItem(text = { Text("This Year", fontWeight = FontWeight.Medium) }, onClick = { selectedFilter = "This Year"; expanded = false })
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp)) 
            Text(text = formattedTotal, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
        }
    }
}

// 2. EXPENSE ADD SCREEN (Aapke Design & GPay Style History ke sath)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseAddScreen(
    username: String, 
    paddingValues: PaddingValues,
    history: List<TransactionModel> // Naya parameter jo sheet data layega
) {
    var amount by remember { mutableStateOf("") }
    var categoryText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val predefinedCategories = listOf("Food", "Transport", "Bills", "Shopping", "Custom")
    
    var detail1 by remember { mutableStateOf("") }
    var detail2 by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // Jadu wala logic: Category badalte hi Labels badal jayenge
    val labels = when (categoryText) {
        "Transport" -> Pair("From (Kahan se?)", "To (Kahan tak?)")
        "Food" -> Pair("Place (Kahan khaya?)", "Food Item (Kya khaya?)")
        "Bills" -> Pair("Whose Bill?", "Company (e.g. Jio)")
        "Shopping" -> Pair("Item/Brand", "Shop/App Name")
        "" -> Pair("Detail 1", "Detail 2")
        else -> Pair("About 1", "About 2") // Custom ke liye About set kiya hai
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)
    ) {
        Text("Add Expenses", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color(0xFF2E7D32))
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(4.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = categoryText,
                        onValueChange = { categoryText = it },
                        label = { Text("Category") },
                        placeholder = { Text("Select or type custom") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        predefinedCategories.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    categoryText = if(selectionOption == "Custom") "" else selectionOption
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
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = detail2, 
                        onValueChange = { detail2 = it }, 
                        label = { Text(labels.second) }, 
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom 
                ) {
                    OutlinedTextField(
                        value = amount, 
                        onValueChange = { amount = it }, 
                        label = { Text("Amount") }, 
                        prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.5f),
                        singleLine = true
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
                                                statusMessage = "Added Successfully! (Restart to see below)"
                                                amount = ""; detail1 = ""; detail2 = ""; categoryText = ""
                                            } else statusMessage = "Failed to add!"
                                        }
                                    } catch (e: Exception) { withContext(Dispatchers.Main) { statusMessage = "Error connecting to server!" } }
                                }
                            } else {
                                statusMessage = "Amount & Category required!"
                            }
                        }, 
                        modifier = Modifier.weight(1f).height(56.dp), 
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)) 
                    ) { 
                        Text("Add", color = Color.White, fontWeight = FontWeight.Bold) 
                    }
                }
                
                if (statusMessage.isNotEmpty()) {
                    Text(statusMessage, color = if(statusMessage.contains("Success")) Color(0xFF2E7D32) else Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Transaction History", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.DarkGray)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // GPAY STYLE TRANSACTION LIST
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
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

@Composable
fun TransactionHistoryRow(txn: TransactionModel) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Circle Avatar (GPay Style)
            Box(
                modifier = Modifier.size(48.dp).background(Color(0xFFE8F5E9), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(txn.category.take(1).uppercase(), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF2E7D32))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(txn.category, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
                val details = listOf(txn.detail1, txn.detail2).filter { it.isNotBlank() }.joinToString(" • ")
                if (details.isNotEmpty()) {
                    Text(details, color = Color.Gray, fontSize = 13.sp, maxLines = 1)
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("-₹${txn.amount.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.Black)
            Text(txn.date.take(10), color = Color.Gray, fontSize = 12.sp)
        }
    }
}
