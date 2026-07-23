package com.kartikey.rupeeflow.UI_Screens.Home

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kartikey.rupeeflow.Cloud_Database.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ==========================================
// DATA MODELS
// ==========================================
data class ContriExpense(val itemName: String, val amount: Double, val date: String)
data class MemberLedger(val memberName: String, val totalSpent: Double, val expenses: List<ContriExpense>)

@Composable
fun InsideContriScreen(
    username: String, // <-- Ab ye component secure hai!
    room: ContriRoomModel,
    onBackClick: () -> Unit,
    onLeaveClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val formattedName = if (room.roomName.length > 10) "${room.roomName.take(10)}..." else room.roomName

    // Cache Engine setup
    val sharedPreferences = context.getSharedPreferences("RupeeFlowCache", Context.MODE_PRIVATE)
    val cacheKey = "room_data_${room.roomCode}"

    var ledgers by remember { mutableStateOf<List<MemberLedger>>(emptyList()) }
    var totalGroupExpense by remember { mutableDoubleStateOf(0.0) }
    var isLoading by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) } // Auto refresh trigger
    
    var showAddExpenseDialog by remember { mutableStateOf(false) }

    // ==========================================
    // OFFLINE-FIRST CACHE & SILENT SYNC LOGIC
    // ==========================================
    LaunchedEffect(room.roomCode, refreshTrigger) {
        val cachedJson = sharedPreferences.getString(cacheKey, null)
        if (cachedJson != null) {
            try {
                val (cachedLedgers, cachedTotal) = parseLedgerData(cachedJson)
                ledgers = cachedLedgers
                totalGroupExpense = cachedTotal
            } catch (e: Exception) { e.printStackTrace() }
        } else {
            isLoading = true
        }

        withContext(Dispatchers.IO) {
            try {
                val jsonBody = JSONObject().apply {
                    put("action", "fetch_room_details")
                    put("room_code", room.roomCode)
                }
                val request = Request.Builder()
                    .url(Constants.GOOGLE_SHEET_API_URL)
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = OkHttpClient().newCall(request).execute()
                val resData = response.body?.string() ?: ""

                if (resData.contains("\"status\":\"success\"")) {
                    sharedPreferences.edit().putString(cacheKey, resData).apply()
                    val (newLedgers, newTotal) = parseLedgerData(resData)
                    withContext(Dispatchers.Main) {
                        ledgers = newLedgers
                        totalGroupExpense = newTotal
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Color.Black)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = formattedName, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.Black)
                Spacer(modifier = Modifier.weight(1f))
                
                IconButton(onClick = onLeaveClick) {
                    Icon(imageVector = Icons.AutoMirrored.Outlined.ExitToApp, contentDescription = "Leave Room", tint = Color.Red)
                }
            }
        },
        floatingActionButton = {
            PremiumFloatingButton(onClick = { showAddExpenseDialog = true })
        },
        containerColor = Color(0xFFFAFAFA)
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            // INFO CARD
            Card(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("₹", fontSize = 38.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(totalGroupExpense.toInt().toString(), fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                clipboardManager.setText(AnnotatedString("Join my RupeeFlow Contri!\nCode: ${room.roomCode}\nPin: ${room.pin}"))
                                Toast.makeText(context, "Code Copied!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = Color.Gray, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = room.roomCode, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black, letterSpacing = 1.sp)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "Pin: ${room.pin}", fontSize = 15.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // SMART DYNAMIC LEDGER SECTION
            if (isLoading && ledgers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF2E7D32))
                }
            } else if (ledgers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(top = 40.dp), contentAlignment = Alignment.TopCenter) {
                    Text("No expenses yet. Tap + to add!", color = Color.Gray, fontWeight = FontWeight.Medium)
                }
            } else {
                val memberCount = ledgers.size
                val isScrollable = memberCount > 3
                val fixedColumnWidth = 110.dp

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { if (isScrollable) it.horizontalScroll(rememberScrollState()) else it }
                        .padding(horizontal = 16.dp)
                ) {
                    Row(modifier = if (!isScrollable) Modifier.fillMaxWidth() else Modifier) {
                        ledgers.forEach { ledger ->
                            Column(
                                modifier = if (isScrollable) Modifier.width(fixedColumnWidth) else Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = ledger.memberName, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "₹${ledger.totalSpent.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }
                        }
                    }

                    val dividerModifier = if (isScrollable) Modifier.width(fixedColumnWidth * memberCount) else Modifier.fillMaxWidth()
                    HorizontalDivider(
                        modifier = dividerModifier.padding(vertical = 12.dp),
                        thickness = 1.dp,
                        color = Color.LightGray
                    )

                    Row(modifier = if (!isScrollable) Modifier.fillMaxWidth() else Modifier) {
                        ledgers.forEach { ledger ->
                            Column(
                                modifier = if (isScrollable) Modifier.width(fixedColumnWidth) else Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ledger.expenses.forEach { expense ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    ) {
                                        Text(text = expense.itemName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = "₹${expense.amount.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = expense.date, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // REAL NETWORK SPLIT ENGINE CALL
        // ==========================================
        if (showAddExpenseDialog) {
            AddContriExpenseDialog(
                onDismiss = { showAddExpenseDialog = false },
                onAdd = { title, dateMillis, amount ->
                    showAddExpenseDialog = false
                    isLoading = true // Show loading spinner
                    
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            // SPLIT MATH ENGINE
                            val memberCount = ledgers.size
                            val splitAmount = if (memberCount > 0) amount / memberCount else amount
                            
                            val amountsObj = JSONObject()
                            ledgers.forEach { ledger ->
                                amountsObj.put(ledger.memberName, splitAmount)
                            }
                            
                            // Format Date exactly as backend needs
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            val formattedDate = sdf.format(Date(dateMillis))

                            val jsonBody = JSONObject().apply {
                                put("action", "add_contri_expense")
                                put("username", username)
                                put("room_code", room.roomCode)
                                put("date", formattedDate)
                                put("item_name", title)
                                put("amounts", amountsObj.toString())
                            }
                            
                            val request = Request.Builder()
                                .url(Constants.GOOGLE_SHEET_API_URL)
                                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                                .build()

                            val response = OkHttpClient().newCall(request).execute()
                            val resData = response.body?.string() ?: ""

                            withContext(Dispatchers.Main) {
                                if (resData.contains("\"status\":\"success\"")) {
                                    Toast.makeText(context, "Expense Added & Split Successfully!", Toast.LENGTH_SHORT).show()
                                    // Remove old cache and fetch fresh data
                                    sharedPreferences.edit().remove(cacheKey).apply()
                                    refreshTrigger++ 
                                } else {
                                    Toast.makeText(context, "Error saving expense", Toast.LENGTH_SHORT).show()
                                    isLoading = false
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
                                isLoading = false
                            }
                        }
                    }
                }
            )
        }
    }
}

// ==========================================
// ADD EXPENSE POPUP (DIALOG)
// ==========================================
@Composable
fun AddContriExpenseDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Long, Double) -> Unit
) {
    var expenseTitle by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dateMillis by remember { mutableStateOf<Long?>(System.currentTimeMillis()) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(dismissOnClickOutside = false)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Add New Expense", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = expenseTitle,
                    onValueChange = { expenseTitle = it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } },
                    label = { Text("Expense Title", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    com.kartikey.rupeeflow.UI_Screens.CustomDatePicker(
                        label = "Date",
                        selectedDateMillis = dateMillis,
                        onDateSelected = { dateMillis = it },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = amount,
                        onValueChange = { newValue -> 
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                amount = newValue
                            }
                        },
                        label = { Text("Amount", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        prefix = { Text("₹ ", color = Color.Black, fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32), focusedLabelColor = Color(0xFF2E7D32))
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { 
                            val amt = amount.toDoubleOrNull()
                            if (expenseTitle.isNotBlank() && amt != null && dateMillis != null) {
                                onAdd(expenseTitle, dateMillis!!, amt)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Add Expense", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// JSON PARSER LOGIC
// ==========================================
fun parseLedgerData(jsonString: String): Pair<List<MemberLedger>, Double> {
    val ledgers = mutableListOf<MemberLedger>()
    var totalGroupExp = 0.0
    
    try {
        val root = JSONObject(jsonString)
        totalGroupExp = root.optDouble("total_group_expense", 0.0)
        
        val membersArray = root.getJSONArray("members")
        for (i in 0 until membersArray.length()) {
            val memberObj = membersArray.getJSONObject(i)
            val name = memberObj.getString("name")
            val totalSpent = memberObj.getDouble("total_spent")
            
            val expensesList = mutableListOf<ContriExpense>()
            val expensesArray = memberObj.getJSONArray("expenses")
            
            for (j in 0 until expensesArray.length()) {
                val expObj = expensesArray.getJSONObject(j)
                expensesList.add(
                    ContriExpense(
                        itemName = expObj.getString("item_name"),
                        amount = expObj.getDouble("amount"),
                        date = expObj.getString("date")
                    )
                )
            }
            ledgers.add(MemberLedger(name, totalSpent, expensesList))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return Pair(ledgers, totalGroupExp)
}

// ==========================================
// PREMIUM BOUNCE FLOATING BUTTON (+)
// ==========================================
@Composable
fun PremiumFloatingButton(onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.85f else 1f, label = "fabScale")

    Box(
        modifier = Modifier
            .scale(scale)
            .size(56.dp)
            .background(Color(0xFF2E7D32), shape = CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = "Add Expense",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}
