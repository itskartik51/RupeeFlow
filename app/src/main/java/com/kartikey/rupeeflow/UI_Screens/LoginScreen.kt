package com.kartikey.rupeeflow.UI_Screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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

@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit) {
    var isLoginMode by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (isLoginMode) "Login" else "Create Account", style = MaterialTheme.typography.headlineLarge)
        
        if (!isLoginMode) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Create Username") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text(if(isLoginMode) "Mobile or Username" else "Mobile Number") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
        
        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    statusMessage = "Processing..."
                    val json = JSONObject().apply {
                        put("action", if (isLoginMode) "login" else "signup")
                        put("name", name)
                        put("mobile", mobile)
                        put("username", username)
                        put("password", password)
                    }

                    val client = OkHttpClient()
                    val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                    val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(body).build()
                    val response = client.newCall(request).execute()
                    val responseData = response.body?.string() ?: ""
                    
                    withContext(Dispatchers.Main) {
                        try {
                            val jsonResponse = JSONObject(responseData)
                            val status = jsonResponse.optString("status")
                            val message = jsonResponse.optString("message")

                            if (status == "success") {
                                // Server se aaya hua asli username pakdenge
                                val loggedInUser = jsonResponse.optString("username")
                                onLoginSuccess(loggedInUser)
                            } else {
                                // Jo asli error hai wo dikhegi (e.g. Invalid password)
                                statusMessage = message
                            }
                        } catch (e: Exception) {
                            statusMessage = "Error parsing data!"
                        }
                    }
                } catch (e: Exception) { withContext(Dispatchers.Main) { statusMessage = "Network Error!" } }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text(if (isLoginMode) "Login" else "Sign Up") }
        
        TextButton(onClick = { 
            isLoginMode = !isLoginMode
            statusMessage = "" 
        }) { Text(if (isLoginMode) "New? Sign Up" else "Already have account? Login") }
        
        Text(statusMessage, color = MaterialTheme.colorScheme.error)
    }
}
