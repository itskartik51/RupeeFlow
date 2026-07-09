package com.kartikey.rupeeflow.UI_Screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen() {
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food") } // Default category
    
    // Optional Details ke variables
    var foodWhere by remember { mutableStateOf("") }
    var foodWhat by remember { mutableStateOf("") }
    var transportFrom by remember { mutableStateOf("") }
    var transportTo by remember { mutableStateOf("") }
    var billFor by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Add Expense", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Amount Field (Zaroori / Mandatory)
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount (₹)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Category Dikhane ke liye (Dropdown ka design baad me add karenge)
        Text("Selected Category: $selectedCategory", color = MaterialTheme.colorScheme.primary)
        
        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic Fields: Category ke hisaab se dabbe badalna
        when (selectedCategory) {
            "Food" -> {
                OutlinedTextField(value = foodWhere, onValueChange = { foodWhere = it }, label = { Text("Where?") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = foodWhat, onValueChange = { foodWhat = it }, label = { Text("What?") }, modifier = Modifier.fillMaxWidth())
            }
            "Transport" -> {
                OutlinedTextField(value = transportFrom, onValueChange = { transportFrom = it }, label = { Text("From?") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = transportTo, onValueChange = { transportTo = it }, label = { Text("To?") }, modifier = Modifier.fillMaxWidth())
            }
            "Bills" -> {
                OutlinedTextField(value = billFor, onValueChange = { billFor = it }, label = { Text("Bill For?") }, modifier = Modifier.fillMaxWidth())
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Save Button
        Button(
            onClick = { /* Google Sheet me data bhejne ka logic yahan aayega */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Expense")
        }
    }
}
