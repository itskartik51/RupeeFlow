package com.kartikey.rupeeflow.UI_Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.Cloud_Database.Constants
import com.kartikey.rupeeflow.R

import com.kartikey.rupeeflow.UI_Screens.Home.ExpenseSummaryCard
import com.kartikey.rupeeflow.UI_Screens.Home.GridCard
import com.kartikey.rupeeflow.UI_Screens.Home.SpendingTrackerCard
import com.kartikey.rupeeflow.UI_Screens.Home.ReminderBanner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(username: String, onLogout: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) } 

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { /* Link later */ },
                    icon = { Icon(Icons.Default.List, contentDescription = "Assets") },
                    label = { Text("Assets") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF2E7D32)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                        }
                    }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { /* Link later */ },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Analytics") },
                    label = { Text("Analytics") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { /* Link later */ },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                )
            }
        }
    ) { paddingValues ->
        if (selectedTab == 0) {
            HomeDashboardDesign(username = username, paddingValues = paddingValues, onLogout = onLogout)
        } else if (selectedTab == 2) {
            AddExpenseForm(username = username, paddingValues = paddingValues)
        }
    }
}

@Composable
fun HomeDashboardDesign(username: String, paddingValues: PaddingValues, onLogout: () -> Unit) {
    var thisMonthExpenses by remember { mutableDoubleStateOf(0.0) }
    var thisYearExpenses by remember { mutableDoubleStateOf(0.0) }
    var isLoadingExpenses by remember { mutableStateOf(true) }
    var debugErrorMessage by remember { mutableStateOf("") } 

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("action", "get_expenses")
                    put("username", username)
                }
                val client = OkHttpClient()
                val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(body).build()
                val response = client.newCall(request).execute()
                val responseData = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    if (!responseData.trim().startsWith("{")) {
                        withContext(Dispatchers.Main) {
                            debugErrorMessage = "API Error: Naya App Script deploy nahi hua hai (Non-JSON return hua)."
                            isLoadingExpenses = false
                        }
                        return@withContext
                    }

                    val jsonResponse = JSONObject(responseData)
                    if (jsonResponse.optString("status") == "success") {
                        val dataArray = jsonResponse.optJSONArray("data")
                        var monthSum = 0.0
                        var yearSum = 0.0
                        var totalParsed = 0 

                        val currentCal = Calendar.getInstance()
                        val currMonth = currentCal.get(Calendar.MONTH)
                        val currYear = currentCal.get(Calendar.YEAR)

                        val parsingFormats = listOf(
                            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()),
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        )

                        if (dataArray != null && dataArray.length() > 0) {
                            for (i in 0 until dataArray.length()) {
                                val item = dataArray.getJSONObject(i)
                                val rawDateStr = item.optString("date")
                                val amtStr = item.optString("amount", "0")
                                
                                val amt = amtStr.toDoubleOrNull() ?: item.optDouble("amount", 0.0)
                                if (amt.isNaN()) continue

                                // Time hatane ke liye space se split kiya ("09-07-2026 4:14 PM" -> "09-07-2026")
                                val datePartOnly = rawDateStr.trim().split("\\s+".toRegex())[0]

                                var parsedDate: java.util.Date? = null
                                for (format in parsingFormats) {
                                    try {
                                        parsedDate = format.parse(datePartOnly)
                                        if (parsedDate != null) break
                                    } catch (e: Exception) {}
                                }

                                if (parsedDate != null) {
                                    totalParsed++
                                    val cal = Calendar.getInstance()
                                    cal.time = parsedDate
                                    if (cal.get(Calendar.YEAR) == currYear) {
                                        yearSum += amt
                                        if (cal.get(Calendar.MONTH) == currMonth) {
                                            monthSum += amt
                                        }
                                    }
                                }
                            }
                            withContext(Dispatchers.Main) {
                                thisMonthExpenses = monthSum
                                thisYearExpenses = yearSum
                                debugErrorMessage = "Success: ${dataArray.length()} entries aayi, usme se $totalParsed ka date match hua!" 
                                isLoadingExpenses = false
                            }
                        } else {
                            withContext(Dispatchers.Main) { debugErrorMessage = "Sheet se 0 data mila. Username check karein."; isLoadingExpenses = false }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            debugErrorMessage = "Sheet Status Error: ${jsonResponse.optString("message")}"
                            isLoadingExpenses = false
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        debugErrorMessage = "HTTP Connection Failed!"
                        isLoadingExpenses = false
                    }
                }
            } catch (e: Exception) { 
                withContext(Dispatchers.Main) { 
                    debugErrorMessage = "Code Exception: ${e.localizedMessage}"
                    isLoadingExpenses = false 
                } 
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(16.dp))

        // X-RAY (DIAGNOSTIC) BANNER 
        if (debugErrorMessage.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2)), // Light Red background
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text(
                    text = "STATUS X-RAY: $debugErrorMessage",
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // HEADER
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Image(painter = painterResource(id = R.mipmap.ic_launcher), contentDescription = "App Logo", modifier = Modifier.size(44.dp).clip(CircleShape))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("RupeeFlow", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text("Hi, $username", color = Color.Gray, fontSize = 12.sp)
            }
            Text("INR (₹) / USD", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFE8F5E9)), contentAlignment = Alignment.Center) {
                Text(username.take(2).uppercase(), color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        ExpenseSummaryCard(thisMonthTotal = thisMonthExpenses, thisYearTotal = thisYearExpenses, isLoading = isLoadingExpenses)
        
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GridCard(title = "STOCKS", value = "₹0", lineColor = Color(0xFF2E7D32), modifier = Modifier.weight(1f)) 
            GridCard(title = "MUTUAL FUNDS", value = "₹0", lineColor = Color(0xFF039BE5), modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GridCard(title = "BANK / FD", value = "₹0", lineColor = Color(0xFFFFB300), modifier = Modifier.weight(1f))
            GridCard(title = "BUDGET LIMIT", value = "0% Used", lineColor = Color.Transparent, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(24.dp))
        SpendingTrackerCard()
        Spacer(modifier = Modifier.height(16.dp))
        ReminderBanner()
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Recent Transactions", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            TextButton(onClick = onLogout) { Text("Logout", color = Color(0xFFD32F2F)) }
        }
        Spacer(modifier = Modifier.height(60.dp)) 
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseForm(username: String, paddingValues: PaddingValues) {
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food") }
    var expanded by remember { mutableStateOf(false) }
    val categories = listOf("Food", "Transport", "Bills", "Shopping", "Others")
    var detail1 by remember { mutableStateOf("") }
    var detail2 by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(20.dp).verticalScroll(rememberScrollState())) {
        Text("Add New Expense", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (₹)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(value = selectedCategory, onValueChange = {}, readOnly = true, label = { Text("Category") }, modifier = Modifier.menuAnchor().fillMaxWidth())
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                categories.forEach { cat -> 
                    DropdownMenuItem(text = { Text(cat) }, onClick = { selectedCategory = cat; expanded = false; detail1 = ""; detail2 = "" }) 
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        when (selectedCategory) {
            "Food" -> {
                OutlinedTextField(value = detail1, onValueChange = { detail1 = it }, label = { Text("Where did you eat?") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = detail2, onValueChange = { detail2 = it }, label = { Text("What did you eat?") }, modifier = Modifier.fillMaxWidth())
            }
            "Transport" -> {
                OutlinedTextField(value = detail1, onValueChange = { detail1 = it }, label = { Text("From Where?") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = detail2, onValueChange = { detail2 = it }, label = { Text("To Where?") }, modifier = Modifier.fillMaxWidth())
            }
            "Bills" -> {
                OutlinedTextField(value = detail1, onValueChange = { detail1 = it }, label = { Text("Which Bill?") }, modifier = Modifier.fillMaxWidth())
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        statusMessage = "Saving..."
                        val json = JSONObject().apply {
                            put("action", "add_expense")
                            put("username", username)
                            put("amount", amount)
                            put("category", selectedCategory)
                            put("detail1", detail1)
                            put("detail2", detail2)
                        }
                        val client = OkHttpClient()
                        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                        val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(body).build()
                        val response = client.newCall(request).execute()
                        
                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful) {
                                statusMessage = "Saved Successfully!"
                                amount = ""; detail1 = ""; detail2 = ""
                            } else statusMessage = "Failed!"
                        }
                    } catch (e: Exception) { withContext(Dispatchers.Main) { statusMessage = "Error!" } }
                }
            }, 
            modifier = Modifier.fillMaxWidth(), 
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
        ) { Text("Save Expense", color = Color.White) }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(statusMessage, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
    }
}
