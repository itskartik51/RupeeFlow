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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.Cloud_Database.Constants
import com.kartikey.rupeeflow.UI_Screens.Assets.BankAccountItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.CashItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.CreditCardItem
import com.kartikey.rupeeflow.UI_Screens.CustomDatePicker
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

data class TransactionModel(
    val date: String,
    val amount: Double,
    val category: String,
    val remark1: String,
    val remark2: String,
    val mode: String = "" 
)

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
    
    var expenseDateMillis by remember { mutableStateOf<Long>(System.currentTimeMillis()) }
    
    var expanded by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var isPressed by remember { mutableStateOf(false) }
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f, 
        label = "ButtonScale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(), 
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            
            // 1. Category
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
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    categories.forEach { (name, icon) ->
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = icon, 
                                        contentDescription = name, 
                                        tint = Color(0xFF2E7D32), 
                                        modifier = Modifier.size(20.dp)
                                    )
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

            // 2. Remarks
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = remark1, 
                    onValueChange = { remark1 = it },
                    label = { Text("Remark 1") },
                    modifier = Modifier.weight(1f), 
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = remark2, 
                    onValueChange = { remark2 = it },
                    label = { Text("Remark 2") },
                    modifier = Modifier.weight(1f), 
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. SMART MODE & PAID BY ROW
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // --- MODE DROPDOWN (35% weight) ---
                ExposedDropdownMenuBox(
                    expanded = modeExpanded,
                    onExpandedChange = { modeExpanded = !modeExpanded },
                    modifier = Modifier.weight(0.35f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .border(1.dp, if (modeExpanded) Color(0xFF2E7D32) else Color.Gray, RoundedCornerShape(12.dp))
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
                                color = if (modeText.isEmpty()) Color.Gray else Color.Black,
                                fontSize = 14.sp,
                                maxLines = 1,
                                softWrap = false, // Stop vertical stacking of letters
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f) // Push icon to the end
                            )
                            Icon(
                                Icons.Outlined.ArrowDropDown, 
                                contentDescription = null, 
                                tint = Color.Gray, 
                                modifier = Modifier.size(20.dp).rotate(if (modeExpanded) 180f else 0f)
                            )
                        }
                    }
                    
                    ExposedDropdownMenu(
                        expanded = modeExpanded,
                        onDismissRequest = { modeExpanded = false },
                        modifier = Modifier.background(Color.White).widthIn(min = 140.dp) // Minimum width so options don't squish
                    ) {
                        paymentModes.forEach { (name, icon) ->
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = icon, 
                                            contentDescription = name, 
                                            tint = Color.DarkGray, 
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(name, fontSize = 14.sp, maxLines = 1, softWrap = false)
                                    }
                                },
                                onClick = {
                                    modeText = name
                                    modeExpanded = false
                                    selectedSourceId = ""
                                    selectedSourceName = ""
                                    selectedSourceLogo = null 
                                    
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

                // --- PAID BY DROPDOWN (65% weight) ---
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
                            .background(
                                if (!isPaidByActive && selectedSourceType != "Cash") Color(0xFFF5F5F5) else Color.Transparent, 
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), 
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedSourceType.isEmpty()) {
                                Text("Select Mode", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.weight(1f)) // Push arrow to end
                                Icon(Icons.Outlined.ArrowDropDown, null, tint = Color.Transparent, modifier = Modifier.size(20.dp)) // Hidden placeholder
                            } else if (selectedSourceId.isEmpty()) {
                                Text(
                                    if(selectedSourceType == "Bank") "Choose Bank" else "Choose Card", 
                                    color = Color.Gray, 
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f) // Push arrow to end
                                )
                                Icon(Icons.Outlined.ArrowDropDown, null, tint = Color.Gray, modifier = Modifier.size(20.dp).rotate(if (paidByExpanded) 180f else 0f))
                            } else {
                                // If selected, show Logo + • XXXX format
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
                                    color = if(selectedSourceType == "Cash") Color(0xFF2E7D32) else Color.Black, 
                                    fontSize = 14.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    maxLines = 1, 
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis, 
                                    modifier = Modifier.weight(1f) // Push arrow to end
                                )
                                
                                if (isPaidByActive) {
                                    Icon(Icons.Outlined.ArrowDropDown, null, tint = Color.Gray, modifier = Modifier.size(20.dp).rotate(if (paidByExpanded) 180f else 0f))
                                }
                            }
                        }
                    }

                    ExposedDropdownMenu(
                        expanded = paidByExpanded && isPaidByActive,
                        onDismissRequest = { paidByExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        if (selectedSourceType == "Bank") {
                            if (bankList.isEmpty()) { 
                                DropdownMenuItem(
                                    text = { Text("No Banks Linked", color = Color.Gray) }, 
                                    onClick = {}
                                ) 
                            }
                            bankList.forEach { bank ->
                                DropdownMenuItem(
                                    text = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val logo = Constants.BankLogoMap[bank.bankName]
                                            if (logo != null) {
                                                Image(
                                                    painter = painterResource(logo), 
                                                    contentDescription = null, 
                                                    modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp))
                                                )
                                            } else {
                                                Icon(Icons.Outlined.AccountBalance, null, tint = Color.DarkGray, modifier = Modifier.size(24.dp))
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            val shortAcc = if (bank.accountNo.length >= 4) bank.accountNo.takeLast(4) else bank.accountNo
                                            // Show ONLY • 1234
                                            Text("• $shortAcc", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    onClick = { 
                                        val shortAcc = if (bank.accountNo.length >= 4) bank.accountNo.takeLast(4) else bank.accountNo
                                        selectedSourceId = bank.accountNo
                                        selectedSourceName = "• $shortAcc" // Setting it strictly to Logo + 4 digits
                                        selectedSourceLogo = Constants.BankLogoMap[bank.bankName]
                                        paidByExpanded = false
                                    }
                                )
                            }
                        } else if (selectedSourceType == "Credit Card") {
                            if (ccList.isEmpty()) { 
                                DropdownMenuItem(
                                    text = { Text("No Cards Linked", color = Color.Gray) }, 
                                    onClick = {}
                                ) 
                            }
                            ccList.forEach { cc ->
                                DropdownMenuItem(
                                    text = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val logo = Constants.BankLogoMap[cc.issuer]
                                            if (logo != null) {
                                                Image(
                                                    painter = painterResource(logo), 
                                                    contentDescription = null, 
                                                    modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp))
                                                )
                                            } else {
                                                Icon(Icons.Outlined.CreditCard, null, tint = Color.DarkGray, modifier = Modifier.size(24.dp))
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            val shortAcc = if (cc.cardNo.length >= 4) cc.cardNo.takeLast(4) else cc.cardNo
                                            // Show ONLY • 1234
                                            Text("• $shortAcc", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    onClick = { 
                                        val shortAcc = if (cc.cardNo.length >= 4) cc.cardNo.takeLast(4) else cc.cardNo
                                        selectedSourceId = cc.cardNo
                                        selectedSourceName = "• $shortAcc" // Setting it strictly to Logo + 4 digits
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

            // 4. Custom Date Picker & Amount
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CustomDatePicker(
                    label = "Date",
                    selectedDateMillis = expenseDateMillis,
                    onDateSelected = { expenseDateMillis = it },
                    restrictToCurrentMonth = true, // Locks date selection to current and previous month
                    modifier = Modifier.weight(1f)
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

            // 5. Submit Engine
            Button(
                onClick = {
                    val finalCategory = categoryText.trim()
                    val finalMode = modeText.trim()
                    val finalExpenseDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(expenseDateMillis))

                    if (amount.isNotBlank() && finalCategory.isNotBlank() && finalMode.isNotBlank()) {
                        
                        if (selectedSourceType.isNotEmpty() && selectedSourceId.isEmpty()) {
                            Toast.makeText(context, "Please select exact Paid By account/card", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isSubmitting = true
                        val expenseAmt = amount.toDoubleOrNull() ?: 0.0
                        val newEntry = TransactionModel(
                            date = finalExpenseDateStr, 
                            amount = expenseAmt, 
                            category = finalCategory, 
                            remark1 = remark1, 
                            remark2 = remark2, 
                            mode = finalMode
                        )
                        onExpenseAdded(newEntry)

                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val json = JSONObject().apply {
                                    put("action", "add_expense")
                                    put("username", username)
                                    put("amount", amount)
                                    put("category", finalCategory)
                                    put("date", finalExpenseDateStr) 
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
                                    amount = ""
                                    remark1 = ""
                                    remark2 = ""
                                    categoryText = ""
                                    modeText = ""
                                    selectedSourceId = ""
                                    selectedSourceName = ""
                                    selectedSourceType = ""
                                    selectedSourceLogo = null
                                    isCategoryEditable = false 
                                    expenseDateMillis = System.currentTimeMillis() 
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
                        Toast.makeText(context, "Please fill Amount, Category & Mode", Toast.LENGTH_SHORT).show()
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
