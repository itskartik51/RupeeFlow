package com.kartikey.rupeeflow.UI_Screens

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kartikey.rupeeflow.Cloud_Database.Constants
import com.kartikey.rupeeflow.UI_Screens.Add.TransactionModel
import com.kartikey.rupeeflow.UI_Screens.Assets.BankAccountItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.CreditCardItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.FDItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// HELPER: To update Budget Collection automatically
suspend fun updateBudgetUsage(userRef: DocumentReference, diff: Double) {
    val budgetDocRef = userRef.collection("Finances").document("Budget")
    val budgetDoc = budgetDocRef.get().await()
    if (budgetDoc.exists()) {
        val userDoc = userRef.get().await()
        val limitVal = userDoc.getDouble("budget_limit") ?: 0.0
        val usedStr = budgetDoc.getString("used") ?: "0"
        val currentUsed = usedStr.substringBefore(" ").toDoubleOrNull() ?: 0.0
        
        val newUsed = (currentUsed + diff).coerceAtLeast(0.0)
        val usedPct = if (limitVal > 0) (newUsed / limitVal) * 100 else 0.0
        val availAmtCalc = maxOf(0.0, limitVal - newUsed)
        val availPctStr = if (limitVal > 0) (availAmtCalc / limitVal) * 100 else 0.0
        
        val formatVal = { amt: Double, pct: Double ->
            "${amt.toInt()} (${String.format(Locale.US, "%.1f", pct)}%)"
        }
        
        budgetDocRef.set(hashMapOf(
            "used" to formatVal(newUsed, usedPct),
            "available" to formatVal(availAmtCalc, availPctStr)
        ), SetOptions.merge()).await()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBankDialog(bank: BankAccountItem, username: String, onDismiss: () -> Unit, onUpdateSuccess: () -> Unit) {
    var bankName by remember { mutableStateOf(bank.bankName) }
    var bankBalance by remember { mutableStateOf(bank.currentBalance.toString()) }
    var interestRate by remember { mutableStateOf(bank.interestRate.toString()) }
    var expanded by remember { mutableStateOf(false) }
    
    val filteredBanks = if (bankName.isNotBlank()) Constants.IndianBanksList.filter { it.contains(bankName, ignoreCase = true) && !it.equals(bankName, ignoreCase = true) } else emptyList()
    val showDropdown = expanded && filteredBanks.isNotEmpty()
    
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    var isUpdatePressed by remember { mutableStateOf(false) }
    val updateButtonScale by animateFloatAsState(targetValue = if (isUpdatePressed) 0.95f else 1f, label = "UpdateAnim")
    var isCancelPressed by remember { mutableStateOf(false) }
    val cancelButtonScale by animateFloatAsState(targetValue = if (isCancelPressed) 0.95f else 1f, label = "CancelAnim")

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Delete Bank Account", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Are you sure you want to permanently remove this bank account? This action cannot be undone.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDismiss()
                        Toast.makeText(context, "Deleting bank...", Toast.LENGTH_SHORT).show()
                        
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val db = FirebaseFirestore.getInstance()
                                val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                                if (!userQuery.isEmpty) {
                                    val userRef = userQuery.documents[0].reference
                                    val bankDocRef = userRef.collection("Finances").document("Bank")
                                    val bankDoc = bankDocRef.get().await()
                                    
                                    if (bankDoc.exists()) {
                                        val data = bankDoc.data ?: emptyMap()
                                        var targetKey: String? = null
                                        for ((key, rawBank) in data) {
                                            if (key != "last_updated" && rawBank is Map<*, *>) {
                                                val accNo = rawBank["account no."]?.toString()
                                                if (accNo == bank.accountNo) {
                                                    targetKey = key
                                                    break
                                                }
                                            }
                                        }
                                        
                                        if (targetKey != null) {
                                            val updates = hashMapOf<String, Any>(
                                                targetKey to FieldValue.delete()
                                            )
                                            bankDocRef.update(updates).await()
                                            withContext(Dispatchers.Main) { onUpdateSuccess() }
                                        } else {
                                            withContext(Dispatchers.Main) { Toast.makeText(context, "Bank not found to delete!", Toast.LENGTH_SHORT).show() }
                                        }
                                    }
                                } else {
                                    withContext(Dispatchers.Main) { Toast.makeText(context, "Failed to delete account!", Toast.LENGTH_SHORT).show() }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) { Toast.makeText(context, "Failed to delete account!", Toast.LENGTH_SHORT).show() }
                            }
                        }
                    }, 
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { 
                    Text("Delete", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold) 
                }
            }, 
            dismissButton = { 
                TextButton(onClick = { showDeleteConfirm = false }) { 
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) 
                } 
            }
        )
    }

    Dialog(onDismissRequest = { onDismiss() }, properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp), 
            shape = RoundedCornerShape(20.dp), 
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), 
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp), 
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.SpaceBetween, 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Edit Bank Details", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    IconButton(onClick = { showDeleteConfirm = true }) { 
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete Bank", tint = MaterialTheme.colorScheme.error) 
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                ExposedDropdownMenuBox(expanded = showDropdown, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = bankName, 
                        onValueChange = { bankName = it; expanded = true }, 
                        label = { Text("Bank Name") }, 
                        modifier = Modifier.fillMaxWidth().menuAnchor(), 
                        singleLine = true, 
                        shape = RoundedCornerShape(12.dp), 
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary, 
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    if (showDropdown) { 
                        ExposedDropdownMenu(
                            expanded = showDropdown, 
                            onDismissRequest = { expanded = false }, 
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) { 
                            filteredBanks.forEach { selectionOption -> 
                                DropdownMenuItem(
                                    text = { Text(selectionOption, color = MaterialTheme.colorScheme.onSurface) }, 
                                    onClick = { bankName = selectionOption; expanded = false }
                                ) 
                            } 
                        } 
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = bankBalance, 
                        onValueChange = { bankBalance = it }, 
                        label = { Text("Balance") }, 
                        prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                        modifier = Modifier.weight(1f), 
                        singleLine = true, 
                        shape = RoundedCornerShape(12.dp), 
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary, 
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    OutlinedTextField(
                        value = interestRate, 
                        onValueChange = { interestRate = it }, 
                        label = { Text("Interest (Yr)") }, 
                        suffix = { Text("%") }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                        modifier = Modifier.weight(1f), 
                        singleLine = true, 
                        shape = RoundedCornerShape(12.dp), 
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary, 
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                Spacer(modifier = Modifier.height(28.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { onDismiss() }, 
                        modifier = Modifier.weight(1f).height(50.dp).scale(cancelButtonScale).pointerInput(Unit) { detectTapGestures(onPress = { isCancelPressed = true; tryAwaitRelease(); isCancelPressed = false }) }, 
                        shape = RoundedCornerShape(12.dp), 
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant), 
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) { 
                        Text("Cancel", fontWeight = FontWeight.Bold) 
                    }
                    
                    Button(
                        onClick = {
                            val newBal = bankBalance.toDoubleOrNull()
                            val newRate = interestRate.toDoubleOrNull()
                            if (bankName.isNotBlank() && newBal != null && newRate != null) {
                                if (!Constants.IndianBanksList.contains(bankName)) { 
                                    Toast.makeText(context, "Please select a valid bank from the dropdown!", Toast.LENGTH_SHORT).show()
                                    return@Button 
                                }
                                
                                onDismiss()
                                Toast.makeText(context, "Updating bank...", Toast.LENGTH_SHORT).show()
                                
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val db = FirebaseFirestore.getInstance()
                                        val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                                        if (!userQuery.isEmpty) {
                                            val userRef = userQuery.documents[0].reference
                                            val bankDocRef = userRef.collection("Finances").document("Bank")
                                            val bankDoc = bankDocRef.get().await()
                                            
                                            if (bankDoc.exists()) {
                                                val data = bankDoc.data ?: emptyMap()
                                                var targetKey: String? = null
                                                for ((key, rawBank) in data) {
                                                    if (key != "last_updated" && rawBank is Map<*, *>) {
                                                        val accNo = rawBank["account no."]?.toString()
                                                        if (accNo == bank.accountNo) {
                                                            targetKey = key
                                                            break
                                                        }
                                                    }
                                                }
                                                
                                                if (targetKey != null) {
                                                    val updateMap = hashMapOf<String, Any>(
                                                        "$targetKey.bank" to bankName,
                                                        "$targetKey.current bal." to newBal,
                                                        "$targetKey.intrest % (yr)" to newRate
                                                    )
                                                    bankDocRef.update(updateMap).await()
                                                    withContext(Dispatchers.Main) { onUpdateSuccess() }
                                                } else {
                                                    withContext(Dispatchers.Main) { Toast.makeText(context, "Bank not found to update!", Toast.LENGTH_SHORT).show() }
                                                }
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) { Toast.makeText(context, "Update Failed!", Toast.LENGTH_SHORT).show() }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) { Toast.makeText(context, "Update Failed!", Toast.LENGTH_SHORT).show() }
                                    }
                                }
                            } else { 
                                Toast.makeText(context, "Please enter valid details", Toast.LENGTH_SHORT).show() 
                            }
                        }, 
                        modifier = Modifier.weight(1f).height(50.dp).scale(updateButtonScale).pointerInput(Unit) { detectTapGestures(onPress = { isUpdatePressed = true; tryAwaitRelease(); isUpdatePressed = false }) }, 
                        shape = RoundedCornerShape(12.dp), 
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { 
                        Text("Update", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) 
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickUpdateCCDialog(cc: CreditCardItem, username: String, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var updateAmount by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f).imePadding(), 
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Update Outstanding", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("Add spend (+) or pay bill (-) on ${cc.issuer}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                
                Spacer(modifier = Modifier.height(20.dp))
                
                OutlinedTextField(
                    value = updateAmount,
                    onValueChange = { updateAmount = it },
                    label = { Text("Amount (+ or -)") },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary, 
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { onDismiss() },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = {
                            val amountEntered = updateAmount.toDoubleOrNull()
                            if (amountEntered != null && amountEntered != 0.0) {
                                val newCalculatedOutstanding = cc.outstanding + amountEntered 
                                
                                onDismiss()
                                Toast.makeText(context, "Updating card...", Toast.LENGTH_SHORT).show()
                                
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val db = FirebaseFirestore.getInstance()
                                        val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                                        if (!userQuery.isEmpty) {
                                            val userRef = userQuery.documents[0].reference
                                            
                                            val updateMap = hashMapOf<String, Any>(
                                                "CC.${cc.cardNo}.outstanding" to newCalculatedOutstanding
                                            )
                                            userRef.collection("Finances").document("CC FD").update(updateMap).await()
                                            withContext(Dispatchers.Main) { onSuccess() }
                                        } else {
                                            withContext(Dispatchers.Main) { Toast.makeText(context, "Update Failed!", Toast.LENGTH_SHORT).show() }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) { Toast.makeText(context, "Update Failed!", Toast.LENGTH_SHORT).show() }
                                    }
                                }
                            } else { Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show() }
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Update", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCreditCardDialog(cc: CreditCardItem, username: String, onDismiss: () -> Unit, onUpdateSuccess: () -> Unit) {
    var limit by remember { mutableStateOf(cc.limit.toString()) }
    var billingDay by remember { mutableStateOf(cc.billingDay.toString()) }
    var dueDay by remember { mutableStateOf(cc.dueDay.toString()) }
    var annualFee by remember { mutableStateOf(cc.annualFee.toString()) }
    
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Remove Card", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Are you sure you want to permanently delete this Credit Card record?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDismiss()
                        Toast.makeText(context, "Deleting card...", Toast.LENGTH_SHORT).show()
                        
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val db = FirebaseFirestore.getInstance()
                                val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                                if (!userQuery.isEmpty) {
                                    val userRef = userQuery.documents[0].reference
                                    
                                    val updates = hashMapOf<String, Any>("CC.${cc.cardNo}" to FieldValue.delete())
                                    userRef.collection("Finances").document("CC FD").update(updates).await()
                                    withContext(Dispatchers.Main) { onUpdateSuccess() }
                                } else {
                                    withContext(Dispatchers.Main) { Toast.makeText(context, "Failed to delete card!", Toast.LENGTH_SHORT).show() }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) { Toast.makeText(context, "Failed to delete card!", Toast.LENGTH_SHORT).show() }
                            }
                        }
                    }, 
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { 
                    Text("Delete", color = MaterialTheme.colorScheme.onError) 
                }
            }, 
            dismissButton = { 
                TextButton(onClick = { showDeleteConfirm = false }) { 
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) 
                } 
            }
        )
    }

    Dialog(onDismissRequest = { onDismiss() }, properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp), 
            shape = RoundedCornerShape(20.dp), 
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), 
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp), 
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.SpaceBetween, 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Edit Credit Card", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    IconButton(onClick = { showDeleteConfirm = true }) { 
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete CC", tint = MaterialTheme.colorScheme.error) 
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = limit, 
                    onValueChange = { limit = it }, 
                    label = { Text("Total Limit") }, 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary, 
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = billingDay, 
                        onValueChange = { billingDay = it }, 
                        label = { Text("Bill Day (1-31)") }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                        modifier = Modifier.weight(1f), 
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary, 
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    OutlinedTextField(
                        value = dueDay, 
                        onValueChange = { dueDay = it }, 
                        label = { Text("Due Day (1-31)") }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                        modifier = Modifier.weight(1f), 
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary, 
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = annualFee, 
                    onValueChange = { annualFee = it }, 
                    label = { Text("Annual Fee") }, 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary, 
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                
                Spacer(modifier = Modifier.height(28.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { onDismiss() }, 
                        modifier = Modifier.weight(1f).height(50.dp), 
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) { 
                        Text("Cancel", fontWeight = FontWeight.Bold) 
                    }
                    Button(
                        onClick = {
                            val newLimit = limit.toDoubleOrNull()
                            if (newLimit != null) {
                                onDismiss()
                                Toast.makeText(context, "Updating card...", Toast.LENGTH_SHORT).show()
                                
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val db = FirebaseFirestore.getInstance()
                                        val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                                        if (!userQuery.isEmpty) {
                                            val userRef = userQuery.documents[0].reference

                                            val updateMap = hashMapOf<String, Any>(
                                                "CC.${cc.cardNo}.limit" to newLimit,
                                                "CC.${cc.cardNo}.billing" to (billingDay.toIntOrNull() ?: 0),
                                                "CC.${cc.cardNo}.due" to (dueDay.toIntOrNull() ?: 0)
                                            )
                                            
                                            // SPARSE DATA LOGIC: Delete key if fee is updated to 0
                                            val updatedAnnFee = annualFee.toDoubleOrNull() ?: 0.0
                                            if (updatedAnnFee > 0.0) {
                                                updateMap["CC.${cc.cardNo}.yr fee"] = updatedAnnFee
                                            } else {
                                                updateMap["CC.${cc.cardNo}.yr fee"] = FieldValue.delete()
                                            }

                                            userRef.collection("Finances").document("CC FD").update(updateMap).await()
                                            withContext(Dispatchers.Main) { onUpdateSuccess() }
                                        } else {
                                            withContext(Dispatchers.Main) { Toast.makeText(context, "Update Failed!", Toast.LENGTH_SHORT).show() }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) { Toast.makeText(context, "Update Failed!", Toast.LENGTH_SHORT).show() }
                                    }
                                }
                            } else { Toast.makeText(context, "Check inputs!", Toast.LENGTH_SHORT).show() }
                        }, 
                        modifier = Modifier.weight(1f).height(50.dp), 
                        shape = RoundedCornerShape(12.dp), 
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { 
                        Text("Save", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) 
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFDDialog(fd: FDItem, username: String, onDismiss: () -> Unit, onUpdateSuccess: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Break / Delete FD", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Are you sure you want to delete this Fixed Deposit record?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDismiss()
                        Toast.makeText(context, "Deleting FD...", Toast.LENGTH_SHORT).show()
                        
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val db = FirebaseFirestore.getInstance()
                                val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                                if (!userQuery.isEmpty) {
                                    val userRef = userQuery.documents[0].reference
                                    
                                    val updates = hashMapOf<String, Any>("FD.${fd.accountNo}" to FieldValue.delete())
                                    userRef.collection("Finances").document("CC FD").update(updates).await()
                                    withContext(Dispatchers.Main) { onUpdateSuccess() }
                                } else {
                                    withContext(Dispatchers.Main) { Toast.makeText(context, "Failed to delete FD!", Toast.LENGTH_SHORT).show() }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) { Toast.makeText(context, "Failed to delete FD!", Toast.LENGTH_SHORT).show() }
                            }
                        }
                    }, 
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { 
                    Text("Delete", color = MaterialTheme.colorScheme.onError) 
                }
            }, 
            dismissButton = { 
                TextButton(onClick = { showDeleteConfirm = false }) { 
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) 
                } 
            }
        )
    }

    Dialog(onDismissRequest = { onDismiss() }, properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp), 
            shape = RoundedCornerShape(20.dp), 
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), 
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp), 
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.SpaceBetween, 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("FD Settings", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    IconButton(onClick = { showDeleteConfirm = true }) { 
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete FD", tint = MaterialTheme.colorScheme.error) 
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Fixed Deposit records cannot be freely edited to maintain interest accuracy. If you need to make changes, please Delete this record and recreate a new FD.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(28.dp))
                OutlinedButton(
                    onClick = { onDismiss() }, 
                    modifier = Modifier.fillMaxWidth().height(50.dp), 
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) { 
                    Text("Close", fontWeight = FontWeight.Bold) 
                }
            }
        }
    }
}

@Composable
fun DeleteExpenseDialog(
    expense: TransactionModel,
    username: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Delete Expense?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
        text = { Text("Are you sure you want to delete this ₹${expense.amount} expense? The deducted balance will be securely refunded to your account.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    Toast.makeText(context, "Deleting expense...", Toast.LENGTH_SHORT).show()
                    
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = FirebaseFirestore.getInstance()
                            val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                            if (!userQuery.isEmpty) {
                                val userRef = userQuery.documents[0].reference
                                
                                val expDateOnly = expense.date.split(" ")[0]
                                val targetDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(expDateOnly) ?: Date()
                                val docId = SimpleDateFormat("yyyy_MM", Locale.getDefault()).format(targetDate)
                                val expensesDocRef = userRef.collection("Expenses").document(docId)
                                val docSnap = expensesDocRef.get().await()
                                
                                var targetKey: String? = null
                                
                                if (docSnap.exists()) {
                                    val dataMap = docSnap.data ?: emptyMap()
                                    
                                    for ((key, value) in dataMap) {
                                        if (value is Map<*, *>) {
                                            val dbDate = value["date"]
                                            val dbDateStr = when (dbDate) {
                                                is Timestamp -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dbDate.toDate())
                                                is String -> dbDate.toString().split(" ")[0]
                                                else -> ""
                                            }
                                            val dbAmount = (value["amount"] as? Number)?.toDouble() ?: 0.0
                                            val dbCategory = value["category"]?.toString() ?: ""
                                            
                                            val amountMatch = Math.abs(dbAmount - expense.amount) < 0.01
                                            
                                            if (dbDateStr == expDateOnly && amountMatch && dbCategory == expense.category) {
                                                targetKey = key
                                                break
                                            }
                                        }
                                    }
                                }

                                if (targetKey != null) {
                                    val updates = hashMapOf<String, Any>(
                                        targetKey to FieldValue.delete(),
                                        "000_total" to FieldValue.increment(-expense.amount)
                                    )
                                    expensesDocRef.update(updates).await()
                                } else {
                                    withContext(Dispatchers.Main) { Toast.makeText(context, "Error: Record not found in database.", Toast.LENGTH_LONG).show() }
                                    return@launch 
                                }

                                val refundAmt = expense.amount
                                when (expense.sourceType) {
                                    "Cash" -> {
                                        val bankDoc = userRef.collection("Finances").document("Bank").get().await()
                                        var cur = 0.0
                                        if (bankDoc.exists()) {
                                            val cashMap = bankDoc.get("cash") as? Map<*, *>
                                            cur = (cashMap?.get("amnt") as? Number)?.toDouble() ?: 0.0
                                        }
                                        userRef.collection("Finances").document("Bank").update("cash.amnt", cur + refundAmt).await()
                                    }
                                    "Bank" -> {
                                        if (expense.sourceId.isNotEmpty()) {
                                            val bankDoc = userRef.collection("Finances").document("Bank").get().await()
                                            val bankData = bankDoc.get(expense.sourceId) as? Map<*, *>
                                            if (bankData != null) {
                                                val curBal = (bankData["current bal."] as? Number)?.toDouble() ?: 0.0
                                                userRef.collection("Finances").document("Bank").update("${expense.sourceId}.current bal.", curBal + refundAmt).await()
                                            }
                                        }
                                    }
                                    "Credit Card" -> {
                                        if (expense.sourceId.isNotEmpty()) {
                                            val ccDoc = userRef.collection("Finances").document("CC FD").get().await()
                                            val ccMap = ccDoc.get("CC") as? Map<*, *>
                                            val ccData = ccMap?.get(expense.sourceId) as? Map<*, *>
                                            if (ccData != null) {
                                                val curOut = (ccData["outstanding"] as? Number)?.toDouble() ?: 0.0
                                                val newOut = (curOut - refundAmt).coerceAtLeast(0.0)
                                                userRef.collection("Finances").document("CC FD").update("CC.${expense.sourceId}.outstanding", newOut).await()
                                            }
                                        }
                                    }
                                }
                                
                                updateBudgetUsage(userRef, -refundAmt)

                                withContext(Dispatchers.Main) { onSuccess() }
                            } else {
                                withContext(Dispatchers.Main) { Toast.makeText(context, "Failed to delete!", Toast.LENGTH_SHORT).show() }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) { Toast.makeText(context, "Failed to delete!", Toast.LENGTH_SHORT).show() }
                        }
                    }
                }, 
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { 
                Text("Delete", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold) 
            }
        }, 
        dismissButton = { 
            TextButton(onClick = { onDismiss() }) { 
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) 
            } 
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseDialog(
    expense: TransactionModel,
    username: String,
    bankList: List<BankAccountItem>,
    ccList: List<CreditCardItem>,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val categories = listOf("Food", "Transport", "Shopping", "Bills", "Custom")
    val paymentModes = listOf("Cash", "UPI", "NEFT", "Credit Card", "Debit Card", "Net Banking")
    
    var categoryText by remember { mutableStateOf(if (categories.contains(expense.category)) expense.category else "Custom") }
    var customCategoryText by remember { mutableStateOf(if (!categories.contains(expense.category)) expense.category else "") }
    var isCustomCategory by remember { mutableStateOf(!categories.contains(expense.category)) }
    
    var remark1 by remember { mutableStateOf(expense.remark1) }
    var remark2 by remember { mutableStateOf(expense.remark2) }
    var modeText by remember { mutableStateOf(expense.mode) }
    
    var catExpanded by remember { mutableStateOf(false) }
    var modeExpanded by remember { mutableStateOf(false) }
    var paidByExpanded by remember { mutableStateOf(false) }
    
    var amount by remember { mutableStateOf(expense.amount.toString()) }

    var expenseDateMillis by remember {
        mutableStateOf(
            try {
                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val dStr = expense.date.split(" ")[0]
                format.parse(dStr)?.time ?: System.currentTimeMillis()
            } catch(e: Exception) { System.currentTimeMillis() }
        )
    }

    var selectedSourceType by remember { mutableStateOf(expense.sourceType) }
    var selectedSourceId by remember { mutableStateOf(expense.sourceId) }
    
    val initialLogo = remember {
        if (expense.sourceType == "Bank") {
            val b = bankList.find { it.accountNo == expense.sourceId }
            b?.bankName?.let { Constants.BankLogoMap[it] }
        } else if (expense.sourceType == "Credit Card") {
            val c = ccList.find { it.cardNo == expense.sourceId }
            c?.issuer?.let { Constants.BankLogoMap[it] }
        } else null
    }
    
    val initialName = remember {
        if (expense.sourceType == "Cash") "Cash in Hand"
        else if (expense.sourceType == "Bank") "• " + (expense.sourceId.takeLast(4).takeIf { it.isNotEmpty() } ?: "")
        else if (expense.sourceType == "Credit Card") "• " + (expense.sourceId.takeLast(4).takeIf { it.isNotEmpty() } ?: "")
        else ""
    }
    
    var selectedSourceName by remember { mutableStateOf(initialName) }
    var selectedSourceLogo by remember { mutableStateOf(initialLogo) }

    val context = LocalContext.current

    Dialog(onDismissRequest = { onDismiss() }, properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp), 
            shape = RoundedCornerShape(20.dp), 
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), 
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Edit Expense", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                Text("Changes will automatically refund & adjust balances.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                
                ExposedDropdownMenuBox(
                    expanded = catExpanded, 
                    onExpandedChange = { catExpanded = !catExpanded }, 
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = if(isCustomCategory) customCategoryText else categoryText, 
                        onValueChange = { if(isCustomCategory) customCategoryText = it }, 
                        readOnly = !isCustomCategory,
                        label = { Text("Category") }, 
                        modifier = Modifier.fillMaxWidth().menuAnchor(), 
                        shape = RoundedCornerShape(12.dp), 
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary, 
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded, 
                        onDismissRequest = { catExpanded = false }, 
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        categories.forEach { name -> 
                            DropdownMenuItem(
                                text = { Text(name, color = MaterialTheme.colorScheme.onSurface) }, 
                                onClick = { 
                                    categoryText = name
                                    isCustomCategory = (name == "Custom")
                                    if(!isCustomCategory) customCategoryText = ""
                                    catExpanded = false 
                                }
                            ) 
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = remark1, 
                        onValueChange = { remark1 = it }, 
                        label = { Text("Remark 1") }, 
                        modifier = Modifier.weight(1f), 
                        shape = RoundedCornerShape(12.dp), 
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary, 
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    OutlinedTextField(
                        value = remark2, 
                        onValueChange = { remark2 = it }, 
                        label = { Text("Remark 2") }, 
                        modifier = Modifier.weight(1f), 
                        shape = RoundedCornerShape(12.dp), 
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary, 
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = modeExpanded, 
                        onExpandedChange = { modeExpanded = !modeExpanded }, 
                        modifier = Modifier.weight(0.35f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .border(1.dp, if (modeExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                .menuAnchor()
                                .background(Color.Transparent, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), 
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (modeText.isEmpty()) "Mode" else modeText, 
                                    color = if (modeText.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface, 
                                    fontSize = 14.sp, 
                                    maxLines = 1, 
                                    softWrap = false, 
                                    overflow = TextOverflow.Ellipsis, 
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Outlined.ArrowDropDown, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                                    modifier = Modifier.size(20.dp).rotate(if (modeExpanded) 180f else 0f)
                                )
                            }
                        }
                        ExposedDropdownMenu(
                            expanded = modeExpanded, 
                            onDismissRequest = { modeExpanded = false }, 
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface).widthIn(min = 140.dp)
                        ) {
                            paymentModes.forEach { name ->
                                DropdownMenuItem(
                                    text = { Text(name, fontSize = 14.sp, maxLines = 1, softWrap = false, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        modeText = name; modeExpanded = false
                                        selectedSourceId = ""; selectedSourceName = ""; selectedSourceLogo = null 
                                        if (name == "Cash") { 
                                            selectedSourceType = "Cash"
                                            selectedSourceId = "Cash"
                                            selectedSourceName = "Cash in Hand" 
                                        } else if (name == "Credit Card") { 
                                            selectedSourceType = "Credit Card" 
                                        } else { 
                                            selectedSourceType = "Bank" 
                                        }
                                    }
                                )
                            }
                        }
                    }

                    val isPaidByActive = selectedSourceType.isNotEmpty() && selectedSourceType != "Cash"

                    ExposedDropdownMenuBox(
                        expanded = paidByExpanded && isPaidByActive, 
                        onExpandedChange = { if(isPaidByActive) paidByExpanded = !paidByExpanded }, 
                        modifier = Modifier.weight(0.65f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .border(1.dp, if(paidByExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                .menuAnchor()
                                .background(
                                    if (!isPaidByActive && selectedSourceType != "Cash") MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.Transparent, 
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), 
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (selectedSourceType.isEmpty()) { 
                                    Text("Select Mode", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.weight(1f)) 
                                } else if (selectedSourceId.isEmpty()) { 
                                    Text(
                                        text = if(selectedSourceType == "Bank") "Choose Bank" else "Choose Card", 
                                        color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                        fontSize = 14.sp, 
                                        modifier = Modifier.weight(1f)
                                    ) 
                                } else {
                                    if (selectedSourceLogo != null && selectedSourceType != "Cash") {
                                        Image(
                                            painter = painterResource(id = selectedSourceLogo!!), 
                                            contentDescription = null, 
                                            modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)), 
                                            contentScale = ContentScale.Fit
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = selectedSourceName, 
                                        color = if(selectedSourceType == "Cash") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, 
                                        fontSize = 14.sp, 
                                        fontWeight = FontWeight.Bold, 
                                        maxLines = 1, 
                                        softWrap = false, 
                                        overflow = TextOverflow.Ellipsis, 
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (isPaidByActive) { 
                                    Icon(
                                        imageVector = Icons.Outlined.ArrowDropDown, 
                                        contentDescription = null, 
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                                        modifier = Modifier.size(20.dp).rotate(if (paidByExpanded) 180f else 0f)
                                    ) 
                                }
                            }
                        }

                        ExposedDropdownMenu(
                            expanded = paidByExpanded && isPaidByActive, 
                            onDismissRequest = { paidByExpanded = false }, 
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            if (selectedSourceType == "Bank") {
                                if (bankList.isEmpty()) { 
                                    DropdownMenuItem(text = { Text("No Banks", color = MaterialTheme.colorScheme.onSurfaceVariant) }, onClick = {}) 
                                } 
                                bankList.forEach { bank ->
                                    DropdownMenuItem(
                                        text = { 
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                val logo = Constants.BankLogoMap[bank.bankName]
                                                if (logo != null) {
                                                    Image(painterResource(logo), null, modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp))) 
                                                } else {
                                                    Icon(Icons.Outlined.AccountBalance, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = "• ${if (bank.accountNo.length >= 4) bank.accountNo.takeLast(4) else bank.accountNo}", 
                                                    maxLines = 1, 
                                                    softWrap = false, 
                                                    overflow = TextOverflow.Ellipsis, 
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        },
                                        onClick = { 
                                            selectedSourceId = bank.accountNo
                                            selectedSourceName = "• ${if (bank.accountNo.length >= 4) bank.accountNo.takeLast(4) else bank.accountNo}"
                                            selectedSourceLogo = Constants.BankLogoMap[bank.bankName]
                                            paidByExpanded = false
                                        }
                                    )
                                }
                            } else if (selectedSourceType == "Credit Card") {
                                if (ccList.isEmpty()) { 
                                    DropdownMenuItem(text = { Text("No Cards", color = MaterialTheme.colorScheme.onSurfaceVariant) }, onClick = {}) 
                                } 
                                ccList.forEach { cc ->
                                    DropdownMenuItem(
                                        text = { 
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                val logo = Constants.BankLogoMap[cc.issuer]
                                                if (logo != null) {
                                                    Image(painterResource(logo), null, modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp))) 
                                                } else {
                                                    Icon(Icons.Outlined.CreditCard, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = "• ${if (cc.cardNo.length >= 4) cc.cardNo.takeLast(4) else cc.cardNo}", 
                                                    maxLines = 1, 
                                                    softWrap = false, 
                                                    overflow = TextOverflow.Ellipsis, 
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        },
                                        onClick = { 
                                            selectedSourceId = cc.cardNo
                                            selectedSourceName = "• ${if (cc.cardNo.length >= 4) cc.cardNo.takeLast(4) else cc.cardNo}"
                                            selectedSourceLogo = Constants.BankLogoMap[cc.issuer]
                                            paidByExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CustomDatePicker(
                        label = "Date", 
                        selectedDateMillis = expenseDateMillis, 
                        onDateSelected = { expenseDateMillis = it }, 
                        restrictToCurrentMonth = false, 
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = amount, 
                        onValueChange = { amount = it }, 
                        label = { Text("Amount") }, 
                        prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                        modifier = Modifier.weight(1f), 
                        shape = RoundedCornerShape(12.dp), 
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary, 
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { onDismiss() }, 
                        modifier = Modifier.weight(1f).height(50.dp), 
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) { 
                        Text("Cancel", fontWeight = FontWeight.Bold) 
                    }
                    Button(
                        onClick = {
                            val finalCategory = if(isCustomCategory) customCategoryText.trim() else categoryText.trim()
                            
                            if (amount.isNotBlank() && finalCategory.isNotBlank() && modeText.isNotBlank()) {
                                if (selectedSourceType.isNotEmpty() && selectedSourceId.isEmpty()) {
                                    Toast.makeText(context, "Select exact account/card!", Toast.LENGTH_SHORT).show(); return@Button
                                }
                                
                                onDismiss()
                                Toast.makeText(context, "Updating expense...", Toast.LENGTH_SHORT).show()
                                
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val db = FirebaseFirestore.getInstance()
                                        val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                                        if (!userQuery.isEmpty) {
                                            val userRef = userQuery.documents[0].reference
                                            
                                            val expDateOnly = expense.date.split(" ")[0]
                                            val oldTargetDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(expDateOnly) ?: Date()
                                            val oldDocId = SimpleDateFormat("yyyy_MM", Locale.getDefault()).format(oldTargetDate)
                                            val newDocId = SimpleDateFormat("yyyy_MM", Locale.getDefault()).format(Date(expenseDateMillis))
                                            
                                            val newAmt = amount.toDoubleOrNull() ?: expense.amount
                                            val paymentDetailStr = "$modeText | $selectedSourceType | $selectedSourceId"
                                            val diff = newAmt - expense.amount

                                            val oldDocRef = userRef.collection("Expenses").document(oldDocId)
                                            val oldDocSnap = oldDocRef.get().await()
                                            var targetKey: String? = null
                                            
                                            if (oldDocSnap.exists()) {
                                                val dataMap = oldDocSnap.data ?: emptyMap()
                                                for ((key, value) in dataMap) {
                                                    if (value is Map<*, *>) {
                                                        val dbDate = value["date"]
                                                        val dbDateStr = when (dbDate) {
                                                            is Timestamp -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dbDate.toDate())
                                                            is String -> dbDate.toString().split(" ")[0]
                                                            else -> ""
                                                        }
                                                        val dbAmount = (value["amount"] as? Number)?.toDouble() ?: 0.0
                                                        val dbCategory = value["category"]?.toString() ?: ""
                                                        
                                                        val amountMatch = Math.abs(dbAmount - expense.amount) < 0.01

                                                        if (dbDateStr == expDateOnly && amountMatch && dbCategory == expense.category) {
                                                            targetKey = key
                                                            break
                                                        }
                                                    }
                                                }
                                            }

                                            if (targetKey == null) {
                                                withContext(Dispatchers.Main) { Toast.makeText(context, "Error: Record not found in database.", Toast.LENGTH_LONG).show() }
                                                return@launch 
                                            }

                                            val expData = hashMapOf<String, Any>(
                                                "date" to Timestamp(Date(expenseDateMillis)),
                                                "amount" to newAmt,
                                                "category" to finalCategory,
                                                "detail_1" to remark1,
                                                "detail_2" to remark2,
                                                "payment_detail" to paymentDetailStr
                                            )

                                            if (oldDocId == newDocId) {
                                                val seqNumber = targetKey.substringBefore("_")
                                                val newKey = "${seqNumber}_${finalCategory}"
                                                
                                                val updates = hashMapOf<String, Any>()
                                                if (newKey != targetKey) {
                                                    updates[targetKey] = FieldValue.delete()
                                                }
                                                updates[newKey] = expData
                                                if (diff != 0.0) {
                                                    updates["000_total"] = FieldValue.increment(diff)
                                                }
                                                oldDocRef.update(updates).await()
                                            } else {
                                                oldDocRef.update(
                                                    mapOf(
                                                        targetKey to FieldValue.delete(),
                                                        "000_total" to FieldValue.increment(-expense.amount)
                                                    )
                                                ).await()
                                                
                                                val newDocRef = userRef.collection("Expenses").document(newDocId)
                                                val newDocSnap = newDocRef.get().await()
                                                var nextSeq = 1
                                                
                                                if (newDocSnap.exists()) {
                                                    val dataMap = newDocSnap.data ?: emptyMap()
                                                    val seqKeys = dataMap.keys.filter { it.matches(Regex("^\\d{3}_.*")) }
                                                    if (seqKeys.isNotEmpty()) {
                                                        val maxSeq = seqKeys.maxOf { it.substring(0, 3).toInt() }
                                                        nextSeq = maxSeq + 1
                                                    }
                                                }
                                                val formattedSeq = String.format(Locale.US, "%03d", nextSeq)
                                                val newKey = "${formattedSeq}_${finalCategory}"
                                                
                                                newDocRef.set(
                                                    mapOf(
                                                        newKey to expData,
                                                        "000_total" to FieldValue.increment(newAmt)
                                                    ), SetOptions.merge()
                                                ).await()
                                            }

                                            if (diff != 0.0) {
                                                when (selectedSourceType) {
                                                    "Cash" -> {
                                                        val bankDoc = userRef.collection("Finances").document("Bank").get().await()
                                                        var cur = 0.0
                                                        if (bankDoc.exists()) {
                                                            val cashMap = bankDoc.get("cash") as? Map<*, *>
                                                            cur = (cashMap?.get("amnt") as? Number)?.toDouble() ?: 0.0
                                                        }
                                                        userRef.collection("Finances").document("Bank").update("cash.amnt", cur - diff).await()
                                                    }
                                                    "Bank" -> {
                                                        if (selectedSourceId.isNotEmpty()) {
                                                            val bankDoc = userRef.collection("Finances").document("Bank").get().await()
                                                            val bankData = bankDoc.get(selectedSourceId) as? Map<*, *>
                                                            if (bankData != null) {
                                                                val curBal = (bankData["current bal."] as? Number)?.toDouble() ?: 0.0
                                                                userRef.collection("Finances").document("Bank").update("${selectedSourceId}.current bal.", curBal - diff).await()
                                                            }
                                                        }
                                                    }
                                                    "Credit Card" -> {
                                                        if (selectedSourceId.isNotEmpty()) {
                                                            val ccDoc = userRef.collection("Finances").document("CC FD").get().await()
                                                            val ccMap = ccDoc.get("CC") as? Map<*, *>
                                                            val ccData = ccMap?.get(selectedSourceId) as? Map<*, *>
                                                            if (ccData != null) {
                                                                val curOut = (ccData["outstanding"] as? Number)?.toDouble() ?: 0.0
                                                                val newOut = (curOut + diff).coerceAtLeast(0.0)
                                                                userRef.collection("Finances").document("CC FD").update("CC.${selectedSourceId}.outstanding", newOut).await()
                                                            }
                                                        }
                                                    }
                                                }
                                                updateBudgetUsage(userRef, diff)
                                            }
                                            withContext(Dispatchers.Main) { onSuccess() }
                                        } else {
                                            withContext(Dispatchers.Main) { Toast.makeText(context, "Update Failed!", Toast.LENGTH_SHORT).show() }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) { Toast.makeText(context, "Update Failed!", Toast.LENGTH_SHORT).show() }
                                    }
                                }
                            } else { 
                                Toast.makeText(context, "Fill required fields!", Toast.LENGTH_SHORT).show() 
                            }
                        }, 
                        modifier = Modifier.weight(1f).height(50.dp), 
                        shape = RoundedCornerShape(12.dp), 
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { 
                        Text("Save", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) 
                    }
                }
            }
        }
    }
}
