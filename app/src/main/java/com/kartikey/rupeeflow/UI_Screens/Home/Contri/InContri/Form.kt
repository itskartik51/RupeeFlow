package com.kartikey.rupeeflow.UI_Screens.Home.Contri.InContri

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kartikey.rupeeflow.UI_Screens.CustomDatePicker
import com.kartikey.rupeeflow.UI_Screens.bounceClick

// ==========================================
// PREMIUM BOUNCE FLOATING BUTTON (+)
// ==========================================
@Composable
fun PremiumFloatingButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .bounceClick { onClick() }
            .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) { 
        Icon(
            imageVector = Icons.Outlined.Add, 
            contentDescription = "Add Expense", 
            tint = MaterialTheme.colorScheme.onPrimary, 
            modifier = Modifier.size(28.dp)
        ) 
    }
}

// ==========================================
// ADD EXPENSE POPUP DIALOG
// ==========================================
@Composable
fun AddContriExpenseDialog(
    onDismiss: () -> Unit, 
    onAdd: (String, Long, Double) -> Unit
) {
    var expenseTitle by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dateMillis by remember { mutableStateOf<Long?>(System.currentTimeMillis()) }

    Dialog(
        onDismissRequest = onDismiss, 
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(), 
            shape = RoundedCornerShape(20.dp), 
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), 
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Add New Expense", 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.ExtraBold, 
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = expenseTitle,
                    onValueChange = { 
                        expenseTitle = it.replaceFirstChar { char -> 
                            if (char.isLowerCase()) char.titlecase() else char.toString() 
                        } 
                    },
                    label = { Text("Expense Title", fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true,
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

                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CustomDatePicker(
                        label = "Date", 
                        selectedDateMillis = dateMillis, 
                        onDateSelected = { dateMillis = it }, 
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) amount = it },
                        label = { Text("Amount", fontSize = 13.sp) },
                        prefix = { Text("₹ ", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary, 
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.End, 
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                                val amt = amount.toDoubleOrNull()
                                if (expenseTitle.isNotBlank() && amt != null && dateMillis != null) {
                                    onAdd(expenseTitle, dateMillis!!, amt)
                                }
                            }
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) { 
                        Text("Add Expense", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) 
                    }
                }
            }
        }
    }
}

// ==========================================
// EDIT EXPENSE POPUP DIALOG
// ==========================================
@Composable
fun EditContriExpenseDialog(
    expense: ContriExpense,
    onDismiss: () -> Unit,
    onUpdate: (String, Long, Double) -> Unit
) {
    var expenseTitle by remember { mutableStateOf(expense.itemName) }
    var amount by remember { 
        mutableStateOf(
            if (expense.amount % 1.0 == 0.0) expense.amount.toInt().toString() else expense.amount.toString()
        ) 
    }
    var dateMillis by remember { mutableStateOf<Long?>(expense.rawDateMillis) }

    Dialog(
        onDismissRequest = onDismiss, 
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Edit Expense", 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.ExtraBold, 
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = expenseTitle,
                    onValueChange = { 
                        expenseTitle = it.replaceFirstChar { char -> 
                            if (char.isLowerCase()) char.titlecase() else char.toString() 
                        } 
                    },
                    label = { Text("Expense Title", fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true,
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

                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CustomDatePicker(
                        label = "Date", 
                        selectedDateMillis = dateMillis, 
                        onDateSelected = { dateMillis = it }, 
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) amount = it },
                        label = { Text("Amount", fontSize = 13.sp) },
                        prefix = { Text("₹ ", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary, 
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.End, 
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                                val amt = amount.toDoubleOrNull()
                                if (expenseTitle.isNotBlank() && amt != null && dateMillis != null) {
                                    onUpdate(expenseTitle, dateMillis!!, amt)
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

// ==========================================
// DELETE EXPENSE CONFIRMATION DIALOG
// ==========================================
@Composable
fun DeleteExpenseConfirmationDialog(
    expense: ContriExpense,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val displayAmount = if (expense.amount % 1.0 == 0.0) expense.amount.toInt().toString() else expense.amount.toString()
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Delete Expense?", 
                    fontSize = 20.sp, 
                    fontWeight = FontWeight.ExtraBold, 
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Are you sure you want to delete this ₹$displayAmount expense? This amount will be deducted from the Contri total.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    
                    Surface(
                        modifier = Modifier.bounceClick { onConfirm() },
                        color = Color(0xFFFF5252),
                        shape = RoundedCornerShape(50)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Delete",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
