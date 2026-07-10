package com.kartikey.rupeeflow.UI_Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.Cloud_Database.Constants
import com.kartikey.rupeeflow.R

// Teeno Naye Screen aur Model Import kiye
import com.kartikey.rupeeflow.UI_Screens.Home.ExpenseSummaryCard
import com.kartikey.rupeeflow.UI_Screens.Home.ExpenseAddScreen 
import com.kartikey.rupeeflow.UI_Screens.Home.ExpenseHistoryScreen 
import com.kartikey.rupeeflow.UI_Screens.Home.TransactionModel
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
import java.util.Calendar
import java.util.Locale

@Composable
fun HomeScreen(username: String, onLogout: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) } 
    var showExpenseHistory by remember { mutableStateOf(false) } // Naya state Red Card click ke liye

    var thisMonthExpenses by remember { mutableDoubleStateOf(0.0) }
    var thisYearExpenses by remember { mutableDoubleStateOf(0.0) }
    var isLoadingExpenses by remember { mutableStateOf(true) }
    var transactionList by remember { mutableStateOf(emptyList<TransactionModel>()) }
    
    // DIAGNOSTIC STATES 
    var dPhoneDate by remember { mutableStateOf("") }
    var dRawDate by remember { mutableStateOf("") }
    var dRawAmt by remember { mutableStateOf("") }
    var dTotalCount by remember { mutableIntStateOf(0) }
    var dTotalUnfiltered by remember { mutableDoubleStateOf(0.0) }
    var dError by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val cal = Calendar.getInstance()
                val currM = cal.get(Calendar.MONTH) + 1
                val currY = cal.get(Calendar.YEAR)
                dPhoneDate = "$currM/$currY"

                val json = JSONObject().apply {
                    put("action", "get_expenses")
                    put("username", username)
                }
                val client = OkHttpClient()
                val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(body).build()
                val response = client.newCall(request).execute()
                val responseData = response.body?.string() ?: ""

                if (response.isSuccessful && responseData.trim().startsWith("{")) {
                    val jsonResponse = JSONObject(responseData)
                    if (jsonResponse.optString("status") == "success") {
                        val dataArray = jsonResponse.optJSONArray("data")
                        var tempTotal = 0.0
                        var tempMonth = 0.0
                        var tempYear = 0.0
                        val tempHistory = mutableListOf<TransactionModel>()

                        if (dataArray != null && dataArray.length() > 0) {
                            dTotalCount = dataArray.length()
                            val firstItem = dataArray.getJSONObject(0)
                            dRawDate = firstItem.optString("date", "NULL")
                            dRawAmt = firstItem.optString("amount", "NULL")

                            val currMonthStr = String.format(Locale.US, "%02d", currM)
                            val currYearStr = currY.toString()

                            for (i in 0 until dataArray.length()) {
                                val item = dataArray.getJSONObject(i)
                                val rawDate = item.optString("date", "").trim()
                                val rawAmt = item.optString("amount", "0")
                                val category = item.optString("category", item.optString("Category", "Unknown"))
                                val detail1 = item.optString("detail 1", item.optString("Detail 1", item.optString("detail1", "")))
                                val detail2 = item.optString("detail 2", item.optString("Detail 2", item.optString("detail2", "")))
                                
                                val cleanAmt = rawAmt.replace("[^\\d.]".toRegex(), "")
                                val amt = cleanAmt.toDoubleOrNull() ?: item.optDouble("amount", 0.0)
                                
                                if (amt > 0.0) {
                                    tempTotal += amt 
                                    tempHistory.add(TransactionModel(rawDate, amt, category, detail1, detail2))
                                    
                                    if (rawDate.contains(currYearStr)) {
                                        tempYear += amt
                                        if (rawDate.contains("-$currMonthStr-") || rawDate.contains("/$currMonthStr/") || rawDate.startsWith("$currMonthStr-") || rawDate.startsWith("$currMonthStr/")) {
                                            tempMonth += amt
                                        }
                                    }
                                }
                            }
                        }
                        withContext(Dispatchers.Main) {
                            dTotalUnfiltered = tempTotal
                            thisMonthExpenses = if (tempMonth > 0) tempMonth else tempTotal 
                            thisYearExpenses = if (tempYear > 0) tempYear else tempTotal
                            transactionList = tempHistory.reversed()
                            isLoadingExpenses = false
                        }
                    } else {
                        dError = "API Status: ${jsonResponse.optString("message")}"
                        isLoadingExpenses = false
                    }
                } else {
                    dError = "Invalid JSON"
                    isLoadingExpenses = false
                }
            } catch (e: Exception) {
                dError = e.localizedMessage ?: "Error"
                isLoadingExpenses = false
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                NavigationBarItem(
                    selected = selectedTab == 0 && !showExpenseHistory,
                    onClick = { selectedTab = 0; showExpenseHistory = false },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; showExpenseHistory = false },
                    icon = { Icon(Icons.Default.List, contentDescription = "Assets") },
                    label = { Text("Assets") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2; showExpenseHistory = false },
                    icon = {
                        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF2E7D32)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                        }
                    }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3; showExpenseHistory = false },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Analytics") },
                    label = { Text("Analytics") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4; showExpenseHistory = false },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                )
            }
        }
    ) { paddingValues ->
        // NAVIGATION LOGIC
        if (showExpenseHistory) {
            // RED CARD PAR CLICK KARTE HI YE DIKHEGA
            ExpenseHistoryScreen(
                paddingValues = paddingValues, 
                history = transactionList,
                onBackClick = { showExpenseHistory = false } // Back dabane par wapas Home par
            )
        } else if (selectedTab == 0) {
            HomeDashboardDesign(
                username = username, paddingValues = paddingValues, 
                thisMonthExpenses = thisMonthExpenses, thisYearExpenses = thisYearExpenses, isLoadingExpenses = isLoadingExpenses,
                dPhoneDate = dPhoneDate, dRawDate = dRawDate, dRawAmt = dRawAmt, dTotalCount = dTotalCount, dTotalUnfiltered = dTotalUnfiltered, dError = dError,
                onLogout = onLogout,
                onExpenseCardClick = { showExpenseHistory = true } // Yahan Action Pass Kiya
            )
        } else if (selectedTab == 2) {
            // '+' BUTTON PAR SIRF ADD WALA FORM DIKHEGA
            ExpenseAddScreen(username = username, paddingValues = paddingValues)
        }
    }
}

@Composable
fun HomeDashboardDesign(
    username: String, paddingValues: PaddingValues, 
    thisMonthExpenses: Double, thisYearExpenses: Double, isLoadingExpenses: Boolean,
    dPhoneDate: String, dRawDate: String, dRawAmt: String, dTotalCount: Int, dTotalUnfiltered: Double, dError: String,
    onLogout: () -> Unit,
    onExpenseCardClick: () -> Unit // Naya Parameter
) {
    Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(16.dp))

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
        
        Spacer(modifier = Modifier.height(16.dp))

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("DIAGNOSTICS (Please Screenshot):", fontWeight = FontWeight.Bold, color = Color.Red, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("1. Phone Date: $dPhoneDate", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text("2. Entries Found: $dTotalCount", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text("3. Row 1 (Raw Date): '$dRawDate'", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                Text("4. Row 1 (Raw Amt): '$dRawAmt'", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                Text("5. Total Sum (No Filter): ₹$dTotalUnfiltered", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                if (dError.isNotEmpty()) { Text("Error: $dError", color = Color.Red, fontSize = 12.sp) }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // RED CARD WALI CALL (Yahan onClick pass kar diya)
        ExpenseSummaryCard(
            thisMonthTotal = thisMonthExpenses, 
            thisYearTotal = thisYearExpenses, 
            isLoading = isLoadingExpenses,
            onClick = onExpenseCardClick
        )
        
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
