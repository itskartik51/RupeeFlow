@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
package com.kartikey.rupeeflow.UI_Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.Cloud_Database.Constants
import com.kartikey.rupeeflow.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit) {
    var isLoginMode by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") } 
    var password by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    
    var emailError by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isLoading = statusMessage == "Processing..."

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8F8))
            .imePadding() 
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Image(
            painter = painterResource(id = R.mipmap.ic_launcher),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoginMode) {
            Text("RupeeFlow", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Personal Finance for Friends & Family", fontSize = 12.sp, color = Color.Gray)
        } else {
            Text("Join RupeeFlow", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Track expenses, budget, & net worth privately.", fontSize = 12.sp, color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(40.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            Text(
                text = if (isLoginMode) "Sign In" else "New Profile",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!isLoginMode) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = "Name", tint = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2E7D32),
                    unfocusedBorderColor = Color(0xFFEEEEEE),
                    // TRANSPARENT BACKGROUND RESTORED
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = "Username", tint = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2E7D32),
                    unfocusedBorderColor = Color(0xFFEEEEEE),
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { 
                    email = it 
                    if (emailError) emailError = false
                },
                label = { Text(if (emailError) "Invalid Email Format!" else "Email ID", color = if (emailError) Color.Red else Color.Gray) },
                isError = emailError,
                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = "Email", tint = if (emailError) Color.Red else Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused && email.isNotBlank()) {
                            val emailRegex = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}".toRegex()
                            emailError = !email.matches(emailRegex)
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (emailError) Color.Red else Color(0xFF2E7D32),
                    unfocusedBorderColor = if (emailError) Color.Red else Color(0xFFEEEEEE),
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        OutlinedTextField(
            value = mobile,
            onValueChange = { input ->
                if (isLoginMode) {
                    mobile = input
                } else {
                    if (input.all { char -> char.isDigit() } && input.length <= 10) {
                        mobile = input
                    }
                }
            },
            label = { Text(if (isLoginMode) "Username or Mobile No." else "Mobile Number", color = Color.Gray) },
            prefix = {
                if (!isLoginMode) {
                    Text("+91 ", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            leadingIcon = { 
                Icon(
                    imageVector = if (isLoginMode) Icons.Outlined.Person else Icons.Outlined.Phone, 
                    contentDescription = "Contact", 
                    tint = Color.Gray
                ) 
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2E7D32),
                unfocusedBorderColor = Color(0xFFEEEEEE),
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isLoginMode) KeyboardType.Text else KeyboardType.Phone,
                imeAction = ImeAction.Next 
            ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = "Password", tint = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2E7D32),
                unfocusedBorderColor = Color(0xFFEEEEEE),
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done 
            ),
            keyboardActions = KeyboardActions(onDone = {
                keyboardController?.hide()
                focusManager.clearFocus()
            })
        )

        if (isLoginMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp), 
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Forget Password?",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.clickable { 
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        if (statusMessage.isNotEmpty() && !isLoading) {
            Text(statusMessage, color = Color.Red, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .appleExpand("login_button_flow")
                .bounceClick {
                    keyboardController?.hide()
                    focusManager.clearFocus()

                    if (!isLoginMode) {
                        if (name.isBlank() || mobile.isBlank() || username.isBlank() || password.isBlank() || email.isBlank()) {
                            statusMessage = "All fields including Email are mandatory!"
                            return@bounceClick
                        }
                        if (mobile.length != 10) {
                            statusMessage = "Mobile number must be exactly 10 digits!"
                            return@bounceClick
                        }
                        if (emailError) {
                            statusMessage = "Please fix the email format!"
                            return@bounceClick
                        }
                    } else {
                        if (mobile.isBlank() || password.isBlank()) {
                            statusMessage = "Please enter your credentials."
                            return@bounceClick
                        }
                    }

                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            statusMessage = "Processing..."
                            val json = JSONObject().apply {
                                put("action", if (isLoginMode) "login" else "signup")
                                put("name", name)
                                put("mobile", if (!isLoginMode) "+91$mobile" else mobile)
                                put("username", username)
                                put("password", password)
                                if (!isLoginMode) {
                                    put("email", email) 
                                }
                            }

                            val client = NetworkClient.instance
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
                                        val loggedInUser = jsonResponse.optString("username")
                                        onLoginSuccess(loggedInUser)
                                    } else {
                                        statusMessage = message
                                    }
                                } catch (e: Exception) {
                                    statusMessage = "Error parsing data!"
                                }
                            }
                        } catch (e: Exception) { withContext(Dispatchers.Main) { statusMessage = "Network Error!" } }
                    }
                }
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2E7D32)),
            contentAlignment = Alignment.Center
        ) { 
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
            } else {
                Text(
                    text = if (isLoginMode) "Access My Flow" else "Create & Seed Profile", 
                    fontSize = 16.sp, 
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ) 
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { 
                isLoginMode = !isLoginMode
                statusMessage = "" 
            }
        ) {
            Text(
                text = if (isLoginMode) "Don't have a profile? " else "Already have a profile? ", 
                color = Color.Gray, 
                fontSize = 14.sp
            )
            Text(
                text = if (isLoginMode) "Create Profile" else "Sign In", 
                color = Color(0xFF2E7D32), 
                fontWeight = FontWeight.Bold, 
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(40.dp)) 
    }
}
