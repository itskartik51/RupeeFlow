package com.kartikey.rupeeflow.UI_Screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food") }
    var expanded by remember { mutableStateOf(false) } // Dropdown khula hai ya band, uske liye
    val categories = listOf("Food", "Transport", "Bills", "Shopping", "Others") // Aapke options
    
    // Optional Details ke variables
    var foodWhere by remember { mutableStateOf("") }
    var foodWhat by remember { mutableStateOf("") }
    var transportFrom by remember { mutableStateOf("") }
    var transportTo by remember { mutableStateOf("") }
    var billFor by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Add New Expense", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))

        // Amount Field
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount (₹)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Asli Dropdown Menu (Category chunne ke liye)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedCategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            selectedCategory = category
                            expanded = false // Select karte hi menu band ho jayega
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic Fields: Jo category chunenge, wahi dabbe aayenge
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
                OutlinedTextField(value = billFor, onValueChange = { billFor = it }, label = { Text("Which Bill? (e.g. Electricity, Jio)") }, modifier = Modifier.fillMaxWidth())
            }
            // "Shopping" aur "Others" ke liye humne abhi khali rakha hai, aap chaho toh amount ke sath direct save kar sakte ho
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Save Button (Abhi iska design thoda aur premium kar diya hai)
        Button(
            onClick = { /* Database me bhejne ka logic aayega */ },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Save Expense", style = MaterialTheme.typography.titleMedium)
        }
    }
}
