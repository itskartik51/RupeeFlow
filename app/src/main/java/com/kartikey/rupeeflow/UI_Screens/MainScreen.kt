package com.kartikey.rupeeflow.UI_Screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kartikey.rupeeflow.Cloud_Database.Constants
import com.kartikey.rupeeflow.UI_Screens.Home.HomeDashboardDesign
import com.kartikey.rupeeflow.UI_Screens.Add.AddScreen 
import com.kartikey.rupeeflow.UI_Screens.Add.TransactionModel
import com.kartikey.rupeeflow.UI_Screens.Assets.AssetsScreen
import com.kartikey.rupeeflow.UI_Screens.Assets.InvestmentItem
import com.kartikey.rupeeflow.UI_Screens.Assets.BankAccountItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.CashItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.CreditCardItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.FDItem
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
    var showContriScreen by remember { mutableStateOf(false) }
    var assetsCurrentView by remember { mutableStateOf("Main") }

    var bankToEdit by remember { mutableStateOf<BankAccountItem?>(null) }
    var ccToEdit by remember { mutableStateOf<CreditCardItem?>(null) }
    var fdToEdit by remember { mutableStateOf<FDItem?>(null) }
    
    var expenseToEdit by remember { mutableStateOf<TransactionModel?>(null) }
    var expenseToDelete by remember { mutableStateOf<TransactionModel?>(null) }
    
    var showAddMenu by remember { mutableStateOf(false) }

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
    var fdList by remember { mutableStateOf(emptyList<FDItem>()) }
    var ccList by remember { mutableStateOf(emptyList<CreditCardItem>()) }
    var cashData by remember { mutableStateOf(CashItem(0.0, "")) }
    
    var dNavState by remember { mutableStateOf("Connecting to Sheet...") }
    var dBackPresses by remember { mutableIntStateOf(0) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedTab, showExpenseHistory, showContriScreen, isLoadingExpenses, transactionList.size, bankToEdit, ccToEdit, fdToEdit, showAddMenu) {
        if (showAddMenu) dNavState = "Add Menu Open"
        else if (bankToEdit != null || ccToEdit != null || fdToEdit != null || expenseToEdit != null) dNavState = "Editing Vault"
        else if (showContriScreen) dNavState = "Contri Hub"
        else if (showExpenseHistory) dNavState = "Expense History"
        else dNavState = if (isLoadingExpenses) "Syncing Data... ⏳" else "Tab $selectedTab ✅"
    }

    BackHandler(enabled = showContriScreen || showExpenseHistory || selectedTab != 0 || assetsCurrentView != "Main" || bankToEdit != null || ccToEdit != null || fdToEdit != null || expenseToEdit != null || expenseToDelete != null) {
        dBackPresses++ 
        when {
            expenseToDelete != null -> expenseToDelete = null
            expenseToEdit != null -> expenseToEdit = null
            bankToEdit != null -> bankToEdit = null 
            ccToEdit != null -> ccToEdit = null
            fdToEdit != null -> fdToEdit = null
            showContriScreen -> showContriScreen = false
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

                val json = JSONObject().apply { 
                    put("action", "get_all_data")
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
                                val amt = rawAmt.replace("[^\\d.]".toRegex(), "").toDoubleOrNull() ?: item.optDouble("amount", 0.0)
                                
                                if (amt > 0.0) {
                                    tempTotal += amt 
                                    tempHistory.add(
                                        TransactionModel(
                                            date = rawDate, 
                                            amount = amt, 
                                            category = item.optString("category", "Unknown"), 
                                            remark1 = item.optString("detail1", ""), 
                                            remark2 = item.optString("detail2", ""), 
                                            mode = item.optString("mode", ""), 
                                            sourceType = item.optString("source_type", ""), 
                                            sourceId = item.optString("source_id", "")
                                        )
                                    )
                                    
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
                                        expQtrInt = item.optDouble("exp_qtr_int", 0.0), 
                                        accruedQtrInt = item.optDouble("accrued_qtr_int", 0.0), 
                                        expYrInt = item.optDouble("exp_yr_int", 0.0), 
                                        accruedYrInt = item.optDouble("accrued_yr_int", 0.0), 
                                        oneDayInt = item.optDouble("one_day_int", 0.0)
                                    )
                                ) 
                            }
                        }

                        val cashObj = jsonResponse.optJSONObject("cash")
                        var fetchedCash = CashItem(0.0, "")
                        if (cashObj != null) { 
                            fetchedCash = CashItem(
                                amount = cashObj.optDouble("amount", 0.0), 
                                lastUpdated = cashObj.optString("last_updated", "")
                            ) 
                        }

                        val fdArray = jsonResponse.optJSONArray("fds")
                        val fetchedFDList = mutableListOf<FDItem>()
                        
                        if (fdArray != null) {
                            for (i in 0 until fdArray.length()) { 
                                val item = fdArray.getJSONObject(i)
                                fetchedFDList.add(
                                    FDItem(
                                        bankName = item.optString("bank_name", ""), 
                                        accountNo = item.optString("account_no", ""), 
                                        createDate = item.optString("create_date", ""), 
                                        maturityDate = item.optString("maturity_date", ""), 
                                        daysToMaturity = item.optInt("days_to_maturity", 0), 
                                        investedAmt = item.optDouble("invested_amt", 0.0), 
                                        interestRate = item.optDouble("interest_rate", 0.0), 
                                        maturityValue = item.optDouble("maturity_value", 0.0), 
                                        accruedValue = item.optDouble("accrued_value", 0.0), 
                                        accruedInt = item.optDouble("accrued_int", 0.0), 
                                        oneDayInt = item.optDouble("one_day_int", 0.0)
                                    )
                                ) 
                            }
                        }

                        val ccArray = jsonResponse.optJSONArray("credit_cards")
                        val fetchedCCList = mutableListOf<CreditCardItem>()
                        
                        if (ccArray != null) {
                            for (i in 0 until ccArray.length()) { 
                                val item = ccArray.getJSONObject(i)
                                fetchedCCList.add(
                                    CreditCardItem(
                                        issuer = item.optString("issuer", ""), 
                                        cardNo = item.optString("card_no", ""), 
                                        type = item.optString("type", ""), 
                                        limit = item.optDouble("limit", 0.0), 
                                        outstanding = item.optDouble("outstanding", 0.0), 
                                        available = item.optDouble("available", 0.0), 
                                        utilization = item.optDouble("utilization", 0.0), 
                                        cibilStatus = item.optString("cibil_status", ""), 
                                        billingDay = item.optInt("billing_day", 0), 
                                        dueDay = item.optInt("due_day", 0), 
                                        reminderDay = item.optInt("reminder_day", 0), 
                                        annualFee = item.optDouble("annual_fee", 0.0), 
                                        joiningFee = item.optDouble("joining_fee", 0.0), 
                                        lastUsed = item.optString("last_used", "")
                                    )
                                ) 
                            }
                        }

                        withContext(Dispatchers.Main) {
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
                            cashData = fetchedCash
                            fdList = fetchedFDList
                            ccList = fetchedCCList
                            
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                    NavigationBarItem(
                        selected = selectedTab == 0 && !showExpenseHistory && !showContriScreen, 
                        onClick = { selectedTab = 0; showExpenseHistory = false; showContriScreen = false }, 
                        icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") }, 
                        label = { Text("Home") }, 
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1, 
                        onClick = { 
                            if (selectedTab == 1) assetsCurrentView = "Main"
                            selectedTab = 1; showExpenseHistory = false; showContriScreen = false 
                        }, 
                        icon = { Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = "Assets") }, 
                        label = { Text("Assets") }, 
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                    )
                    NavigationBarItem(
                        selected = false, 
                        onClick = { showAddMenu = !showAddMenu }, 
                        icon = { Spacer(modifier = Modifier.size(48.dp)) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3, 
                        onClick = { selectedTab = 3; showExpenseHistory = false; showContriScreen = false }, 
                        icon = { Icon(Icons.Outlined.PieChart, contentDescription = "Analytics") }, 
                        label = { Text("Analytics") }, 
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4, 
                        onClick = { selectedTab = 4; showExpenseHistory = false; showContriScreen = false }, 
                        icon = { Icon(Icons.Outlined.Person, contentDescription = "Profile") }, 
                        label = { Text("Profile") }, 
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                    )
                }
            }
        ) { paddingValues ->
            AnimatedContent(
                targetState = Triple(selectedTab, showExpenseHistory, showContriScreen),
                transitionSpec = {
                    val targetIsSecondary = targetState.second || targetState.third
                    val initialIsSecondary = initialState.second || initialState.third
                    
                    if (targetIsSecondary && !initialIsSecondary) {
                        (slideInHorizontally(animationSpec = tween(250)) { width -> width } + fadeIn(tween(250))).togetherWith(
                            slideOutHorizontally(animationSpec = tween(250)) { width -> -width / 2 } + fadeOut(tween(250))
                        )
                    } else if (!targetIsSecondary && initialIsSecondary) {
                        (slideInHorizontally(animationSpec = tween(250)) { width -> -width / 2 } + fadeIn(tween(250))).togetherWith(
                            slideOutHorizontally(animationSpec = tween(250)) { width -> width } + fadeOut(tween(250))
                        )
                    } else {
                        fadeIn(tween(250)) togetherWith fadeOut(tween(250))
                    }
                }, 
                label = "Screen Transition"
            ) { state ->
                val (currentTab, isHistoryVisible, isContriVisible) = state
                if (isContriVisible) {
                    com.kartikey.rupeeflow.UI_Screens.Home.ContriScreen(
                        paddingValues = paddingValues,
                        onBackClick = { showContriScreen = false }
                    )
                } else if (isHistoryVisible) {
                    com.kartikey.rupeeflow.UI_Screens.Home.ExpenseHistoryScreen(
                        paddingValues = paddingValues, 
                        history = transactionList, 
                        isLoading = isLoadingExpenses,
                        onRefreshClick = { refreshTrigger++ }, 
                        onBackClick = { showExpenseHistory = false },
                        onEditClick = { expenseToEdit = it }, 
                        onDeleteClick = { expenseToDelete = it }
                    )
                } else {
                    when (currentTab) {
                        0 -> HomeDashboardDesign(
                            username = username, 
                            paddingValues = paddingValues, 
                            thisMonthExpenses = thisMonthExpenses, 
                            thisYearExpenses = thisYearExpenses, 
                            isLoadingExpenses = isLoadingExpenses, 
                            dNavState = dNavState, 
                            dBackPresses = dBackPresses, 
                            onLogout = onLogout, 
                            onRefreshExpenses = { refreshTrigger++ }, 
                            onExpenseCardClick = { showExpenseHistory = true },
                            onContriClick = { showContriScreen = true }
                        )
                        1 -> AssetsScreen(
                            paddingValues = paddingValues, 
                            username = username, 
                            investmentList = investmentList, 
                            bankList = bankList, 
                            fdList = fdList, 
                            ccList = ccList, 
                            cashData = cashData, 
                            isLoading = isLoadingExpenses, 
                            onRefreshClick = { refreshTrigger++ }, 
                            currentView = assetsCurrentView, 
                            onViewChange = { assetsCurrentView = it }, 
                            onEditBankClick = { bankToEdit = it }, 
                            onEditCCClick = { ccToEdit = it }, 
                            onEditFDClick = { fdToEdit = it }
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
                            onProfileRefresh = { refreshTrigger++ }
                        )
                    }
                }
            }
        }

        AddScreen(
            username = username, 
            showMenu = showAddMenu, 
            onToggleMenu = { showAddMenu = !showAddMenu }, 
            onExpenseAdded = { newEntry -> transactionList = listOf(newEntry) + transactionList }, 
            onInvestmentAdded = { refreshTrigger++ }, 
            onFinanceAdded = { refreshTrigger++ }, 
            bankList = bankList, 
            ccList = ccList, 
            cashData = cashData
        )

        if (bankToEdit != null) { 
            EditBankDialog(bank = bankToEdit!!, username = username, onDismiss = { bankToEdit = null }, onUpdateSuccess = { bankToEdit = null; refreshTrigger++ }) 
        }
        if (ccToEdit != null) { 
            EditCreditCardDialog(cc = ccToEdit!!, username = username, onDismiss = { ccToEdit = null }, onUpdateSuccess = { ccToEdit = null; refreshTrigger++ }) 
        }
        if (fdToEdit != null) { 
            EditFDDialog(fd = fdToEdit!!, username = username, onDismiss = { fdToEdit = null }, onUpdateSuccess = { fdToEdit = null; refreshTrigger++ }) 
        }
        
        if (expenseToDelete != null) { 
            DeleteExpenseDialog(expense = expenseToDelete!!, username = username, onDismiss = { expenseToDelete = null }, onSuccess = { expenseToDelete = null; refreshTrigger++ }) 
        }
        if (expenseToEdit != null) { 
            EditExpenseDialog(expense = expenseToEdit!!, username = username, bankList = bankList, ccList = ccList, onDismiss = { expenseToEdit = null }, onSuccess = { expenseToEdit = null; refreshTrigger++ }) 
        }
    }
}
