package com.kartikey.rupeeflow.UI_Screens.Home.Contri

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// ==========================================
// DIRECT AUTO-JOIN DIALOG (FROM QR SCAN)
// ==========================================
@Composable
fun AutoJoinContriDialog(
    username: String,
    roomCode: String,
    pin: String,
    roomName: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val db = FirebaseFirestore.getInstance()
                val query = db.collection("Contri")
                    .whereEqualTo("contri_code", roomCode)
                    .get()
                    .await()
                
                if (!query.isEmpty) {
                    val doc = query.documents[0]
                    val dbPasskey = doc.getString("passkey")
                    
                    if (dbPasskey == pin || dbPasskey.isNullOrEmpty()) {
                        val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                        
                        if (!userQuery.isEmpty) {
                            val userDoc = userQuery.documents[0]
                            val userDocRef = userDoc.reference
                            val currentUserId = userDoc.id
                            
                            val existingRooms = userDoc.get("rooms") as? List<*> ?: emptyList<Any>()
                            if (existingRooms.size >= 5) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Limit reached! Max 5 Contri rooms allowed.", Toast.LENGTH_LONG).show()
                                    onDismiss()
                                }
                                return@launch
                            }
                            
                            doc.reference.update("member_ids", FieldValue.arrayUnion(currentUserId)).await()

                            val docId = doc.id 
                            val prefix = docId.split("_").firstOrNull() ?: "000"
                            val exactRoomName = doc.getString("contri_name") ?: roomName
                            val validPin = dbPasskey?.ifBlank { pin } ?: pin
                            
                            val roomArrayString = "${prefix}_${roomCode}_${exactRoomName}_$validPin"
                            
                            userDocRef.update("rooms", FieldValue.arrayUnion(roomArrayString)).await()
                            
                            withContext(Dispatchers.Main) { 
                                Toast.makeText(context, "Joined $exactRoomName!", Toast.LENGTH_SHORT).show()
                                onSuccess() 
                            }
                        } else {
                            withContext(Dispatchers.Main) { 
                                Toast.makeText(context, "Error: User Profile not found!", Toast.LENGTH_LONG).show()
                                onDismiss() 
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) { 
                            Toast.makeText(context, "Invalid Pin!", Toast.LENGTH_LONG).show()
                            onDismiss() 
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) { 
                        Toast.makeText(context, "Room not found!", Toast.LENGTH_LONG).show()
                        onDismiss() 
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                    onDismiss()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = { /* Lock back press */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp), 
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary, 
                    modifier = Modifier.size(36.dp), 
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Joining Room...", 
                    fontSize = 13.sp, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = roomName, 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.ExtraBold, 
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ==========================================
// JOIN CONTRI ROOM DIALOG (QR & MANUAL)
// ==========================================
@Composable
fun JoinContriDialog(
    username: String, 
    initialScannedCode: String = "", 
    onScanClick: () -> Unit, 
    onDismiss: () -> Unit, 
    onSuccess: () -> Unit
) {
    var viewState by remember { mutableIntStateOf(if (initialScannedCode.isNotBlank()) 1 else 0) }
    
    var roomCode by remember(initialScannedCode) { 
        mutableStateOf(initialScannedCode.replace("-", "").filter { it.isLetterOrDigit() }.uppercase().take(9)) 
    }
    var pin by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            if (viewState == 0) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.SpaceBetween, 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Join Contri", 
                            fontSize = 20.sp, 
                            fontWeight = FontWeight.ExtraBold, 
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.Close, 
                                contentDescription = "Close", 
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                                .bounceClick { onScanClick() },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.QrCodeScanner, 
                                contentDescription = "Scan QR", 
                                modifier = Modifier.size(28.dp), 
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Scan QR", 
                                fontSize = 13.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(40.dp)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                                .bounceClick { viewState = 1 },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Keyboard, 
                                contentDescription = "Manual", 
                                modifier = Modifier.size(28.dp), 
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Enter Manually", 
                                fontSize = 13.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isSubmitting) {
                            IconButton(onClick = { viewState = 0 }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowBack, 
                                    contentDescription = "Back", 
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Enter Details", 
                            fontSize = 20.sp, 
                            fontWeight = FontWeight.ExtraBold, 
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.size(24.dp)) 
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = roomCode,
                        onValueChange = { raw ->
                            roomCode = raw.replace("-", "").filter { it.isLetterOrDigit() }.uppercase().take(9)
                        },
                        label = { Text("Enter Contri Code") },
                        placeholder = { Text("ABC-123-XYZ") },
                        singleLine = true,
                        visualTransformation = RoomCodeVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary, 
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) pin = it },
                        label = { Text("Enter Pin") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary, 
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    AnimatedVisibility(visible = pin.isNotEmpty() && pin.length < 6) {
                        Text(
                            text = "Enter 6 digits", 
                            color = MaterialTheme.colorScheme.error, 
                            fontSize = 11.sp, 
                            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 2.dp),
                            textAlign = TextAlign.Start
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (roomCode.length == 9 && pin.length == 6) {
                                isSubmitting = true
                                val formattedCode = roomCode.chunked(3).joinToString("-")
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val db = FirebaseFirestore.getInstance()
                                        val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                                        
                                        if (!userQuery.isEmpty) {
                                            val userDoc = userQuery.documents[0]
                                            val userDocRef = userDoc.reference
                                            val currentUserId = userDoc.id
                                            
                                            val existingRooms = userDoc.get("rooms") as? List<*> ?: emptyList<Any>()
                                            if (existingRooms.size >= 5) {
                                                withContext(Dispatchers.Main) {
                                                    isSubmitting = false
                                                    Toast.makeText(context, "Limit reached! Max 5 Contri rooms allowed.", Toast.LENGTH_LONG).show()
                                                }
                                                return@launch
                                            }

                                            val query = db.collection("Contri").whereEqualTo("contri_code", formattedCode).get().await()
                                            
                                            if (!query.isEmpty) {
                                                val doc = query.documents[0]
                                                val dbPasskey = doc.getString("passkey")
                                                
                                                if (dbPasskey == pin || dbPasskey.isNullOrEmpty()) {
                                                    val roomName = doc.getString("contri_name") ?: "Contri Room"
                                                    
                                                    doc.reference.update("member_ids", FieldValue.arrayUnion(currentUserId)).await()
                                                    
                                                    val docId = doc.id 
                                                    val prefix = docId.split("_").firstOrNull() ?: "000"
                                                    val validPin = dbPasskey?.ifBlank { pin } ?: pin
                                                    val roomArrayString = "${prefix}_${formattedCode}_${roomName}_$validPin"
                                                    
                                                    userDocRef.update("rooms", FieldValue.arrayUnion(roomArrayString)).await()
                                                    
                                                    withContext(Dispatchers.Main) {
                                                        isSubmitting = false
                                                        Toast.makeText(context, "Joined Successfully!", Toast.LENGTH_SHORT).show()
                                                        onSuccess()
                                                    }
                                                } else {
                                                    withContext(Dispatchers.Main) {
                                                        isSubmitting = false
                                                        Toast.makeText(context, "Invalid Pin", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            } else {
                                                withContext(Dispatchers.Main) {
                                                    isSubmitting = false
                                                    Toast.makeText(context, "Room not found", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isSubmitting = false
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Enter Valid Code & Pin", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        enabled = !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary, 
                                modifier = Modifier.size(24.dp), 
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Join Contri", 
                                color = MaterialTheme.colorScheme.onPrimary, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// CODE MASK (ABC-123-XYZ) VISUAL TRANSFORMATION
// ==========================================
class RoomCodeVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.take(9)
        val formatted = buildString {
            for (i in raw.indices) {
                append(raw[i])
                if ((i == 2 || i == 5) && i != raw.lastIndex) {
                    append("-")
                }
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset <= 3) return offset
                if (offset <= 6) return offset + 1
                if (offset <= 9) return offset + 2
                return formatted.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset <= 3) return offset
                if (offset <= 7) return (offset - 1).coerceAtLeast(0)
                if (offset <= 11) return (offset - 2).coerceAtLeast(0)
                return raw.length
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}
