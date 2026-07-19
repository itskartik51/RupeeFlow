package com.kartikey.rupeeflow.UI_Screens.Add

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.Cloud_Database.Constants
import com.kartikey.rupeeflow.UI_Screens.Assets.BankAccountItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.CashItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.CreditCardItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseForm(
    username: String,
    bankList: List<BankAccountItem>,
    ccList: List<CreditCardItem>,
    cashData: CashItem?,
    onExpenseAdded: (TransactionModel) -> Unit,
    onDismiss: () -> Unit
) {
    val categories = listOf(
        "Food" to Icons.Outlined.Restaurant,
        "Transport" to Icons.Outlined.DirectionsCar,
        "Shopping" to Icons.Outlined.ShoppingBag,
        "Bills" to Icons.Outlined.Receipt,
        "Custom" to Icons.Outlined.Edit
    )
    val paymentModes = listOf(
        "Cash" to Icons.Outlined.Payments,
        "UPI" to Icons.Outlined.QrCodeScanner,
        "NEFT" to Icons.Outlined.AccountBalance,
        "Credit Card" to Icons.Outlined.CreditCard,
        "Debit Card" to Icons.Outlined.CreditCard,
        "Net Banking" to Icons.Outlined.Computer
    )
    
    var categoryText by remember { mutableStateOf("") }
    var isCategoryEditable by remember { mutableStateOf(false) } 
    var remark1 by remember { mutableStateOf("") }
    var remark2 by remember { mutableStateOf("") }
    var modeText by remember { mutableStateOf("") }
    var modeExpanded by remember { mutableStateOf(false) }

    // Auto-Deduct States
    var paidByExpanded by remember { mutableStateOf(false) }
    var selectedSourceType by remember { mutableStateOf("") }
    var selectedSourceId by remember { mutableStateOf("") }
    var selectedSourceName by remember { mutableStateOf("") }
    var selectedSourceLogo by remember { mutableStateOf<Int?>(null) }
    
    var amount by remember { mutableStateOf("") }
    
    val todayDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    var expenseDate by remember { mutableStateOf(todayDate) }
    
    var expanded by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var isPressed by remember { mutableStateOf(false) }
    val buttonScale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "ButtonScale")

    Card(
        modifier = Modifier.fillMaxWidth(), 
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = categoryText,
                    onValueChange = { categoryText = it },
                    readOnly = !isCategoryEditable,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { (name, icon) ->
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = icon, contentDescription = name, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(name, fontSize = 16.sp)
                                }
                            },
                            onClick = {
                                if (name == "Custom") {
                                    categoryText = "" 
                                    isCategoryEditable = true 
                                } else {
                                    categoryText = name
                                    isCategoryEditable = false 
                                }
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = remark1, onValueChange = { remark1 = it },
                    label = { Text("Remark 1") },
                    modifier = Modifier.weight(1f), singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = remark2, onValueChange = { remark2 = it },
                    label = { Text("Remark 2") },
                    modifier = Modifier.weight(1f), singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SMART MODE & PAID BY ROW (35% & 65%)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Mode Dropdown (35%)
                ExposedDropdownMenuBox(
                    expanded = modeExpanded,
                    onExpandedChange = { modeExpanded = !modeExpanded },
                    modifier = Modifier.weight(0.35f)
                ) {
                    OutlinedTextField(
                        value = modeText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Mode") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 14.sp)
                    )
                    ExposedDropdownMenu(
                        expanded = modeExpanded,
                        onDismissRequest = { modeExpanded = false }
                    ) {
                        paymentModes.forEach { (name, icon) ->
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = icon, contentDescription = name, tint = Color.DarkGray, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(name, fontSize = 14.sp)
                                    }
                                },
                                onClick = {
                                    modeText = name
                                    modeExpanded = false
                                    selectedSourceId = ""
                                    selectedSourceName = ""
                                    selectedSourceLogo = null // Reset
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

                // Paid By Dropdown (65%)
                val modeIcon = paymentModes.find { it.first == modeText }?.second ?: Icons.Outlined.Payments
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
                            .border(1.dp, if(paidByExpanded) Color(0xFF2E7D32) else Color.Gray, RoundedCornerShape(12.dp))
                            .menuAnchor()
                            .background(if (!isPaidByActive && selectedSourceType != "Cash") Color(0xFFF5F5F5) else Color.Transparent, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (selectedSourceType.isEmpty()) {
                                Text("Select Mode", color = Color.Gray, fontSize = 14.sp)
                            } else if (selectedSourceId.isEmpty()) {
                                Text(if(selectedSourceType == "Bank") "Choose Bank" else "Choose Card", color = Color.Gray, fontSize = 14.sp)
                            } else {
                                Icon(modeIcon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                if (selectedSourceLogo != null) {
                                    Image(painterResource(id = selectedSourceLogo!!), contentDescription = null, modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Fit)
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else if (selectedSourceType != "Cash") {
                                    Icon(Icons.Outlined.AccountBalance, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(selectedSourceName, color = if(selectedSourceType == "Cash") Color(0xFF2E7D32) else Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            }
                            if (isPaidByActive) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Outlined.ArrowDropDown, null, tint = Color.Gray, modifier = Modifier.rotate(if (paidByExpanded) 180f else 0f))
                            }
                        }
                    }

                    ExposedDropdownMenu(
                        expanded = paidByExpanded && isPaidByActive,
                        onDismissRequest = { paidByExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        if (selectedSourceType == "Bank") {
                            if (bankList.isEmpty()) { DropdownMenuItem(text = { Text("No Banks Linked", color = Color.Gray) }, onClick = {}) }
                            bankList.forEach { bank ->
                                DropdownMenuItem(
                                    text = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val logo = Constants.BankLogoMap[bank.bankName]
                                            if(logo != null) Image(painterResource(logo), null, modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            val shortAcc = if(bank.accountNo.length >= 4) bank.accountNo.takeLast(4) else bank.accountNo
                                            Text("${bank.bankName} • $shortAcc", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    },
                                    onClick = { 
                                        val shortAcc = if(bank.accountNo.length >= 4) bank.accountNo.takeLast(4) else bank.accountNo
                                        selectedSourceId = bank.accountNo
                                        selectedSourceName = "${bank.bankName} • $shortAcc"
                                        selectedSourceLogo = Constants.BankLogoMap[bank.bankName]
                                        paidByExpanded = false
                                    }
                                )
                            }
                        } else if (selectedSourceType == "Credit Card") {
                            if (ccList.isEmpty()) { DropdownMenuItem(text = { Text("No Cards Linked", color = Color.Gray) }, onClick = {}) }
                            ccList.forEach { cc ->
                                DropdownMenuItem(
                                    text = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val logo = Constants.BankLogoMap[cc.issuer]
                                            if(logo != null) Image(painterResource(logo), null, modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            val shortAcc = if(cc.cardNo.length >= 4) cc.cardNo.takeLast(4) else cc.cardNo
                                            Text("${cc.issuer} • $shortAcc", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    },
                                    onClick = { 
                                        val shortAcc = if(cc.cardNo.length >= 4) cc.cardNo.takeLast(4) else cc.cardNo
                                        selectedSourceId = cc.cardNo
                                        selectedSourceName = "${cc.issuer} • $shortAcc"
                                        selectedSourceLogo = Constants.BankLogoMap[cc.issuer]
                                        paidByExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = expenseDate, 
                    onValueChange = { expenseDate = it },
                    label = { Text("Date (DD/MM/YYYY)") },
                    modifier = Modifier.weight(1f), 
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = amount, 
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = Color.Black) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), 
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val finalCategory = categoryText.trim()
                    val finalMode = modeText.trim()

                    if (amount.isNotBlank() && finalCategory.isNotBlank() && expenseDate.isNotBlank() && finalMode.isNotBlank()) {
                        
                        if (selectedSourceType.isNotEmpty() && selectedSourceId.isEmpty()) {
                            Toast.makeText(context, "Please select exact Paid By account/card", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isSubmitting = true
                        val expenseAmt = amount.toDoubleOrNull() ?: 0.0
                        val newEntry = TransactionModel(expenseDate, expenseAmt, finalCategory, remark1, remark2, finalMode)
                        onExpenseAdded(newEntry)

                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val json = JSONObject().apply {
                                    put("action", "add_expense")
                                    put("username", username)
                                    put("amount", amount)
                                    put("category", finalCategory)
                                    put("date", expenseDate) 
                                    put("detail1", remark1)
                                    put("detail2", remark2)
                                    put("payment_method", finalMode) 
                                    put("source_type", selectedSourceType) 
                                    put("source_identifier", selectedSourceId) 
                                }
                                val client = OkHttpClient()
                                val body = json.toString().toRequestBody("application/json".toMediaType())
                                val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(body).build()
                                client.newCall(request).execute()

                                withContext(Dispatchers.Main) {
                                    isSubmitting = false
                                    Toast.makeText(context, "Saved & Balance Adjusted! ✅", Toast.LENGTH_LONG).show()
                                    amount = ""; remark1 = ""; remark2 = ""; categoryText = ""; modeText = ""
                                    selectedSourceId = ""; selectedSourceName = ""; selectedSourceType = ""; selectedSourceLogo = null
                                    isCategoryEditable = false 
                                    expenseDate = todayDate
                                    onDismiss() 
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isSubmitting = false
                                    Toast.makeText(context, "Failed to connect to sheet ❌", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(context, "Please fill Amount, Category, Mode & Date", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .scale(buttonScale)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                            }
                        )
                    },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(26.dp), strokeWidth = 3.dp)
                } else {
                    Text("Save Expense", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

data class TransactionModel(
    val date: String,
    val amount: Double,
    val category: String,
    val remark1: String,
    val remark2: String,
    val mode: String = "" 
)
