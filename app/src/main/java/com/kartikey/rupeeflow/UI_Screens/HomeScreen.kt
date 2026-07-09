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
fun HomeScreen(username: String, onLogout: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food") }
    var expanded by remember { mutableStateOf(false) }
    val categories = listOf("Food", "Transport", "Bills", "Shopping", "Others")
    var detail1 by remember { mutableStateOf("") }
    var detail2 by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        
        // Welcome Text aur Logout Button
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Welcome, $username", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            TextButton(onClick = onLogout) { Text("Logout", color = MaterialTheme.colorScheme.error) }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (₹)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(value = selectedCategory, onValueChange = {}, readOnly = true, label = { Text("Category") }, modifier = Modifier.menuAnchor().fillMaxWidth())
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                categories.forEach { cat -> 
                    DropdownMenuItem(text = { Text(cat) }, onClick = { 
                        selectedCategory = cat
                        expanded = false
                        detail1 = ""
                        detail2 = ""
                    }) 
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedCategory) {
            "Food" -> {
                OutlinedTextField(value = detail1, onValueChange = { detail1 = it }, label = { Text("Where did you eat?") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = detail2, onValueChange = { detail2 = it }, label = { Text("What did you eat?") }, modifier = Modifier.fillMaxWidth())
            }
            "Transport" -> {
                OutlinedTextField(value = detail1, onValueChange = { detail1 = it }, label = { Text("From Where?") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = detail2, onValueChange = { detail2 = it }, label = { Text("To Where?") }, modifier = Modifier.fillMaxWidth())
            }
            "Bills" -> {
                OutlinedTextField(value = detail1, onValueChange = { detail1 = it }, label = { Text("Which Bill?") }, modifier = Modifier.fillMaxWidth())
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    statusMessage = "Saving..."
                    val json = JSONObject().apply {
                        put("action", "add_expense")
                        put("username", username)
                        put("amount", amount)
                        put("category", selectedCategory)
                        put("detail1", detail1)
                        put("detail2", detail2)
                    }
                    val client = OkHttpClient()
                    val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                    val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(body).build()
                    val response = client.newCall(request).execute()
                    
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            statusMessage = "Saved Successfully!"
                            amount = ""; detail1 = ""; detail2 = ""
                        } else {
                            statusMessage = "Failed!"
                        }
                    }
                } catch (e: Exception) { withContext(Dispatchers.Main) { statusMessage = "Error!" } }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Save Expense") }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(statusMessage, color = MaterialTheme.colorScheme.primary)
    }
}
