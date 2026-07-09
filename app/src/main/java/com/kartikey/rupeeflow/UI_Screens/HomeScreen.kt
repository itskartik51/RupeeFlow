package com.kartikey.rupeeflow.UI_Screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
fun HomeScreen() {
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food") }
    var expanded by remember { mutableStateOf(false) }
    val categories = listOf("Food", "Transport", "Bills", "Shopping", "Others")
    
    var foodWhere by remember { mutableStateOf("") }
    var foodWhat by remember { mutableStateOf("") }
    var transportFrom by remember { mutableStateOf("") }
    var transportTo by remember { mutableStateOf("") }
    var billFor by remember { mutableStateOf("") }
    
    val coroutineScope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Add New Expense", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))

        // Number-Only Keyboard wala Amount Field
        OutlinedTextField(
            value = amount, 
            onValueChange = { 
                if (it.all { char -> char.isDigit() }) {
                    amount = it
                }
            }, 
            label = { Text("Amount (₹)") },
            modifier = Modifier.fillMaxWidth(), 
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = MaterialTheme.colorScheme.primary)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = selectedCategory, onValueChange = {}, readOnly = true, label = { Text("Select Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = { selectedCategory = category; expanded = false }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        when (selectedCategory) {
            "Food" -> {
                OutlinedTextField(value = foodWhere, onValueChange = { foodWhere = it }, label = { Text("Where did you eat?") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = foodWhat, onValueChange = { foodWhat = it }, label = { Text("What did you eat?") }, modifier = Modifier.fillMaxWidth())
            }
            "Transport" -> {
                OutlinedTextField(value = transportFrom, onValueChange = { transportFrom = it }, label = { Text("From Where?") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = transportTo, onValueChange = { transportTo = it }, label = { Text("To Where?") }, modifier = Modifier.fillMaxWidth())
            }
            "Bills" -> {
                OutlinedTextField(value = billFor, onValueChange = { billFor = it }, label = { Text("Which Bill?") }, modifier = Modifier.fillMaxWidth())
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = { 
                if(amount.isNotEmpty()) {
                    statusMessage = "Saving to Database..."
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val json = JSONObject()
                            json.put("amount", amount)
                            json.put("category", selectedCategory)
                            
                            when (selectedCategory) {
                                "Food" -> { json.put("detail1", foodWhere); json.put("detail2", foodWhat) }
                                "Transport" -> { json.put("detail1", transportFrom); json.put("detail2", transportTo) }
                                "Bills" -> { json.put("detail1", billFor); json.put("detail2", "") }
                                else -> { json.put("detail1", ""); json.put("detail2", "") }
                            }

                            val client = OkHttpClient()
                            val mediaType = "application/json; charset=utf-8".toMediaType()
                            val body = json.toString().toRequestBody(mediaType)
                            val request = Request.Builder()
                                .url(Constants.GOOGLE_SHEET_API_URL)
                                .post(body)
                                .build()

                            client.newCall(request).execute().use { response ->
                                withContext(Dispatchers.Main) {
                                    if (response.isSuccessful) {
                                        statusMessage = "🎉 Expense Saved Successfully!"
                                        amount = ""; foodWhere = ""; foodWhat = ""; transportFrom = ""; transportTo = ""; billFor = ""
                                    } else {
                                        statusMessage = "❌ Failed to save!"
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) { statusMessage = "Error: Network issue" }
                        }
                    }
                } else {
                    statusMessage = "Please enter amount!"
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Save Expense", style = MaterialTheme.typography.titleMedium)
        }
        
        if (statusMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(statusMessage, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
        }
    }
}
