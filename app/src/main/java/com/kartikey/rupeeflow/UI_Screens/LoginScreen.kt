package com.kartikey.rupeeflow.UI_Screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
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
    var authStep by remember { mutableStateOf(1) } // 1 = Google Button, 2 = Complete Profile
    var statusMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Google Fetched Data
    var googleEmail by remember { mutableStateOf("") }
    var googleName by remember { mutableStateOf("") }
    var googlePhotoUrl by remember { mutableStateOf("") }

    // Form Inputs (New User)
    var inputName by remember { mutableStateOf("") }
    var inputUsername by remember { mutableStateOf("") }
    var inputMobile by remember { mutableStateOf("") }
    
    // Username Check States
    var isCheckingUsername by remember { mutableStateOf(false) }
    var usernameAvailable by remember { mutableStateOf<Boolean?>(null) } // null = unchecked, true = yes, false = taken

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(stringResource(id = R.string.default_web_client_id))
        .requestEmail()
        .build()
    
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    // Smart Interaction Lock: Back button on Step 2 cancels signup and signs out of Google
    BackHandler(enabled = authStep == 2) {
        auth.signOut()
        googleSignInClient.signOut()
        authStep = 1
        statusMessage = "" // Removed "Signup Cancelled" text
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    withContext(Dispatchers.Main) { 
                        isLoading = true
                        statusMessage = "" 
                    }
                    val account = task.getResult(ApiException::class.java)
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    
                    auth.signInWithCredential(credential).await()
                    val firebaseUser = auth.currentUser
                    
                    if (firebaseUser != null) {
                        val userEmail = firebaseUser.email ?: ""
                        val fetchedPhotoUrl = firebaseUser.photoUrl?.toString() ?: ""

                        val query = db.collection("Users").whereEqualTo("email", userEmail).get().await()
                        
                        if (!query.isEmpty) {
                            // EXISTING USER
                            val userDoc = query.documents[0]
                            val savedUsername = userDoc.getString("username") ?: userEmail.substringBefore("@")
                            
                            // 🚀 UPDATE: Silently update the latest profile photo link for existing users
                            userDoc.reference.update("prfl", fetchedPhotoUrl).await()

                            withContext(Dispatchers.Main) {
                                onLoginSuccess(savedUsername)
                            }
                        } else {
                            // NEW USER -> Send to Step 2
                            withContext(Dispatchers.Main) {
                                googleEmail = userEmail
                                googleName = firebaseUser.displayName ?: ""
                                googlePhotoUrl = fetchedPhotoUrl
                                inputName = googleName
                                
                                authStep = 2
                                isLoading = false
                                statusMessage = ""
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { 
                        statusMessage = "Authentication failed: ${e.localizedMessage}" 
                        isLoading = false
                    } 
                }
            }
        } else {
            statusMessage = "" // Removed "Google Sign-In Cancelled" text
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(18.dp))
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        if (authStep == 1) {
            Text("RupeeFlow", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Personal Finance for Friends & Family", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(60.dp))

            if (isLoading) {
                // Clean loading UI (only spinner)
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .bounceClick {
                            isLoading = true
                            statusMessage = "" // Cleared loading message
                            googleSignInClient.signOut().addOnCompleteListener {
                                launcher.launch(googleSignInClient.signInIntent)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google),
                            contentDescription = "Google Logo",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continue with Google",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                if (statusMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(statusMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        } else {
            // STEP 2: COMPLETE PROFILE FOR NEW USERS
            Text("Complete Your Profile", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Choose a unique identity for RupeeFlow.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(modifier = Modifier.height(32.dp))

            // 1. Name Field (Pre-filled, Editable, Updated Label)
            OutlinedTextField(
                value = inputName,
                onValueChange = { inputName = it },
                label = { Text("Name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            // 2. Username Field (Lowercase, No Spaces, Realtime Check, Updated Label)
            OutlinedTextField(
                value = inputUsername,
                onValueChange = { newValue ->
                    inputUsername = newValue.lowercase().replace(" ", "")
                    usernameAvailable = null // Reset check state when typing
                },
                label = { Text("Username", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { 
                    Icon(
                        imageVector = Icons.Outlined.Person, 
                        contentDescription = "Username", 
                        tint = if (usernameAvailable == true) MaterialTheme.colorScheme.primary else if (usernameAvailable == false) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                },
                trailingIcon = {
                    if (isCheckingUsername) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                    } else if (usernameAvailable == true) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = "Available", tint = MaterialTheme.colorScheme.primary)
                    } else if (usernameAvailable == false) {
                        Icon(Icons.Outlined.Cancel, contentDescription = "Taken", tint = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        // The Magic Check: When user leaves this field
                        if (!focusState.isFocused && inputUsername.isNotBlank()) {
                            isCheckingUsername = true
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val checkQuery = db.collection("Users").whereEqualTo("username", inputUsername).get().await()
                                    withContext(Dispatchers.Main) {
                                        usernameAvailable = checkQuery.isEmpty // true if empty (available)
                                        isCheckingUsername = false
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) { isCheckingUsername = false }
                                }
                            }
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (usernameAvailable == false) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (usernameAvailable == false) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )
            
            // Availability Helper Text
            if (usernameAvailable == false) {
                Text("Username already taken! Please try another.", color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.align(Alignment.Start).padding(start = 12.dp, top = 2.dp))
            } else if (usernameAvailable == true) {
                Text("Username Available!", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, modifier = Modifier.align(Alignment.Start).padding(start = 12.dp, top = 2.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Mobile Number Field (Strict Numeric, 10 Digits)
            OutlinedTextField(
                value = inputMobile,
                onValueChange = { newValue ->
                    // Strict numeric filter & max length 10
                    if (newValue.all { it.isDigit() } && newValue.length <= 10) {
                        inputMobile = newValue
                    }
                },
                label = { Text("Mobile Number", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                prefix = { Text("+91 ", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = "Mobile", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                })
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (statusMessage.isNotEmpty() && !isLoading) {
                Text(statusMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Create Profile Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .bounceClick {
                        keyboardController?.hide()
                        focusManager.clearFocus()

                        val finalName = inputName.trim()

                        if (finalName.isBlank() || inputUsername.isBlank() || inputMobile.isBlank()) {
                            statusMessage = "All fields are mandatory!"
                            return@bounceClick
                        }
                        if (inputMobile.length != 10) {
                            statusMessage = "Mobile number must be 10 digits!"
                            return@bounceClick
                        }
                        if (usernameAvailable == false) {
                            statusMessage = "Please select an available username."
                            return@bounceClick
                        }

                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                withContext(Dispatchers.Main) { 
                                    isLoading = true
                                    statusMessage = "" // Cleared loading message
                                }

                                // Fetch Counter and Gen ID
                                val metaRef = db.collection("System").document("Metadata")
                                val metaDoc = metaRef.get().await()
                                var lastCounter = 8L
                                if (metaDoc.exists()) {
                                    lastCounter = metaDoc.getLong("last_user_id") ?: metaDoc.getLong("counter") ?: 8L
                                }
                                val nextCounter = lastCounter + 1
                                val newUserId = String.format(Locale.US, "%04d", nextCounter)

                                // Update Counter
                                metaRef.set(mapOf("last_user_id" to nextCounter), SetOptions.merge()).await()

                                // Build Clean Data (No password field, prfl included)
                                val userData = hashMapOf(
                                    "name" to finalName,
                                    "username" to inputUsername,
                                    "mobile_no_" to inputMobile,
                                    "email" to googleEmail,
                                    "prfl" to googlePhotoUrl, 
                                    "dob" to null,
                                    "budget_limit" to 0.0,
                                    "created_at" to FieldValue.serverTimestamp()
                                )

                                val newUserRef = db.collection("Users").document(newUserId)
                                newUserRef.set(userData).await()

                                // Seed Cash Doc
                                val cashData = hashMapOf(
                                    "total_cash" to 0.0,
                                    "last_updated" to SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                                )
                                newUserRef.collection("Finances").document("Cash").set(cashData).await()

                                withContext(Dispatchers.Main) {
                                    onLoginSuccess(inputUsername)
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) { 
                                    statusMessage = "Error creating profile: ${e.localizedMessage}" 
                                    isLoading = false
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
                        text = "Create Profile", 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    ) 
                }
            }
        }
    }
}
