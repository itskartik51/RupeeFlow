package com.kartikey.rupeeflow.UI_Screens.Assets.Finance

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Money
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kartikey.rupeeflow.Cloud_Database.Constants
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CashItem(
    val amount: Double,
    val lastUpdated: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashScreen(
    onBackClick: () -> Unit,
    username: String,
    cashData: CashItem,
    onRefreshRequest: () -> Unit
) {
    var showUpdateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Physical Cash", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.bounceClick()) { 
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface) 
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(60.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Money, contentDescription = "Cash", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Total Cash in Hand", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }
                    Text(text = format.format(cashData.amount), fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    val lastUpd = if (cashData.lastUpdated.isBlank()) "Never" else cashData.lastUpdated
                    Text(text = "Last Verified: $lastUpd", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showUpdateDialog = true },
                        modifier = Modifier.fillMaxWidth().height(50.dp).bounceClick(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Update Cash Balance", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showUpdateDialog) {
        UpdateCashDialog(
            currentAmount = cashData.amount,
            username = username,
            onDismiss = { showUpdateDialog = false },
            onSuccess = {
                showUpdateDialog = false
                onRefreshRequest()
            }
        )
    }
}

@Composable
fun UpdateCashDialog(currentAmount: Double, username: String, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var updateAmount by remember { mutableStateOf(if (currentAmount > 0) currentAmount.toInt().toString() else "") }
    val context = LocalContext.current
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f).imePadding(), 
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Verify Cash Balance", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("Enter your exact current cash in hand", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                
                Spacer(modifier = Modifier.height(20.dp))
                
                OutlinedTextField(
                    value = updateAmount,
                    onValueChange = { updateAmount = it },
                    label = { Text("Exact Amount") },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary, 
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(50.dp).bounceClick(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = {
                            val amt = updateAmount.toDoubleOrNull()
                            if (amt != null && amt >= 0.0) {
                                
                                onDismiss()
                                Toast.makeText(context, "Verifying cash...", Toast.LENGTH_SHORT).show()
                                
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val db = FirebaseFirestore.getInstance()
                                        val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                                        
                                        if (!userQuery.isEmpty) {
                                            val userRef = userQuery.documents[0].reference
                                            val cashData = hashMapOf(
                                                "total_cash" to amt,
                                                "last_updated" to SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                                            )
                                            userRef.collection("Finances").document("Cash")
                                                .set(cashData, SetOptions.merge())
                                                .await()

                                            withContext(Dispatchers.Main) {
                                                onSuccess() 
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Update Failed!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).height(50.dp).bounceClick(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Verify", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}
