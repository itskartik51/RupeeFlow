package com.kartikey.rupeeflow.UI_Screens.Home.Contri

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ==========================================
// CREATE CONTRI ROOM DIALOG
// ==========================================
@Composable
fun CreateContriDialog(
    username: String, 
    onDismiss: () -> Unit, 
    onSuccess: () -> Unit
) {
    var contriName by remember { mutableStateOf("") }
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
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.SpaceBetween, 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Create Room", 
                        fontSize = 20.sp, 
                        fontWeight = FontWeight.ExtraBold, 
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!isSubmitting) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.Close, 
                                contentDescription = "Close", 
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = contriName,
                    onValueChange = { 
                        contriName = it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } 
                    },
                    label = { Text("Contri Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
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
                    label = { Text("Create Pin") },
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
                        if (contriName.isNotBlank() && pin.length == 6) {
                            isSubmitting = true
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

                                        val metaRef = db.collection("System").document("Metadata")
                                        val metaDoc = metaRef.get().await()
                                        
                                        val lastCounter = metaDoc.getLong("last_contri_id") ?: 0L
                                        val nextCounter = lastCounter + 1
                                        metaRef.set(mapOf("last_contri_id" to nextCounter), SetOptions.merge()).await()

                                        val counterStr = String.format(Locale.US, "%03d", nextCounter)
                                        val randomChars = (1..6).map { ('A'..'Z').random() }.joinToString("")
                                        val randomCode = "${randomChars.take(3)}-${(100..999).random()}-${randomChars.takeLast(3)}"
                                        
                                        val docId = "${counterStr}_${contriName.replace(" ", "")}_$randomCode"
                                        val todayStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

                                        val contriData = hashMapOf(
                                            "contri_code" to randomCode,
                                            "contri_date" to todayStr,
                                            "contri_name" to contriName,
                                            "member_ids" to listOf(currentUserId),
                                            "admin_id" to currentUserId,
                                            "passkey" to pin,
                                            "total_group_expense" to 0.0,
                                            "expenses_data" to emptyMap<String, Any>() 
                                        )
                                        
                                        db.collection("Contri").document(docId).set(contriData).await()
                                        
                                        val roomArrayString = "${counterStr}_${randomCode}_${contriName}_$pin"
                                        userDocRef.update("rooms", FieldValue.arrayUnion(roomArrayString)).await()
                                        
                                        withContext(Dispatchers.Main) {
                                            isSubmitting = false
                                            Toast.makeText(context, "Room Created!", Toast.LENGTH_SHORT).show()
                                            onSuccess()
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
                            Toast.makeText(context, "Fill details correctly", Toast.LENGTH_SHORT).show()
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
                            text = "Create Contri", 
                            color = MaterialTheme.colorScheme.onPrimary, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
