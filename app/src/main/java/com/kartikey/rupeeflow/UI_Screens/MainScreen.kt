package com.kartikey.rupeeflow.UI_Screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kartikey.rupeeflow.Cloud_Database.Constants
import com.kartikey.rupeeflow.UI_Screens.Home.HomeDashboardDesign
import com.kartikey.rupeeflow.UI_Screens.Add.AddScreen
import com.kartikey.rupeeflow.UI_Screens.Add.TransactionModel
import com.kartikey.rupeeflow.UI_Screens.Assets.AssetsScreen
import com.kartikey.rupeeflow.UI_Screens.Assets.InvestmentItem
import com.kartikey.rupeeflow.UI_Screens.Assets.BankAccountItem
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

    // State hoisted from AssetsScreen
    var assetsCurrentView by remember { mutableStateOf("Main") }

    var thisMonthExpenses by remember { mutableDoubleStateOf(0.0) }
    var thisYearExpenses by remember { mutableDoubleStateOf(0.0) }
    var isLoadingExpenses by remember { mutableStateOf(true) }
    
    var transactionList by remember { mutableStateOf(emptyList<TransactionModel>()) }
    var investmentList by remember { mutableStateOf(emptyList<InvestmentItem>()) }
    var bankList by remember { mutableStateOf(emptyList<BankAccountItem>()) } 
    
    var dNavState by remember { mutableStateOf("Connecting to Sheet...") }
    var dBackPresses by remember { mutableIntStateOf(0) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedTab, showExpenseHistory, isLoadingExpenses, transactionList.size, investmentList.size, bankList.size) {
        if (showExpenseHistory) {
            dNavState = "Expense History"
        } else if (selectedTab != 0) {
            dNavState = "Tab $selectedTab"
        } else {
            dNavState = if (isLoadingExpenses) {
                "Syncing Data... ⏳"
            } else {
                "Sheet Sync: Exp (${transactionList.size}) ✅ | Inv (${investmentList.size}) ✅ | Banks (${bankList.size}) ✅"
            }
        }
    }

    // Smart Back Handler
    BackHandler(enabled = showExpenseHistory || selectedTab != 0 || assetsCurrentView != "Main") {
        dBackPresses++ 
        if (showExpenseHistory) {
            showExpenseHistory = false 
        } else if (selectedTab == 1 && assetsCurrentView != "Main") {
            assetsCurrentView = "Main" // Sub-screen se Assets root par layega
        } else if (selectedTab != 0) {
            selectedTab = 0 
        }
    }

    LaunchedEffect(refreshTrigger) {
        isLoadingExpenses = true
        launch(Dispatchers.IO) {
            try {
                val cal = Calendar.getInstance()
                val currM = cal.get(Calendar.MONTH) + 1
                val currY = cal.get(Calendar.YEAR)

                val json = JSONObject().apply { put("action", "get_all_data"); put("username", username) }
                val client = OkHttpClient()
                val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(body).build()
                val response = client.newCall(request).execute()
                val responseData = response.body?.string() ?: ""

                if (response.isSuccessful && responseData.trim().startsWith("{")) {
                    val jsonResponse = JSONObject(responseData)
                    if (jsonResponse.optString("status") == "success") {
                        
                        val expensesArray = jsonResponse.optJSONArray("expenses")
                        var tempTotal = 0.0
                        var tempMonth = 0.0
                        var tempYear = 0.0
                        val tempHistory = mutableListOf<TransactionModel>()

                        if (expensesArray != null && expensesArray.length() > 0) {
                            val currMonthStr = String.format(Locale.US, "%02d", currM)
                            val currYearStr = currY.toString()

                            for (i in 0 until expensesArray.length()) {
                                val item = expensesArray.getJSONObject(i)
                                val rawDate = item.optString("date", "").trim()
                                val rawAmt = item.optString("amount", "0")
                                val cleanAmt = rawAmt.replace("[^\\d.]".toRegex(), "")
                                val amt = cleanAmt.toDoubleOrNull() ?: item.optDouble("amount", 0.0)
                                
                                if (amt > 0.0) {
                                    tempTotal += amt 
                                    tempHistory.add(TransactionModel(rawDate, amt, item.optString("category", "Unknown"), item.optString("detail1", ""), item.optString("detail2", "")))
                                    
                                    if (rawDate.contains(currYearStr)) {
                                        tempYear += amt
                                        if (rawDate.contains("-$currMonthStr-") || rawDate.contains("/$currMonthStr/") || rawDate.startsWith("$currMonthStr-") || rawDate.startsWith("$currMonthStr/")) {
                                            tempMonth += amt
                                        }
                                    }
                                }
                            }
                        }

                        val invArray = jsonResponse.optJSONArray("investments")
                        val fetchedInvList = mutableListOf<InvestmentItem>()
                        if (invArray != null) {
                            for (i in 0 until invArray.length()) {
                                val item = invArray.getJSONObject(i)
                                fetchedInvList.add(
                                    InvestmentItem(
                                        assetName = item.optString("asset_name", ""),
                                        quantity = item.optDouble("quantity", 0.0),
                                        avgBuyPrice = item.optDouble("buy_price", 0.0),
                                        currentPrice = item.optDouble("current_price", item.optDouble("buy_price", 0.0)),
                                        oneDayChangePrice = item.optDouble("one_day_change", 0.0)
                                    )
                                )
                            }
                        }

                        val banksArray = jsonResponse.optJSONArray("banks")
                        val fetchedBankList = mutableListOf<BankAccountItem>()
                        if (banksArray != null) {
                            for (i in 0 until banksArray.length()) {
                                val item = banksArray.getJSONObject(i)
                                fetchedBankList.add(
                                    BankAccountItem(
                                        bankName = item.optString("bank_name", ""),
                                        accountNo = item.optString("account_no", ""),
                                        currentBalance = item.optDouble("current_bal", 0.0),
                                        interestRate = item.optDouble("interest_rate", 0.0),
                                        qtrInterestPct = item.optDouble("qtr_interest_pct", 0.0),
                                        oneDayInt = item.optDouble("one_day_int", 0.0)
                                    )
                                )
                            }
                        }

                        withContext(Dispatchers.Main) {
                            thisMonthExpenses = if (tempMonth > 0) tempMonth else tempTotal 
                            thisYearExpenses = if (tempYear > 0) tempYear else tempTotal
                            transactionList = tempHistory.reversed()
                            investmentList = fetchedInvList
                            bankList = fetchedBankList
                            isLoadingExpenses = false
                        }
                    } else {
                        withContext(Dispatchers.Main) { isLoadingExpenses = false }
                    }
                } else {
                    withContext(Dispatchers.Main) { isLoadingExpenses = false }
                }
            } catch (e: Exception) { 
                withContext(Dispatchers.Main) { isLoadingExpenses = false } 
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                NavigationBarItem(
                    selected = selectedTab == 0 && !showExpenseHistory,
                    onClick = { selectedTab = 0; showExpenseHistory = false },
                    icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") }, 
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { 
                        // The magic logic: if already on tab 1, reset the view.
                        if (selectedTab == 1) {
                            assetsCurrentView = "Main"
                        }
                        selectedTab = 1
                        showExpenseHistory = false 
                    },
                    icon = { Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = "Assets") }, 
                    label = { Text("Assets") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2; showExpenseHistory = false },
                    icon = {
                        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF2E7D32)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Add, contentDescription = "Add", tint = Color.White) 
                        }
                    }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3; showExpenseHistory = false },
                    icon = { Icon(Icons.Outlined.PieChart, contentDescription = "Analytics") }, 
                    label = { Text("Analytics") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4; showExpenseHistory = false },
                    icon = { Icon(Icons.Outlined.Person, contentDescription = "Profile") }, 
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                )
            }
        }
    ) { paddingValues ->
        Crossfade(
            targetState = Pair(selectedTab, showExpenseHistory), 
            animationSpec = tween(durationMillis = 400),
            label = "Screen Transition"
        ) { state ->
            val (currentTab, isHistoryVisible) = state
            
            if (isHistoryVisible) {
                com.kartikey.rupeeflow.UI_Screens.Home.ExpenseHistoryScreen(
                    paddingValues = paddingValues, 
                    history = transactionList,
                    onBackClick = { showExpenseHistory = false }
                )
            } else {
                when (currentTab) {
                    0 -> HomeDashboardDesign(
                        username = username, paddingValues = paddingValues, 
                        thisMonthExpenses = thisMonthExpenses, thisYearExpenses = thisYearExpenses, isLoadingExpenses = isLoadingExpenses,
                        dNavState = dNavState, dBackPresses = dBackPresses, 
                        onLogout = onLogout,
                        onRefreshExpenses = { refreshTrigger++ }, 
                        onExpenseCardClick = { showExpenseHistory = true }
                    )
                    1 -> AssetsScreen(
                        paddingValues = paddingValues, 
                        username = username, 
                        investmentList = investmentList,
                        bankList = bankList, 
                        isLoading = isLoadingExpenses,
                        onRefreshClick = { refreshTrigger++ },
                        currentView = assetsCurrentView,
                        onViewChange = { assetsCurrentView = it }
                    )
                    2 -> AddScreen(
                        paddingValues = paddingValues, 
                        username = username,
                        onExpenseAdded = { newEntry -> 
                            transactionList = listOf(newEntry) + transactionList 
                        },
                        onInvestmentAdded = { refreshTrigger++ },
                        onFinanceAdded = { refreshTrigger++ } 
                    )
                    3 -> AnalyticsScreen(paddingValues = paddingValues)
                    4 -> ProfileScreen(username = username, paddingValues = paddingValues, onLogout = onLogout)
                }
            }
        }
    }
}
