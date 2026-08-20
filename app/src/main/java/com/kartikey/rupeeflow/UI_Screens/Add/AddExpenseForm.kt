package com.kartikey.rupeeflow.UI_Screens.Add

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kartikey.rupeeflow.Cloud_Database.Constants
import com.kartikey.rupeeflow.UI_Screens.Assets.BankAccountItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.CashItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.CreditCardItem
import com.kartikey.rupeeflow.UI_Screens.CustomDatePicker
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TransactionModel(
    val date: String,
    val amount: Double,
    val category: String,
    val remark1: String,
    val remark2: String,
    val mode: String = "",
    val sourceType: String = "",
    val sourceId: String = ""
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
        "Groceries" to Icons.Outlined.LocalGroceryStore,
        "Transport" to Icons.Outlined.DirectionsCar,
        "Fuel" to Icons.Outlined.LocalGasStation,
        "Shopping" to Icons.Outlined.ShoppingBag,
        "Bills" to Icons.Outlined.Receipt,
        "Rent" to Icons.Outlined.Home,
        "EMI" to Icons.Outlined.AccountBalanceWallet,
        "Subscription" to Icons.Outlined.Subscriptions,
        "Gift" to Icons.Outlined.CardGiftcard,
        "Personal Care" to Icons.Outlined.Spa,
        "Health" to Icons.Outlined.MedicalServices,
        "Education" to Icons.Outlined.School,
        "Entertainment" to Icons.Outlined.SportsEsports,
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
    
    val hasBank = bankList.isNotEmpty()
    val hasCC = ccList.isNotEmpty()
    val hasCash = cashData != null && cashData.amount > 0.0
    val hasNoFinance = !hasBank && !hasCC && !hasCash

    var categoryText by remember { mutableStateOf("") }
    var isCategoryEditable by remember { mutableStateOf(false) } 
    var remark1 by remember { mutableStateOf("") }
    var remark2 by remember { mutableStateOf("") }
    
    var modeText by remember { mutableStateOf("") }
    var modeExpanded by remember { mutableStateOf(false) }

    var paidByExpanded by remember { mutableStateOf(false) }
    var selectedSourceType by remember { mutableStateOf("") }
    var selectedSourceId by remember { mutableStateOf("") }
    var selectedSourceName by remember { mutableStateOf("") }
    var selectedSourceLogo by remember { mutableStateOf<Int?>(null) }
    
    var amount by remember { mutableStateOf("") }
    var expenseDateMillis by remember { mutableStateOf<Long>(System.currentTimeMillis()) }
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(), 
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary, 
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    categories.forEach { (name, icon) ->
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = icon, 
                                        contentDescription = name, 
                                        tint = MaterialTheme.colorScheme.primary, 
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(name, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
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
                    value = remark1, 
                    onValueChange = { remark1 = it }, 
                    label = { Text("Remark 1") }, 
                    modifier = Modifier.weight(1f), 
                    singleLine = true, 
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary, 
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), 
                        focusedTextColor = MaterialTheme.colorScheme.onSurface, 
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                OutlinedTextField(
                    value = remark2, 
                    onValueChange = { remark2 = it }, 
                    label = { Text("Remark 2") }, 
                    modifier = Modifier.weight(1f), 
                    singleLine = true, 
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary, 
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), 
                        focusedTextColor = MaterialTheme.colorScheme.onSurface, 
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = modeExpanded, 
                    onExpandedChange = { 
                        if (hasNoFinance) {
                            Toast.makeText(context, "Add Finance Detail", Toast.LENGTH_SHORT).show()
                            modeExpanded = false
                        } else {
                            modeExpanded = !modeExpanded 
                        }
                    }, 
                    modifier = Modifier.weight(0.35f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .border(
                                1.dp, 
                                if (modeExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), 
                                RoundedCornerShape(12.dp)
                            )
                            .menuAnchor()
                            .background(
                                if (hasNoFinance) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else Color.Transparent, 
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp), 
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (modeText.isEmpty()) "Mode" else modeText,
                                color = if (modeText.isEmpty() || hasNoFinance) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
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
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(if (modeExpanded) 180f else 0f)
                            )
                        }
                    }
                    
                    ExposedDropdownMenu(
                        expanded = modeExpanded, 
                        onDismissRequest = { modeExpanded = false }, 
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .widthIn(min = 140.dp)
                    ) {
                        paymentModes.forEach { (name, icon) ->
                            val isAvailable = when (name) {
                                "Cash" -> hasCash
                                "Credit Card" -> hasCC
                                else -> hasBank
                            }
                            val itemColor = if (isAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = icon, 
                                            contentDescription = name, 
                                            tint = itemColor, 
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = name, 
                                            fontSize = 14.sp, 
                                            color = itemColor, 
                                            maxLines = 1, 
                                            softWrap = false
                                        )
                                    }
                                },
                                onClick = {
                                    if (isAvailable) {
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
                                    } else {
                                        val msg = when(name) {
                                            "Cash" -> "Please add Cash Balance first"
                                            "Credit Card" -> "Please add a Credit Card first"
                                            else -> "Please add a Bank Account to use $name"
                                        }
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        modeExpanded = false
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
                            .border(
                                1.dp, 
                                if(paidByExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), 
                                RoundedCornerShape(12.dp)
                            )
                            .menuAnchor()
                            .background(
                                if (!isPaidByActive && selectedSourceType != "Cash") MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else Color.Transparent, 
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp), 
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedSourceType.isEmpty()) {
                                Text(
                                    text = "Select Mode", 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                    fontSize = 14.sp, 
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Outlined.ArrowDropDown, null, tint = Color.Transparent, modifier = Modifier.size(20.dp))
                            } else if (selectedSourceId.isEmpty()) {
                                Text(
                                    text = if(selectedSourceType == "Bank") "Choose Bank" else "Choose Card", 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                    fontSize = 14.sp, 
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Outlined.ArrowDropDown, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                                    modifier = Modifier
                                        .size(20.dp)
                                        .rotate(if (paidByExpanded) 180f else 0f)
                                )
                            } else {
                                if (selectedSourceLogo != null && selectedSourceType != "Cash") {
                                    Image(
                                        painter = painterResource(id = selectedSourceLogo!!), 
                                        contentDescription = null, 
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(RoundedCornerShape(4.dp)), 
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
                                if (isPaidByActive) {
                                    Icon(
                                        imageVector = Icons.Outlined.ArrowDropDown, 
                                        contentDescription = null, 
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                                        modifier = Modifier
                                            .size(20.dp)
                                            .rotate(if (paidByExpanded) 180f else 0f)
                                    )
                                }
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
                                DropdownMenuItem(
                                    text = { Text("No Banks Linked", color = MaterialTheme.colorScheme.onSurfaceVariant) }, 
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
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                ) 
                                            } else { 
                                                Icon(
                                                    imageVector = Icons.Outlined.AccountBalance, 
                                                    contentDescription = null, 
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                                                    modifier = Modifier.size(24.dp)
                                                ) 
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            val shortAcc = if (bank.accountNo.length >= 4) bank.accountNo.takeLast(4) else bank.accountNo
                                            Text(
                                                text = "• $shortAcc", 
                                                color = MaterialTheme.colorScheme.onSurface, 
                                                maxLines = 1, 
                                                softWrap = false, 
                                                overflow = TextOverflow.Ellipsis, 
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    },
                                    onClick = { 
                                        val shortAcc = if (bank.accountNo.length >= 4) bank.accountNo.takeLast(4) else bank.accountNo
                                        selectedSourceId = bank.accountNo
                                        selectedSourceName = "• $shortAcc"
                                        selectedSourceLogo = Constants.BankLogoMap[bank.bankName]
                                        paidByExpanded = false
                                    }
                                )
                            }
                        } else if (selectedSourceType == "Credit Card") {
                            if (ccList.isEmpty()) { 
                                DropdownMenuItem(
                                    text = { Text("No Cards Linked", color = MaterialTheme.colorScheme.onSurfaceVariant) }, 
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
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                ) 
                                            } else { 
                                                Icon(
                                                    imageVector = Icons.Outlined.CreditCard, 
                                                    contentDescription = null, 
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                                                    modifier = Modifier.size(24.dp)
                                                ) 
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            val shortAcc = if (cc.cardNo.length >= 4) cc.cardNo.takeLast(4) else cc.cardNo
                                            Text(
                                                text = "• $shortAcc", 
                                                color = MaterialTheme.colorScheme.onSurface, 
                                                maxLines = 1, 
                                                softWrap = false, 
                                                overflow = TextOverflow.Ellipsis, 
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    },
                                    onClick = { 
                                        val shortAcc = if (cc.cardNo.length >= 4) cc.cardNo.takeLast(4) else cc.cardNo
                                        selectedSourceId = cc.cardNo
                                        selectedSourceName = "• $shortAcc"
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
                CustomDatePicker(
                    label = "Date",
                    selectedDateMillis = expenseDateMillis,
                    onDateSelected = { expenseDateMillis = it },
                    restrictToCurrentMonth = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = amount, 
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), 
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary, 
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), 
                        focusedTextColor = MaterialTheme.colorScheme.onSurface, 
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val finalCategory = categoryText.trim()
                    val finalMode = modeText.trim()
                    val finalExpenseDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(expenseDateMillis))
                    
                    val canSave = if (hasNoFinance) {
                        amount.isNotBlank() && finalCategory.isNotBlank()
                    } else {
                        amount.isNotBlank() && finalCategory.isNotBlank() && finalMode.isNotBlank() &&
                        (selectedSourceType.isEmpty() || selectedSourceId.isNotEmpty())
                    }

                    if (canSave) {
                        val expenseAmt = amount.toDoubleOrNull() ?: 0.0
                        
                        val actualMode = if (hasNoFinance) "Unspecified" else finalMode
                        val actualSourceType = if (hasNoFinance) "None" else selectedSourceType
                        val actualSourceId = if (hasNoFinance) "None" else selectedSourceId
                        
                        val newEntry = TransactionModel(
                            date = finalExpenseDateStr, 
                            amount = expenseAmt, 
                            category = finalCategory, 
                            remark1 = remark1, 
                            remark2 = remark2, 
                            mode = actualMode,
                            sourceType = actualSourceType,
                            sourceId = actualSourceId
                        )
                        onExpenseAdded(newEntry)
                        onDismiss() 

                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val db = FirebaseFirestore.getInstance()
                                val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                                if (!userQuery.isEmpty) {
                                    val userRef = userQuery.documents[0].reference
                                    val paymentDetailStr = "$actualMode | $actualSourceType | $actualSourceId"

                                    val dateForDoc = SimpleDateFormat("yyyy_MM", Locale.getDefault()).format(Date(expenseDateMillis))
                                    val expensesDocRef = userRef.collection("Expenses").document(dateForDoc)
                                    
                                    val expenseDocSnap = expensesDocRef.get().await()
                                    var nextSeq = 1
                                    
                                    if (expenseDocSnap.exists()) {
                                        val dataMap = expenseDocSnap.data
                                        if (dataMap != null) {
                                            val seqKeys = dataMap.keys.filter { it.matches(Regex("^\\d{3}$")) && it != "000_total" }
                                            if (seqKeys.isNotEmpty()) {
                                                val maxSeq = seqKeys.maxOf { it.toInt() }
                                                nextSeq = maxSeq + 1
                                            }
                                        }
                                    }
                                    
                                    val formattedSeq = String.format(Locale.US, "%03d", nextSeq)
                                    
                                    val expData = hashMapOf<String, Any>(
                                        "dt" to Timestamp(Date(expenseDateMillis)),
                                        "amnt" to expenseAmt,
                                        "cat" to finalCategory,
                                        "det1" to remark1,
                                        "det2" to remark2,
                                        "pay" to paymentDetailStr
                                    )
                                    
                                    val updateMap = hashMapOf<String, Any>(
                                        formattedSeq to expData,
                                        "000_total" to FieldValue.increment(expenseAmt)
                                    )
                                    
                                    expensesDocRef.set(updateMap, SetOptions.merge()).await()

                                    // BALANCE DEDUCTION LOGIC
                                    if (expenseAmt > 0) {
                                        when (actualSourceType) {
                                            "Cash" -> {
                                                val cashDoc = userRef.collection("Finances").document("Cash").get().await()
                                                val cur = cashDoc.getDouble("total_cash") ?: 0.0
                                                val newCash = (cur - expenseAmt).coerceAtLeast(0.0)
                                                userRef.collection("Finances").document("Cash").update("total_cash", newCash).await()
                                            }
                                            "Bank" -> {
                                                if (actualSourceId.isNotEmpty()) {
                                                    val bankDoc = userRef.collection("Finances").document("Banking_Data").get().await()
                                                    val bankData = bankDoc.get(actualSourceId) as? Map<*, *>
                                                    if (bankData != null) {
                                                        val curBal = (bankData["current_bal"] as? Number)?.toDouble() ?: 0.0
                                                        val newBal = (curBal - expenseAmt).coerceAtLeast(0.0)
                                                        userRef.collection("Finances").document("Banking_Data").update("${actualSourceId}.current_bal", newBal).await()
                                                    }
                                                }
                                            }
                                            "Credit Card" -> {
                                                if (actualSourceId.isNotEmpty()) {
                                                    val ccDoc = userRef.collection("Finances").document("Credit_Cards").get().await()
                                                    val ccData = ccDoc.get(actualSourceId) as? Map<*, *>
                                                    if (ccData != null) {
                                                        val curOut = (ccData["outstanding"] as? Number)?.toDouble() ?: 0.0
                                                        val limit = (ccData["limit"] as? Number)?.toDouble() ?: 0.0
                                                        val newOut = curOut + expenseAmt
                                                        val avail = limit - newOut
                                                        val util = if (limit > 0) (newOut / limit) * 100.0 else 0.0
                                                        
                                                        userRef.collection("Finances").document("Credit_Cards").update(
                                                            mapOf(
                                                                "${actualSourceId}.outstanding" to newOut,
                                                                "${actualSourceId}.available" to avail,
                                                                "${actualSourceId}.utilization" to util
                                                            )
                                                        ).await()
                                                    }
                                                }
                                            }
                                        }

                                        // UPDATE BUDGET STRING FORMAT
                                        val budgetDocRef = userRef.collection("Finances").document("Budget")
                                        val budgetDoc = budgetDocRef.get().await()
                                        if (budgetDoc.exists()) {
                                            val limitVal = userQuery.documents[0].getDouble("budget_limit") ?: 0.0
                                            val usedStr = budgetDoc.getString("used") ?: "0"
                                            val currentUsed = usedStr.substringBefore(" ").toDoubleOrNull() ?: 0.0
                                            val newUsed = (currentUsed + expenseAmt).coerceAtLeast(0.0)
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
                                }
                            } catch (e: Exception) { }
                        }
                    } else {
                        if (!hasNoFinance && finalMode.isNotBlank() && selectedSourceType.isNotEmpty() && selectedSourceId.isEmpty()) {
                            Toast.makeText(context, "Please select exact Paid By account/card", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please fill required fields", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .bounceClick(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Save Expense", 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 16.sp, 
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
