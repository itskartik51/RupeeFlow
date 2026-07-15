package com.kartikey.rupeeflow.UI_Screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.rotate
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
    var assetsCurrentView by remember { mutableStateOf("Main") }

    var bankToEdit by remember { mutableStateOf<BankAccountItem?>(null) }
    
    var showAddMenu by remember { mutableStateOf(false) }

    // FULL PROFILE DATA STATES (Ab inme data automatically ayega)
    var userFullName by remember { mutableStateOf("") } 
    var userEmail by remember { mutableStateOf("") } 
    var userMobile by remember { mutableStateOf("") }
    var userPassword by remember { mutableStateOf("") }
    var userDob by remember { mutableStateOf("") }

    var thisMonthExpenses by remember { mutableDoubleStateOf(0.0) }
    var thisYearExpenses by remember { mutableDoubleStateOf(0.0) }
    var isLoadingExpenses by remember { mutableStateOf(true) }
    
    var transactionList by remember { mutableStateOf(emptyList<TransactionModel>()) }
    var investmentList by remember { mutableStateOf(emptyList<InvestmentItem>()) }
    var bankList by remember { mutableStateOf(emptyList<BankAccountItem>()) } 
    
    var dNavState by remember { mutableStateOf("Connecting to Sheet...") }
    var dBackPresses by remember { mutableIntStateOf(0) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedTab, showExpenseHistory, isLoadingExpenses, transactionList.size, bankToEdit, showAddMenu) {
        if (showAddMenu) dNavState = "Add Menu Open"
        else if (bankToEdit != null) dNavState = "Editing Bank"
        else if (showExpenseHistory) dNavState = "Expense History"
        else dNavState = if (isLoadingExpenses) "Syncing Data... ⏳" else "Tab $selectedTab ✅"
    }

    BackHandler(enabled = showExpenseHistory || selectedTab != 0 || assetsCurrentView != "Main" || bankToEdit != null) {
        dBackPresses++ 
        when {
            bankToEdit != null -> bankToEdit = null 
            showExpenseHistory -> showExpenseHistory = false 
            selectedTab == 1 && assetsCurrentView != "Main" -> assetsCurrentView = "Main"
            selectedTab != 0 -> selectedTab = 0 
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
                        
                        // PARSE PROFILE DATA HERE
                        val profileObj = jsonResponse.optJSONObject("profile")
                        var tempName = ""
                        var tempEmail = ""
                        var tempMobile = ""
                        var tempPass = ""
                        var tempDob = ""
                        
                        if (profileObj != null) {
                            tempName = profileObj.optString("name", "")
                            tempEmail = profileObj.optString("email", "")
                            tempMobile = profileObj.optString("mobile", "")
                            tempPass = profileObj.optString("password", "")
                            tempDob = profileObj.optString("dob", "")
                        }

                        val expensesArray = jsonResponse.optJSONArray("expenses")
                        var tempTotal = 0.0; var tempMonth = 0.0; var tempYear = 0.0
                        val tempHistory = mutableListOf<TransactionModel>()

                        if (expensesArray != null && expensesArray.length() > 0) {
                            val currMonthStr = String.format(Locale.US, "%02d", currM)
                            val currYearStr = currY.toString()
                            for (i in 0 until expensesArray.length()) {
                                val item = expensesArray.getJSONObject(i)
                                val rawDate = item.optString("date", "").trim()
                                val rawAmt = item.optString("amount", "0")
                                val amt = rawAmt.replace("[^\\d.]".toRegex(), "").toDoubleOrNull() ?: item.optDouble("amount", 0.0)
                                if (amt > 0.0) {
                                    tempTotal += amt 
                                    tempHistory.add(TransactionModel(rawDate, amt, item.optString("category", "Unknown"), item.optString("detail1", ""), item.optString("detail2", ""), item.optString("mode", "")))
                                    if (rawDate.contains(currYearStr)) {
                                        tempYear += amt
                                        if (rawDate.contains("-$currMonthStr-") || rawDate.contains("/$currMonthStr/") || rawDate.startsWith("$currMonthStr-") || rawDate.startsWith("$currMonthStr/")) tempMonth += amt
                                    }
                                }
                            }
                        }

                        val invArray = jsonResponse.optJSONArray("investments")
                        val fetchedInvList = mutableListOf<InvestmentItem>()
                        if (invArray != null) {
                            for (i in 0 until invArray.length()) {
                                val item = invArray.getJSONObject(i)
                                fetchedInvList.add(InvestmentItem(item.optString("asset_name", ""), item.optDouble("quantity", 0.0), item.optDouble("buy_price", 0.0), item.optDouble("current_price", item.optDouble("buy_price", 0.0)), item.optDouble("one_day_change", 0.0)))
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
                                        expQtrInt = item.optDouble("exp_qtr_int", 0.0),
                                        accruedQtrInt = item.optDouble("accrued_qtr_int", 0.0),
                                        expYrInt = item.optDouble("exp_yr_int", 0.0),
                                        accruedYrInt = item.optDouble("accrued_yr_int", 0.0),
                                        oneDayInt = item.optDouble("one_day_int", 0.0)
                                    )
                                )
                            }
                        }

                        withContext(Dispatchers.Main) {
                            // Assign profile details to States
                            userFullName = tempName
                            userEmail = tempEmail
                            userMobile = tempMobile
                            userPassword = tempPass
                            userDob = tempDob

                            thisMonthExpenses = if (tempMonth > 0) tempMonth else tempTotal 
                            thisYearExpenses = if (tempYear > 0) tempYear else tempTotal
                            transactionList = tempHistory.reversed()
                            investmentList = fetchedInvList
                            bankList = fetchedBankList
                            isLoadingExpenses = false
                        }
                    } else { withContext(Dispatchers.Main) { isLoadingExpenses = false } }
                } else { withContext(Dispatchers.Main) { isLoadingExpenses = false } }
            } catch (e: Exception) { withContext(Dispatchers.Main) { isLoadingExpenses = false } }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                    NavigationBarItem(
                        selected = selectedTab == 0 && !showExpenseHistory,
                        onClick = { selectedTab = 0; showExpenseHistory = false },
                        icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") }, label = { Text("Home") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { 
                            if (selectedTab == 1) assetsCurrentView = "Main"
                            selectedTab = 1; showExpenseHistory = false
                        },
                        icon = { Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = "Assets") }, label = { Text("Assets") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                    )
                    
                    NavigationBarItem(
                        selected = false,
                        onClick = { showAddMenu = !showAddMenu },
                        icon = { 
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF2E7D32)), 
                                contentAlignment = Alignment.Center
                            ) { 
                                val rotation by animateFloatAsState(targetValue = if (showAddMenu) 45f else 0f, label = "iconRotate")
                                Icon(Icons.Outlined.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.rotate(rotation)) 
                            } 
                        }
                    )
                    
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3; showExpenseHistory = false },
                        icon = { Icon(Icons.Outlined.PieChart, contentDescription = "Analytics") }, label = { Text("Analytics") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4; showExpenseHistory = false },
                        icon = { Icon(Icons.Outlined.Person, contentDescription = "Profile") }, label = { Text("Profile") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                    )
                }
            }
        ) { paddingValues ->
            Crossfade(targetState = Pair(selectedTab, showExpenseHistory), animationSpec = tween(400), label = "Screen Transition") { state ->
                val (currentTab, isHistoryVisible) = state
                
                if (isHistoryVisible) {
                    com.kartikey.rupeeflow.UI_Screens.Home.ExpenseHistoryScreen(paddingValues = paddingValues, history = transactionList, onBackClick = { showExpenseHistory = false })
                } else {
                    when (currentTab) {
                        0 -> HomeDashboardDesign(username = username, paddingValues = paddingValues, thisMonthExpenses = thisMonthExpenses, thisYearExpenses = thisYearExpenses, isLoadingExpenses = isLoadingExpenses, dNavState = dNavState, dBackPresses = dBackPresses, onLogout = onLogout, onRefreshExpenses = { refreshTrigger++ }, onExpenseCardClick = { showExpenseHistory = true })
                        1 -> AssetsScreen(
                            paddingValues = paddingValues, username = username, investmentList = investmentList, bankList = bankList, isLoading = isLoadingExpenses, onRefreshClick = { refreshTrigger++ },
                            currentView = assetsCurrentView, onViewChange = { assetsCurrentView = it },
                            onEditBankClick = { bankToEdit = it }
                        )
                        3 -> AnalyticsScreen(paddingValues = paddingValues)
                        4 -> ProfileScreen(
                            username = username, 
                            name = userFullName, 
                            email = userEmail,
                            mobile = userMobile,
                            password = userPassword,
                            dob = userDob,
                            paddingValues = paddingValues, 
                            onLogout = onLogout,
                            onProfileRefresh = { refreshTrigger++ } // Add this so edit triggers a reload
                        )
                    }
                }
            }
        }

        AddScreen(
            username = username,
            showMenu = showAddMenu,
            onCloseMenu = { showAddMenu = false },
            onExpenseAdded = { newEntry -> 
                transactionList = listOf(newEntry) + transactionList 
            },
            onInvestmentAdded = { refreshTrigger++ },
            onFinanceAdded = { refreshTrigger++ }
        )

        if (bankToEdit != null) {
            EditBankDialog(
                bank = bankToEdit!!,
                username = username,
                onDismiss = { bankToEdit = null },
                onUpdateSuccess = { 
                    bankToEdit = null 
                    refreshTrigger++ 
                }
            )
        }
    }
}
