package com.kartikey.rupeeflow.UI_Screens.Home.Contri.InContri

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kartikey.rupeeflow.UI_Screens.bounceClick

// ==========================================
// CORE DATA MODELS
// ==========================================
data class ContriExpense(
    val key: String,
    val itemName: String,
    val amount: Double,
    val date: String,
    val rawDateMillis: Long
)

data class MemberLedger(
    val userId: String,
    val memberName: String,
    val totalSpent: Double,
    val expenses: List<ContriExpense>,
    val isVerified: Boolean = false
)

data class Settlement(val from: String, val to: String, val amount: Double)
data class PastCycle(val dateRange: String, val totalAmount: String)

// ==========================================
// BASE-26 EXPENSE ID GENERATOR
// ==========================================
fun generateExpenseId(index: Int): String {
    var n = index
    var result = ""
    while (n >= 0) {
        result = ('A' + (n % 26)).toString() + result
        n = (n / 26) - 1
    }
    return result.padStart(4, '0')
}

// ==========================================
// ADMIN & LIFECYCLE MANAGEMENT DIALOGS
// ==========================================
@Composable
fun AdminSettingsDialog(
    initialName: String,
    initialPin: String,
    ledgers: List<MemberLedger>,
    currentUserId: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onRemoveMemberClick: (MemberLedger) -> Unit
) {
    var editName by remember { mutableStateOf(initialName) }
    var editPin by remember { mutableStateOf(initialPin) }
    var isEditingName by remember { mutableStateOf(false) }
    var isEditingPin by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Room Settings", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = editName,
                    onValueChange = { 
                        editName = it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() }
                    },
                    label = { Text("Contri Name", fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true,
                    readOnly = !isEditingName,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isEditingName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), 
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    trailingIcon = {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).bounceClick { isEditingName = !isEditingName },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = if (isEditingName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = editPin,
                    onValueChange = { newValue -> 
                        if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                            editPin = newValue
                        }
                    },
                    label = { Text("Contri Pin", fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    readOnly = !isEditingPin,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isEditingPin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), 
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    trailingIcon = {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).bounceClick { isEditingPin = !isEditingPin },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = if (isEditingPin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))
                Text("Manage Members", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 170.dp)
                        .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    val allNames = ledgers.map { it.memberName }
                    if (ledgers.isNotEmpty()) {
                        LazyColumn {
                            itemsIndexed(ledgers) { index, member ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val rawName = if (member.userId == currentUserId) "You" else formatMemberDisplayName(member.memberName, allNames)
                                    val dispName = if (rawName.length > 12) rawName.take(12) + "..." else rawName
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = dispName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                                        if (member.isVerified) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Filled.Verified,
                                                contentDescription = "Verified",
                                                tint = Color(0xFF1DA1F2),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                    
                                    if (member.userId == currentUserId) {
                                        Text("Admin", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(24.dp).bounceClick { onRemoveMemberClick(member) }
                                        )
                                    }
                                }
                                if (index < ledgers.size - 1) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), thickness = 1.dp)
                                }
                            }
                        }
                    } else {
                        Text("No members found.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Cancel", 
                        color = MaterialTheme.colorScheme.onSurfaceVariant, 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.bounceClick { onDismiss() }.padding(8.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .bounceClick { 
                                if (editName.isNotBlank() && editPin.length == 6) {
                                    onSave(editName, editPin)
                                }
                            }
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) { 
                        Text("Save Changes", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) 
                    }
                }
            }
        }
    }
}

@Composable
fun RemoveMemberDialog(memberName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove Member?", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface) },
        text = { Text("Are you sure you want to kick '$memberName'? This will deduct their expenses from the total.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        containerColor = MaterialTheme.colorScheme.surface,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Remove", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) }
        }
    )
}

@Composable
fun NewCycleDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start New Cycle?", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface) },
        text = { Text("Once created, calculations will restart from zero.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        containerColor = MaterialTheme.colorScheme.surface,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("New", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) } 
        }
    )
}

@Composable
fun LeaveRoomDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Leave Room?", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface) },
        text = { Text("Are you sure you want to leave this Contri room? Your expenses will be deducted from the total.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        containerColor = MaterialTheme.colorScheme.surface,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Leave", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) } 
        }
    )
}
