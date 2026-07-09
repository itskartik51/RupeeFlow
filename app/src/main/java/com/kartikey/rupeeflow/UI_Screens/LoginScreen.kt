package com.kartikey.rupeeflow.UI_Screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kartikey.rupeeflow.Cloud_Database.Constants
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var isLoginMode by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center) {
        Text(if (isLoginMode) "Login" else "Sign Up", style = MaterialTheme.typography.headlineLarge)
        
        if (!isLoginMode) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
        }
        OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile Number") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
        
        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = {
            coroutineScope.launch {
                val json = JSONObject().apply {
                    put("action", if (isLoginMode) "login" else "signup")
                    put("name", name)
                    put("mobile", mobile)
                    put("username", username)
                    put("password", password)
                }
                // (Yahan OkHttp request code wahi purana wala rahega, bas JSON badal gaya)
                // ... (OkHttp logic same rahenga)
                statusMessage = "Processing..."
            }
        }, modifier = Modifier.fillMaxWidth()) { Text(if (isLoginMode) "Login" else "Sign Up") }
        
        TextButton(onClick = { isLoginMode = !isLoginMode }) { Text(if (isLoginMode) "New? Create Account" else "Already have account? Login") }
        Text(statusMessage)
    }
}
