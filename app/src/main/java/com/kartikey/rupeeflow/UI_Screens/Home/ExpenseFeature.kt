package com.kartikey.rupeeflow.UI_Screens.Home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

// 1. YE WAHI PURANA RED CARD HAI (Jo Home Screen par dikhta hai)
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

// 2. YE NAYA EXPENSE PAGE HAI (Aapke hath ke banaye Sketch jaisa)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseAddScreen(username: String, paddingValues: PaddingValues) {
    var amount by remember { mutableStateOf("") }
    // Category ab ek freely editable text ban gaya hai
    var categoryText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val predefinedCategories = listOf("Food", "Transport", "Bills", "Shopping", "Others")
    
    var detail1 by remember { mutableStateOf("") }
    var detail2 by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
    ) {
        Text("Add Expenses", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color(0xFF2E7D32))
        Spacer(modifier = Modifier.height(16.dp))

        // MAIN CARD FROM SKETCH
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(4.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                
                // ROW 1: Editable Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = categoryText,
                        onValueChange = { categoryText = it }, // User ab kuch bhi type kar sakta hai
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
                                    categoryText = selectionOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ROW 2: Detail 1 | Detail 2
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = detail1, 
                        onValueChange = { detail1 = it }, 
                        label = { Text("Detail_1") }, 
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = detail2, 
                        onValueChange = { detail2 = it }, 
                        label = { Text("Detail_2") }, 
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ROW 3: Amount | Green Add Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom // Align bottom taaki button aur text field ek line me dikhe
                ) {
                    OutlinedTextField(
                        value = amount, 
                        onValueChange = { amount = it }, 
                        label = { Text("Amount") }, 
                        prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) }, // AUTOMATIC ₹ SYMBOL
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
                                                statusMessage = "Added Successfully!"
                                                amount = ""; detail1 = ""; detail2 = ""; categoryText = ""
                                            } else statusMessage = "Failed to add!"
                                        }
                                    } catch (e: Exception) { withContext(Dispatchers.Main) { statusMessage = "Error connecting to server!" } }
                                }
                            } else {
                                statusMessage = "Amount & Category required!"
                            }
                        }, 
                        modifier = Modifier.weight(1f).height(56.dp), // Height matches OutlinedTextField
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)) // GREEN COLOR
                    ) { 
                        Text("Add", color = Color.White, fontWeight = FontWeight.Bold) 
                    }
                }
                
                if (statusMessage.isNotEmpty()) {
                    Text(statusMessage, color = if(statusMessage.contains("Success")) Color(0xFF2E7D32) else Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // TRANSACTION HISTORY (M/Y) SKETCH UI
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Transaction History", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.DarkGray)
            
            // Month / Year Dropdown Placeholder
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text("M / Y", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2E7D32))
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Filter", tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Blank box for future history list
        Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Text("History will appear here...", color = Color.Gray, fontSize = 12.sp)
        }
    }
}
