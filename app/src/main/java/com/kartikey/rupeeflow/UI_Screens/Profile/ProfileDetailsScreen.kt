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
import com.kartikey.rupeeflow.UI_Screens.updateUserProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailsScreen(username: String, onBackClick: () -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    // States for Form
    var currentName by remember { mutableStateOf("") }
    var currentUsername by remember { mutableStateOf(username) }
    var currentMobile by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var currentEmail by remember { mutableStateOf("") }
    var currentDob by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF2E7D32))
                    }
                },
                actions = {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 16.dp).size(24.dp), 
                            color = Color(0xFF2E7D32), 
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { 
                            if (isEditing) {
                                isSubmitting = true
                                coroutineScope.launch {
                                    // Ye function EditSheet.kt ke andar hai
                                    updateUserProfile(
                                        oldUsername = username,
                                        newName = currentName,
                                        newUsername = currentUsername,
                                        newMobile = currentMobile,
                                        newEmail = currentEmail,
                                        newPassword = currentPassword,
                                        newDob = currentDob,
                                        onSuccess = {
                                            isSubmitting = false
                                            isEditing = false
                                            Toast.makeText(context, "Profile Details Updated!", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = {
                                            isSubmitting = false
                                            Toast.makeText(context, "Update Failed!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            } else {
                                isEditing = true // Pen Icon par click karte hi editable banega
                            }
                        }) {
                            Icon(
                                imageVector = if (isEditing) Icons.Default.Check else Icons.Outlined.Edit, 
                                contentDescription = if (isEditing) "Save" else "Edit", 
                                tint = Color(0xFF2E7D32)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8F9FA))
            )
        },
        containerColor = Color(0xFFF8F9FA)
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

            // Premium Line-based TextField Component
            @Composable
            fun DetailField(
                label: String,
                value: String,
                onValueChange: (String) -> Unit,
                icon: ImageVector,
                keyboardType: KeyboardType = KeyboardType.Text,
                isPassword: Boolean = false
            ) {
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    readOnly = !isEditing, // Lock/Unlock magic yahin se hoga
                    label = { Text(label, color = Color.Gray) },
                    leadingIcon = { Icon(icon, contentDescription = label, tint = Color(0xFF2E7D32)) },
                    trailingIcon = {
                        if (isPassword) {
                            val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = "Toggle Password", tint = Color.Gray)
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
                        focusedIndicatorColor = Color(0xFF2E7D32),
                        unfocusedIndicatorColor = Color.LightGray,
                        disabledIndicatorColor = Color.LightGray,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        disabledTextColor = Color.Black
                    ),
                    singleLine = true
                )
            }

            DetailField("Name", currentName, { currentName = it }, Icons.Outlined.Person)
            DetailField("Username", currentUsername, { currentUsername = it }, Icons.Outlined.Badge)
            DetailField("Mobile No.", currentMobile, { currentMobile = it }, Icons.Outlined.Phone, KeyboardType.Phone)
            DetailField("Password", currentPassword, { currentPassword = it }, Icons.Outlined.Lock, isPassword = true)
            DetailField("Email ID", currentEmail, { currentEmail = it }, Icons.Outlined.Email, KeyboardType.Email)
            DetailField("Date of Birth (DD/MM/YYYY)", currentDob, { currentDob = it }, Icons.Outlined.Cake)
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
