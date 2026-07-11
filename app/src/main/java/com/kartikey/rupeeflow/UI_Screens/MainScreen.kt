package com.kartikey.rupeeflow.UI_Screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.* // NAYA IMPORT: Outlined icons ke liye
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kartikey.rupeeflow.Cloud_Database.Constants

// HOMESCREEN AUR EXPENSE HISTORY KE IMPORTS
import com.kartikey.rupeeflow.UI_Screens.Home.HomeDashboardDesign
import com.kartikey.rupeeflow.UI_Screens.AddExpense.ExpenseHistoryScreen
import com.kartikey.rupeeflow.UI_Screens.AddExpense.TransactionModel

// NAYA ADD SCREEN IMPORT
import com.kartikey.rupeeflow.UI_Screens.Add.AddScreen

// BAAKI 3 PAGES KE IMPORTS
import com.kartikey.rupeeflow.UI_Screens.Assets.AssetsScreen
import com.kartikey.rupeeflow.UI_Screens.Analytics.AnalyticsScreen
import com.kartikey.rupeeflow.UI_Screens.Profile.ProfileScreen
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
fun MainScreen(username: String, onLogout: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) } 
    var showExpenseHistory by remember { mutableStateOf(false) }

    var thisMonthExpenses by remember { mutableDoubleStateOf(0.0) }
    var thisYearExpenses by remember { mutableDoubleStateOf(0.0) }
    var isLoadingExpenses by remember { mutableStateOf(true) }
    var transactionList by remember { mutableStateOf(emptyList<TransactionModel>()) }
    
    var investmentCount by remember { mutableIntStateOf(0) }
    var isLoadingInvestments by remember { mutableStateOf(true) }
    
    var dNavState by remember { mutableStateOf("Connecting to Sheet...") }
    var dBackPresses by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedTab, showExpenseHistory, isLoadingExpenses, isLoadingInvestments, transactionList.size, investmentCount) {
        if (showExpenseHistory) {
            dNavState = "Expense History"
        } else if (selectedTab != 0) {
            dNavState = "Tab $selectedTab"
        } else {
            dNavState = if (isLoadingExpenses || isLoadingInvestments) {
                "Syncing Data... ⏳"
            } else {
                "Sheet Sync: Expenses (${transactionList.size}) ✅ | Investments ($investmentCount) ✅"
            }
        }
    }

    BackHandler(enabled = showExpenseHistory || selectedTab != 0) {
        dBackPresses++ 
        if (showExpenseHistory) {
            showExpenseHistory = false 
        } else if (selectedTab != 0) {
            selectedTab = 0 
        }
    }

    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            try {
                val cal = Calendar.getInstance()
                val currM = cal.get(Calendar.MONTH) + 1
                val currY = cal.get(Calendar.YEAR)

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
                            thisMonthExpenses = if (tempMonth > 0) tempMonth else tempTotal 
                            thisYearExpenses = if (tempYear > 0) tempYear else tempTotal
                            transactionList = tempHistory.reversed()
                            isLoadingExpenses = false
                        }
                    } else isLoadingExpenses = false
                } else isLoadingExpenses = false
            } catch (e: Exception) { isLoadingExpenses = false }
        }

        launch(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("action", "get_investments")
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
                        withContext(Dispatchers.Main) {
                            investmentCount = dataArray?.length() ?: 0
                            isLoadingInvestments = false
                        }
                    } else isLoadingInvestments = false
                } else isLoadingInvestments = false
            } catch (e: Exception) { isLoadingInvestments = false }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                NavigationBarItem(
                    selected = selectedTab == 0 && !showExpenseHistory,
                    onClick = { selectedTab = 0; showExpenseHistory = false },
                    icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") }, // UPDATED to Outlined Home
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; showExpenseHistory = false },
                    icon = { Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = "Assets") }, // UPDATED to Wallet
                    label = { Text("Assets") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2; showExpenseHistory = false },
                    icon = {
                        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF2E7D32)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Add, contentDescription = "Add", tint = Color.White) // UPDATED to Outlined Add
                        }
                    }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3; showExpenseHistory = false },
                    icon = { Icon(Icons.Outlined.PieChart, contentDescription = "Analytics") }, // UPDATED to PieChart
                    label = { Text("Analytics") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4; showExpenseHistory = false },
                    icon = { Icon(Icons.Outlined.Person, contentDescription = "Profile") }, // UPDATED to Outlined Person
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                )
            }
        }
    ) { paddingValues ->
        if (showExpenseHistory) {
            ExpenseHistoryScreen(
                paddingValues = paddingValues, 
                history = transactionList,
                onBackClick = { showExpenseHistory = false }
            )
        } else if (selectedTab == 0) {
            HomeDashboardDesign(
                username = username, paddingValues = paddingValues, 
                thisMonthExpenses = thisMonthExpenses, thisYearExpenses = thisYearExpenses, isLoadingExpenses = isLoadingExpenses,
                dNavState = dNavState, dBackPresses = dBackPresses, 
                onLogout = onLogout,
                onExpenseCardClick = { showExpenseHistory = true }
            )
        } else if (selectedTab == 1) {
            AssetsScreen(paddingValues = paddingValues, username = username)
        } else if (selectedTab == 2) {
            AddScreen(paddingValues = paddingValues, username = username)
        } else if (selectedTab == 3) {
            AnalyticsScreen(paddingValues = paddingValues)
        } else if (selectedTab == 4) {
            ProfileScreen(username = username, paddingValues = paddingValues, onLogout = onLogout)
        }
    }
}
