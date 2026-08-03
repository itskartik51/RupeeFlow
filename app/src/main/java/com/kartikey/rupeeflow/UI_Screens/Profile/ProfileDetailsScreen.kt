package com.kartikey.rupeeflow.UI_Screens.Profile

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailsScreen(
    username: String, 
    name: String,
    email: String,
    mobile: String,
    password: String,
    dob: String,
    onBackClick: () -> Unit,
    onProfileUpdated: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }

    var currentName by remember { mutableStateOf(name) }
    var currentUsername by remember { mutableStateOf(username) }
    var currentMobile by remember { mutableStateOf(mobile) }
    var currentPassword by remember { mutableStateOf(password) }
    var currentEmail by remember { mutableStateOf(email) }
    var currentDob by remember { mutableStateOf(dob) }

    var passwordVisible by remember { mutableStateOf(false) }
    var usernameError by remember { mutableStateOf(false) }
    
    val context = LocalContext.current

    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val dobMillis = remember(currentDob) {
        try {
            if (currentDob.isNotBlank()) sdf.parse(currentDob)?.time else null
        } catch (e: Exception) { null }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    // Applied Premium Bounce Here
                    IconButton(onClick = onBackClick, modifier = Modifier.bounceClick()) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    // Applied Premium Bounce Here
                    IconButton(
                        modifier = Modifier.bounceClick(),
                        onClick = { 
                            if (isEditing) {
                                isEditing = false
                                Toast.makeText(context, "Saving changes...", Toast.LENGTH_SHORT).show()
                                
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val db = FirebaseFirestore.getInstance()
                                        
                                        // 1. If username changed, verify it's not already taken
                                        if (currentUsername.trim() != username) {
                                            val duplicateCheck = db.collection("Users")
                                                .whereEqualTo("username", currentUsername.trim())
                                                .get()
                                                .await()
                                                
                                            if (!duplicateCheck.isEmpty) {
                                                withContext(Dispatchers.Main) {
                                                    isEditing = true
                                                    usernameError = true
                                                    Toast.makeText(context, "Username already taken!", Toast.LENGTH_SHORT).show()
                                                }
                                                return@launch
                                            }
                                        }

                                        // 2. Fetch User Document
                                        val userQuery = db.collection("Users")
                                            .whereEqualTo("username", username)
                                            .get()
                                            .await()
                                            
                                        if (!userQuery.isEmpty) {
                                            val userRef = userQuery.documents[0].reference
                                            
                                            val formattedMobile = if (currentMobile.startsWith("+91")) currentMobile else "+91$currentMobile"
                                            
                                            // 3. Update Firestore Data
                                            val updates = hashMapOf<String, Any>(
                                                "name" to currentName.trim(),
                                                "username" to currentUsername.trim(),
                                                "mobile_no_" to formattedMobile,
                                                "email" to currentEmail.trim(),
                                                "password" to currentPassword.trim(),
                                                "dob" to currentDob.trim()
                                            )
                                            
                                            userRef.update(updates).await()
                                            
                                            withContext(Dispatchers.Main) {
                                                usernameError = false
                                                onProfileUpdated() 
                                                Toast.makeText(context, "Profile Details Updated!", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                isEditing = true
                                                Toast.makeText(context, "User not found!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isEditing = true
                                            Toast.makeText(context, "Update Failed!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                isEditing = true 
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Check else Icons.Outlined.Edit, 
                            contentDescription = if (isEditing) "Save" else "Edit", 
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            @Composable
            fun DetailField(
                label: String,
                value: String,
                onValueChange: (String) -> Unit,
                icon: ImageVector,
                keyboardType: KeyboardType = KeyboardType.Text,
                isPassword: Boolean = false,
                isErrorState: Boolean = false 
            ) {
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    readOnly = !isEditing,
                    label = { Text(label, color = if (isErrorState) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = { Icon(icon, contentDescription = label, tint = if (isErrorState) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (isPassword) {
                            val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = "Toggle Password", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = if (isErrorState) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = if (isErrorState) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        disabledIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        disabledTextColor = MaterialTheme.colorScheme.onBackground
                    ),
                    singleLine = true,
                    isError = isErrorState
                )
            }

            DetailField("Name", currentName, { currentName = it }, Icons.Outlined.Person)
            
            Column {
                DetailField(
                    label = "Username", 
                    value = currentUsername, 
                    onValueChange = { 
                        currentUsername = it
                        usernameError = false
                    }, 
                    icon = Icons.Outlined.Badge,
                    isErrorState = usernameError
                )
                if (usernameError) {
                    Text(
                        text = "This username is already used by someone.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 48.dp, top = 4.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            DetailField("Mobile No.", currentMobile, { currentMobile = it }, Icons.Outlined.Phone, KeyboardType.Phone)
            DetailField("Password", currentPassword, { currentPassword = it }, Icons.Outlined.Lock, isPassword = true)
            DetailField("Email ID", currentEmail, { currentEmail = it }, Icons.Outlined.Email, KeyboardType.Email)
            
            if (isEditing) {
                com.kartikey.rupeeflow.UI_Screens.CustomDatePicker(
                    label = "Date of Birth (DD/MM/YYYY)",
                    selectedDateMillis = dobMillis,
                    onDateSelected = { millis ->
                        currentDob = sdf.format(Date(millis))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    restrictToCurrentMonth = false
                )
            } else {
                DetailField("Date of Birth (DD/MM/YYYY)", currentDob, { }, Icons.Outlined.Cake)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
