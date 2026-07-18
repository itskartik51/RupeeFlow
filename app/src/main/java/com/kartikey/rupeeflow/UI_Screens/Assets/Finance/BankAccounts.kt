package com.kartikey.rupeeflow.UI_Screens.Assets.Finance

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kartikey.rupeeflow.Cloud_Database.Constants
import com.kartikey.rupeeflow.UI_Screens.Assets.BankAccountItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankAccountsScreen(
    onBackClick: () -> Unit,
    username: String, 
    bankList: List<BankAccountItem>,
    isLoading: Boolean,
    onRefreshClick: () -> Unit,
    onEditBankClick: (BankAccountItem) -> Unit 
) {
    val infiniteTransition = rememberInfiniteTransition(label = "refresh")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "spin"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Linked Banks", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                actions = { IconButton(onClick = onRefreshClick) { Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", modifier = Modifier.rotate(if (isLoading) angle else 0f)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8F9FA))
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        if (bankList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No Bank Accounts Added Yet", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp), 
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(bankList) { bank ->
                    BankDetailCard(bank = bank, username = username, onEditClick = onEditBankClick, onRefreshRequest = onRefreshClick)
                }
            }
        }
    }
}

@Composable
fun BankDetailCard(bank: BankAccountItem, username: String, onEditClick: (BankAccountItem) -> Unit, onRefreshRequest: () -> Unit) {
    var showQuickUpdate by remember { mutableStateOf(false) }
    val logoRes = Constants.BankLogoMap[bank.bankName]
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF1976D2).copy(alpha = 0.05f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (logoRes != null) {
                        Image(
                            painter = painterResource(id = logoRes),
                            contentDescription = bank.bankName,
                            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalance, 
                            contentDescription = "Bank Fallback", 
                            tint = Color(0xFF1976D2), 
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = bank.bankName.uppercase(), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                    Text(text = bank.accountNo, color = Color.Gray, fontSize = 12.sp, letterSpacing = 1.sp)
                }
                
                IconButton(onClick = { onEditClick(bank) }) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit Bank", tint = Color.Gray, modifier = Modifier.size(22.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(text = "Available Balance", color = Color.Gray, fontSize = 12.sp)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = formatRupeeAmount(bank.currentBalance), fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = Color.Black)
                
                IconButton(
                    onClick = { showQuickUpdate = true },
                    modifier = Modifier.size(32.dp).background(Color(0xFFE8F5E9), CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Update Balance", tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MetricItem(label = "Interest Rate", value = "${bank.interestRate}% Yr", valueColor = Color.DarkGray, alignment = Alignment.Start)
                    MetricItem(label = "Exp. Qtr", value = "+${formatRupeeAmount(bank.expQtrInt)}", valueColor = Color(0xFFF57C00), alignment = Alignment.CenterHorizontally)
                    MetricItem(label = "Exp. Yearly", value = "+${formatRupeeAmount(bank.expYrInt)}", valueColor = Color(0xFF1976D2), alignment = Alignment.End)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MetricItem(label = "1-Day Earn", value = "+${formatRupeeAmount(bank.oneDayInt)}", valueColor = Color(0xFF388E3C), alignment = Alignment.Start)
                    MetricItem(label = "Accrued Qtr", value = "+${formatRupeeAmount(bank.accruedQtrInt)}", valueColor = Color(0xFF388E3C), alignment = Alignment.CenterHorizontally)
                    MetricItem(label = "Accrued Yr", value = "+${formatRupeeAmount(bank.accruedYrInt)}", valueColor = Color(0xFF388E3C), alignment = Alignment.End)
                }
            }
        }
    }
    
    if (showQuickUpdate) {
        QuickUpdateDialog(
            bank = bank,
            username = username,
            onDismiss = { showQuickUpdate = false },
            onSuccess = {
                showQuickUpdate = false
                onRefreshRequest()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickUpdateDialog(bank: BankAccountItem, username: String, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var updateAmount by remember { mutableStateOf("") }
    var isUpdating by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    Dialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f).imePadding(), 
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Update Balance", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black)
                Text("Add or deduct amount from ${bank.bankName}", color = Color.Gray, fontSize = 13.sp)
                
                Spacer(modifier = Modifier.height(20.dp))
                
                OutlinedTextField(
                    value = updateAmount,
                    onValueChange = { updateAmount = it },
                    label = { Text("Amount (+ or -)") },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = Color.Black) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { if (!isUpdating) onDismiss() },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = {
                            val amountEntered = updateAmount.toDoubleOrNull()
                            if (amountEntered != null && amountEntered != 0.0) {
                                isUpdating = true
                                val newCalculatedBalance = bank.currentBalance + amountEntered 
                                
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val jsonBody = JSONObject().apply {
                                            put("action", "edit_bank")
                                            put("username", username)
                                            put("original_account_no", bank.accountNo)
                                            put("new_account_no", bank.accountNo)
                                            put("new_bank_name", bank.bankName)
                                            put("new_interest_rate", bank.interestRate)
                                            put("new_current_bal", newCalculatedBalance)
                                        }
                                        
                                        val client = OkHttpClient()
                                        val body = jsonBody.toString().toRequestBody("application/json".toMediaType())
                                        val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(body).build()
                                        val response = client.newCall(request).execute()
                                        val resData = response.body?.string() ?: ""

                                        withContext(Dispatchers.Main) {
                                            isUpdating = false
                                            if (resData.contains("success")) {
                                                Toast.makeText(context, "Balance Updated Successfully!", Toast.LENGTH_SHORT).show()
                                                onSuccess()
                                            } else {
                                                Toast.makeText(context, "Update Failed!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isUpdating = false
                                            Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                        enabled = !isUpdating
                    ) {
                        if (isUpdating) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                        else Text("Update", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, valueColor: Color, alignment: Alignment.Horizontal) {
    Column(horizontalAlignment = alignment) {
        Text(text = label, color = Color.Gray, fontSize = 11.sp)
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = valueColor)
    }
}

fun formatRupeeAmount(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.maximumFractionDigits = 2
    return format.format(amount).replace("-₹", "-₹ ")
}
