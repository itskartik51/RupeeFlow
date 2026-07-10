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
import com.kartikey.rupeeflow.UI_Screens.Features.ExpenseSummaryCard
import com.kartikey.rupeeflow.UI_Screens.Features.StockButton
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
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
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
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF2E7D32)),
                            contentAlignment = Alignment.Center
                        ) {
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
    // 1. Data Store Karne ke liye memory variables
    var thisMonthExpenses by remember { mutableDoubleStateOf(0.0) }
    var thisYearExpenses by remember { mutableDoubleStateOf(0.0) }
    var isLoadingExpenses by remember { mutableStateOf(true) }

    // 2. Background task jo App khulte hi API se data mangwayega aur calculate karega
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
                    val jsonResponse = JSONObject(responseData)
                    if (jsonResponse.optString("status") == "success") {
                        val dataArray = jsonResponse.optJSONArray("data")
                        
                        var monthSum = 0.0
                        var yearSum = 0.0

                        // Aaj ka current Month aur Year nikal rahe hain
                        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                        val currentCal = Calendar.getInstance()
                        val currMonth = currentCal.get(Calendar.MONTH)
                        val currYear = currentCal.get(Calendar.YEAR)

                        if (dataArray != null) {
                            for (i in 0 until dataArray.length()) {
                                val item = dataArray.getJSONObject(i)
                                val dateStr = item.optString("date") // Format: "10-07-2026 10:11 AM"
                                val amt = item.optDouble("amount", 0.0)

                                try {
                                    // Sirf shuru ki tarikh parse karenge "dd-MM-yyyy" (Time ignore karenge)
                                    val date = sdf.parse(dateStr.take(10))
                                    if (date != null) {
                                        val cal = Calendar.getInstance()
                                        cal.time = date
                                        val itemMonth = cal.get(Calendar.MONTH)
                                        val itemYear = cal.get(Calendar.YEAR)

                                        // Agar Expense is saal ka hai to YearSum me jodo
                                        if (itemYear == currYear) {
                                            yearSum += amt
                                            // Agar Expense is mahine ka bhi hai to MonthSum me bhi jodo
                                            if (itemMonth == currMonth) {
                                                monthSum += amt
                                            }
                                        }
                                    }
                                } catch (e: Exception) { }
                            }
                        }
                        
                        // Result ko UI mein update karna
                        withContext(Dispatchers.Main) {
                            thisMonthExpenses = monthSum
                            thisYearExpenses = yearSum
                            isLoadingExpenses = false
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { isLoadingExpenses = false }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // HEADER
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher),
                contentDescription = "App Logo",
                modifier = Modifier.size(44.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("RupeeFlow", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text("Hi, $username", color = Color.Gray, fontSize = 12.sp)
            }
            Text("INR (₹) / USD", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9)), 
                contentAlignment = Alignment.Center
            ) {
                Text(username.take(2).uppercase(), color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Yahan hum calculate kiya hua data Card ko bhej rahe hain
        ExpenseSummaryCard(
            thisMonthTotal = thisMonthExpenses,
            thisYearTotal = thisYearExpenses,
            isLoading = isLoadingExpenses
        )

        Spacer(modifier = Modifier.height(16.dp))

        // FOUR GRID CARDS
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

        // SPENDING HABITS TRACKER
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Spending Habits Tracker", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("VIEW ANALYTICS", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF2E7D32))
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val heights = listOf(30f, 40f, 20f, 80f, 40f, 35f, 50f)
                    heights.forEachIndexed { index, height ->
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .fillMaxHeight(height / 100f)
                                .background(
                                    if (index == 3) Color(0xFF5E35B1) else Color(0xFFD1C4E9),
                                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // REMINDER BANNER
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Notifications, contentDescription = "Reminder", tint = Color(0xFF2E7D32))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Daily Expense Filling Reminder", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Default reminder set for 21:00", color = Color.Gray, fontSize = 10.sp)
                }
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Gray, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Recent Transactions", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            TextButton(onClick = onLogout) { Text("Logout", color = Color(0xFFD32F2F)) }
        }
        Spacer(modifier = Modifier.height(60.dp)) 
    }
}

@Composable
fun GridCard(title: String, value: String, lineColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(8.dp))
            if (lineColor != Color.Transparent) {
                Box(modifier = Modifier.fillMaxWidth(0.6f).height(3.dp).background(lineColor, RoundedCornerShape(50)))
            }
        }
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
        
        OutlinedTextField(
            value = amount, 
            onValueChange = { amount = it }, 
            label = { Text("Amount (₹)") }, 
            modifier = Modifier.fillMaxWidth(), 
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = selectedCategory, 
                onValueChange = {}, 
                readOnly = true, 
                label = { Text("Category") }, 
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                categories.forEach { cat -> 
                    DropdownMenuItem(text = { Text(cat) }, onClick = { 
                        selectedCategory = cat
                        expanded = false
                        detail1 = ""; detail2 = ""
                    }) 
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
            colors = ButtonDefau
