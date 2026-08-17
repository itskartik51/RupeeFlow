package com.kartikey.rupeeflow.UI_Screens.Profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
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
    profilePicUrl: String, 
    dob: String,
    onBackClick: () -> Unit,
    onProfileUpdated: () -> Unit
) {
    var currentName by remember { mutableStateOf(name) }
    var currentUsername by remember { mutableStateOf(username) }
    var currentMobile by remember { mutableStateOf(mobile) }
    var currentEmail by remember { mutableStateOf(email) }
    var currentDob by remember { mutableStateOf(dob) }

    var editingField by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var usernameError by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val dobMillis = remember(currentDob) {
        try {
            if (currentDob.isNotBlank()) sdf.parse(currentDob)?.time else null
        } catch (e: Exception) { null }
    }

    // Image Picker Launcher for Offline Custom Photo
    var imageUpdateTrigger by remember { mutableStateOf(System.currentTimeMillis()) }
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val success = com.kartikey.rupeeflow.UI_Screens.CacheManager.saveCustomProfilePic(context, uri)
                withContext(Dispatchers.Main) {
                    if (success) {
                        imageUpdateTrigger = System.currentTimeMillis()
                        Toast.makeText(context, "Profile Photo Updated Offline", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to save photo", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // 🚀 NEW: Generic Field Update Engine
    fun saveFieldToFirestore(field: String, value: Any, onSuccess: () -> Unit) {
        isSaving = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val db = FirebaseFirestore.getInstance()
                val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                if (!userQuery.isEmpty) {
                    userQuery.documents[0].reference.update(field, value).await()
                    withContext(Dispatchers.Main) {
                        isSaving = false
                        onProfileUpdated()
                        Toast.makeText(context, "Updated successfully!", Toast.LENGTH_SHORT).show()
                        onSuccess()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isSaving = false
                        Toast.makeText(context, "User not found", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isSaving = false
                    Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.bounceClick()) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                // Removed Global Edit Button
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
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
            Spacer(modifier = Modifier.height(12.dp))

            // Profile Pic Component: Always editable with Camera Icon
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ProfileAvatar(
                    name = currentName,
                    profilePicUrl = profilePicUrl,
                    size = 110.dp,
                    fontSize = 42.sp,
                    isEditable = true,
                    onEditClick = { imagePickerLauncher.launch("image/*") },
                    forceUpdateTrigger = imageUpdateTrigger
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            // 🚀 NEW: Smart Individual Detail Field Component
            @Composable
            fun DetailField(
                label: String,
                value: String,
                onValueChange: (String) -> Unit,
                icon: ImageVector,
                isEditableField: Boolean = true,
                isEditing: Boolean = false,
                isSavingStatus: Boolean = false,
                onEditClick: () -> Unit = {},
                onSaveClick: () -> Unit = {},
                keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                isErrorState: Boolean = false,
                errorMessage: String = ""
            ) {
                val focusRequester = remember { FocusRequester() }
                val keyboardController = LocalSoftwareKeyboardController.current
                val focusManager = LocalFocusManager.current
                
                LaunchedEffect(isEditing) {
                    if (isEditing) {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = value,
                        onValueChange = onValueChange,
                        readOnly = !isEditing,
                        label = { Text(label, color = if (isErrorState) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingIcon = { Icon(icon, contentDescription = label, tint = if (isErrorState) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            if (isEditableField) {
                                if (isSavingStatus && isEditing) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                                } else if (!isEditing) {
                                    IconButton(onClick = onEditClick, enabled = !isSaving) {
                                        Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        },
                        keyboardOptions = keyboardOptions,
                        keyboardActions = KeyboardActions(onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onSaveClick()
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
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
                    if (isErrorState && errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 48.dp, top = 4.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 1. Name Field (Auto-Capitalization & Trimming)
            DetailField(
                label = "Name",
                value = currentName,
                onValueChange = { currentName = it },
                icon = Icons.Outlined.Person,
                isEditing = editingField == "Name",
                isSavingStatus = isSaving,
                onEditClick = { editingField = "Name" },
                onSaveClick = {
                    val cleanName = currentName.trim().replace("\\s+".toRegex(), " ")
                    currentName = cleanName
                    saveFieldToFirestore("name", cleanName) { editingField = null }
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done)
            )
            
            // 2. Username Field (Lowercase, Live Check)
            DetailField(
                label = "Username", 
                value = currentUsername, 
                onValueChange = { 
                    currentUsername = it.lowercase().replace(" ", "")
                    usernameError = false
                }, 
                icon = Icons.Outlined.Badge,
                isEditing = editingField == "Username",
                isSavingStatus = isSaving,
                onEditClick = { editingField = "Username" },
                onSaveClick = {
                    val cleanUser = currentUsername.trim().lowercase().replace(" ", "")
                    currentUsername = cleanUser
                    if (cleanUser == username) {
                        editingField = null
                    } else {
                        isSaving = true
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val db = FirebaseFirestore.getInstance()
                                val check = db.collection("Users").whereEqualTo("username", cleanUser).get().await()
                                if (!check.isEmpty) {
                                    withContext(Dispatchers.Main) {
                                        isSaving = false
                                        usernameError = true
                                        Toast.makeText(context, "Username already taken!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                                    if (!userQuery.isEmpty) {
                                        userQuery.documents[0].reference.update("username", cleanUser).await()
                                        withContext(Dispatchers.Main) {
                                            isSaving = false
                                            editingField = null
                                            usernameError = false
                                            onProfileUpdated()
                                            Toast.makeText(context, "Username updated!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isSaving = false
                                    Toast.makeText(context, "Update Failed!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                isErrorState = usernameError,
                errorMessage = "This username is already used by someone."
            )

            // 3. Mobile Number Field (Validation)
            DetailField(
                label = "Mobile No.",
                value = currentMobile,
                onValueChange = { 
                    if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                        if(it.length <= 10) currentMobile = it
                    }
                },
                icon = Icons.Outlined.Phone,
                isEditing = editingField == "Mobile",
                isSavingStatus = isSaving,
                onEditClick = { editingField = "Mobile" },
                onSaveClick = {
                    val cleanMobile = currentMobile.trim().removePrefix("+91")
                    if (cleanMobile.length == 10) {
                        saveFieldToFirestore("mobile_no_", cleanMobile) { editingField = null }
                    } else {
                        Toast.makeText(context, "Mobile number must be 10 digits!", Toast.LENGTH_SHORT).show()
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
            )
            
            // 4. Email ID (Fully Locked)
            DetailField(
                label = "Email ID", 
                value = currentEmail, 
                onValueChange = { }, 
                icon = Icons.Outlined.Email, 
                isEditableField = false, // Locks the field completely
                isEditing = false
            )
            
            // 5. Date of Birth (Calendar Engine)
            if (editingField == "DOB") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    com.kartikey.rupeeflow.UI_Screens.CustomDatePicker(
                        label = "Date of Birth",
                        selectedDateMillis = dobMillis,
                        onDateSelected = { millis ->
                            currentDob = sdf.format(Date(millis))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        restrictToCurrentMonth = false
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { editingField = null }) { 
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) 
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val dobDate = try { sdf.parse(currentDob.trim()) } catch (e: Exception) { null }
                                if (dobDate != null) {
                                    saveFieldToFirestore("dob", Timestamp(dobDate)) { editingField = null }
                                } else {
                                    saveFieldToFirestore("dob", currentDob.trim()) { editingField = null }
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            else Text("Save Date", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                DetailField(
                    label = "Date of Birth", 
                    value = currentDob, 
                    onValueChange = { }, 
                    icon = Icons.Outlined.CalendarToday, // Calendar Icon Instead of Cake
                    isEditing = false,
                    isSavingStatus = false,
                    onEditClick = { editingField = "DOB" }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
