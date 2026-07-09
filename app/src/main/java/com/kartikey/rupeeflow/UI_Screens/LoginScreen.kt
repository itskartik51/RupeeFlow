package com.kartikey.rupeeflow.UI_Screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen() {
    // Ye variables mobile aur password ko yaad rakhenge jab user type karega
    var mobile by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Rupee Flow", style = MaterialTheme.typography.headlineLarge)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Mobile Number wala dabba (Sirf 10 digit allow karega)
        OutlinedTextField(
            value = mobile,
            onValueChange = { if (it.length <= 10) mobile = it },
            label = { Text("Mobile Number") },
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Password wala dabba
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Login Button
        Button(
            onClick = { /* Database se check karne ka logic yahan baad me aayega */ },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Login")
        }
    }
}
