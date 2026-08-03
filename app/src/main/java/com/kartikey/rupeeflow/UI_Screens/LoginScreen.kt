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
import com.google.firebase.FieldValue
import com.google.firebase.firestore.FieldValue as FirestoreFieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kartikey.rupeeflow.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            .background(MaterialTheme.colorScheme.background)
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
            Text("RupeeFlow", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Personal Finance for Friends & Family", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("Join RupeeFlow", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Track expenses, budget, & net worth privately.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.height(40.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            Text(
                text = if (isLoginMode) "Sign In" else "New Profile",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!isLoginMode) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = "Name", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
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
                label = { Text("Username", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = "Username", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
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
                label = { Text(if (emailError) "Invalid Email Format!" else "Email ID", color = if (emailError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) },
                isError = emailError,
                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = "Email", tint = if (emailError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) },
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
                    focusedBorderColor = if (emailError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (emailError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
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
            label = { Text(if (isLoginMode) "Username or Mobile No." else "Mobile Number", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            prefix = {
                if (!isLoginMode) {
                    Text("+91 ", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                }
            },
            leadingIcon = { 
                Icon(
                    imageVector = if (isLoginMode) Icons.Outlined.Person else Icons.Outlined.Phone, 
                    contentDescription = "Contact", 
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                ) 
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
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
            label = { Text("Password", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = "Password", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
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
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        if (statusMessage.isNotEmpty() && !isLoading) {
            Text(statusMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
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
                            val db = FirebaseFirestore.getInstance()

                            if (isLoginMode) {
                                // ==========================================
                                // FIRESTORE LOGIN LOGIC
                                // ==========================================
                                val inputUserOrMobile = mobile.trim()
                                val inputPass = password.trim()

                                // 1. Try search by Username
                                var query = db.collection("Users")
                                    .whereEqualTo("username", inputUserOrMobile)
                                    .get()
                                    .await()

                                // 2. If empty, search by Mobile
                                if (query.isEmpty) {
                                    val formattedMobile = if (inputUserOrMobile.startsWith("+91")) inputUserOrMobile else "+91$inputUserOrMobile"
                                    query = db.collection("Users")
                                        .whereEqualTo("mobile_no_", formattedMobile)
                                        .get()
                                        .await()

                                    if (query.isEmpty) {
                                        query = db.collection("Users")
                                            .whereEqualTo("mobile", formattedMobile)
                                            .get()
                                            .await()
                                    }
                                }

                                if (!query.isEmpty) {
                                    val userDoc = query.documents[0]
                                    val savedPassword = userDoc.getString("password") ?: ""

                                    if (savedPassword == inputPass) {
                                        val matchedUsername = userDoc.getString("username") ?: inputUserOrMobile
                                        withContext(Dispatchers.Main) {
                                            onLoginSuccess(matchedUsername)
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            statusMessage = "Incorrect password!"
                                        }
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        statusMessage = "User not found!"
                                    }
                                }

                            } else {
                                // ==========================================
                                // FIRESTORE SIGNUP LOGIC
                                // ==========================================
                                val formattedMobile = "+91${mobile.trim()}"
                                val newUsername = username.trim()

                                // 1. Check if Username already taken
                                val userCheck = db.collection("Users")
                                    .whereEqualTo("username", newUsername)
                                    .get()
                                    .await()

                                if (!userCheck.isEmpty) {
                                    withContext(Dispatchers.Main) {
                                        statusMessage = "Username already taken!"
                                    }
                                    return@launch
                                }

                                // 2. Check if Mobile already registered
                                val mobileCheck = db.collection("Users")
                                    .whereEqualTo("mobile_no_", formattedMobile)
                                    .get()
                                    .await()

                                if (!mobileCheck.isEmpty) {
                                    withContext(Dispatchers.Main) {
                                        statusMessage = "Mobile number already registered!"
                                    }
                                    return@launch
                                }

                                // 3. Auto-increment User ID from System/Metadata
                                val metaRef = db.collection("System").document("Metadata")
                                val metaDoc = metaRef.get().await()
                                var lastCounter = 8L

                                if (metaDoc.exists()) {
                                    lastCounter = metaDoc.getLong("last_user_id") 
                                        ?: metaDoc.getLong("counter") 
                                        ?: 8L
                                }

                                val nextCounter = lastCounter + 1
                                val newUserId = String.format(Locale.US, "%04d", nextCounter) // e.g. "0009"

                                // Update counter in Metadata
                                metaRef.set(mapOf("last_user_id" to nextCounter), SetOptions.merge())

                                // 4. Create User Profile Doc
                                val userData = hashMapOf(
                                    "name" to name.trim(),
                                    "username" to newUsername,
                                    "mobile_no_" to formattedMobile,
                                    "email" to email.trim(),
                                    "password" to password.trim(),
                                    "dob" to "",
                                    "budget_limit" to 0.0,
                                    "created_at" to FirestoreFieldValue.serverTimestamp()
                                )

                                val newUserRef = db.collection("Users").document(newUserId)
                                newUserRef.set(userData).await()

                                // 5. Seed default Cash Document
                                val cashData = hashMapOf(
                                    "total_cash" to 0.0,
                                    "last_updated" to SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                                )
                                newUserRef.collection("Finances").document("Cash").set(cashData).await()

                                withContext(Dispatchers.Main) {
                                    onLoginSuccess(newUsername)
                                }
                            }

                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) { 
                                statusMessage = "Network/Firestore Error: ${e.localizedMessage ?: "Try again"}" 
                            } 
                        }
                    }
                }
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) { 
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
            } else {
                Text(
                    text = if (isLoginMode) "Access My Flow" else "Create & Seed Profile", 
                    fontSize = 16.sp, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
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
                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                fontSize = 14.sp
            )
            Text(
                text = if (isLoginMode) "Create Profile" else "Sign In", 
                color = MaterialTheme.colorScheme.primary, 
                fontWeight = FontWeight.Bold, 
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(40.dp)) 
    }
}
