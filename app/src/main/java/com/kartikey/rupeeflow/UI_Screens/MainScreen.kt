package com.kartikey.rupeeflow.UI_Screens

import androidx.activity.compose.BackHandler
// NAYE IMPORTS ANIMATION AUR HAPTICS KE LIYE
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.kartikey.rupeeflow.Cloud_Database.Constants

// HOMESCREEN AUR EXPENSE HISTORY KE IMPORTS
import com.kartikey.rupeeflow.UI_Screens.Home.HomeDashboardDesign
import com.kartikey.rupeeflow.UI_Screens.AddExpense.ExpenseHistoryScreen
import com.kartikey.rupeeflow.UI_Screens.AddExpense.TransactionModel

// ADD SCREEN IMPORT
import com.kartikey.rupeeflow.UI_Screens.Add.AddScreen

// BAAKI PAGES KE IMPORTS
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

    var refreshTrigger by remember { mutableIntStateOf(0) }
    
    // HAPTIC FEEDBACK CONTROLLER
    val haptic = LocalHapticFeedback.current

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

    // COMBO API CALL (Phase 1)
    LaunchedEffect(refreshTrigger) {
        isLoadingExpenses = true
        isLoadingInvestments = true
        
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
                                val category = item.optString("category", "Unknown")
                                val detail1 = item.optString("detail1", "")
                                val detail2 = item.optString("detail2", "")
                                
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

                        val investmentsArray = jsonResponse.optJSONArray("investments")
                        val tempInvCount = investmentsArray?.length() ?: 0

                        withContext(Dispatchers.Main) {
                            thisMonthExpenses = if (tempMonth > 0) tempMonth else tempTotal 
                            thisYearExpenses = if (tempYear > 0) tempYear else tempTotal
                            transactionList = tempHistory.reversed()
                            investmentCount = tempInvCount
                            
                            isLoadingExpenses = false
                            isLoadingInvestments = false
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            isLoadingExpenses = false
                            isLoadingInvestments = false
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isLoadingExpenses = false
                        isLoadingInvestments = false
                    }
                }
            } catch (e: Exception) { 
                withContext(Dispatchers.Main) { 
                    isLoadingExpenses = false
                    isLoadingInvestments = false
                } 
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                NavigationBarItem(
                    selected = selectedTab == 0 && !showExpenseHistory,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress) // NAYA: Vibrate on Click
                        selectedTab = 0; showExpenseHistory = false 
                    },
                    icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") }, 
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedTab = 1; showExpenseHistory = false 
                    },
                    icon = { Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = "Assets") }, 
                    label = { Text("Assets") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedTab = 2; showExpenseHistory = false 
                    },
                    icon = {
                        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF2E7D32)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Add, contentDescription = "Add", tint = Color.White) 
                        }
                    }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedTab = 3; showExpenseHistory = false 
                    },
                    icon = { Icon(Icons.Outlined.PieChart, contentDescription = "Analytics") }, 
                    label = { Text("Analytics") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedTab = 4; showExpenseHistory = false 
                    },
                    icon = { Icon(Icons.Outlined.Person, contentDescription = "Profile") }, 
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                )
            }
        }
    ) { paddingValues ->
        // NAYA: Crossfade Animation for smooth screen transitions
        Crossfade(
            targetState = Pair(selectedTab, showExpenseHistory), 
            animationSpec = tween(durationMillis = 400),
            label = "Screen Transition"
        ) { state ->
            val (currentTab, isHistoryVisible) = state
            
            if (isHistoryVisible) {
                ExpenseHistoryScreen(
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
                    1 -> AssetsScreen(paddingValues = paddingValues, username = username)
                    2 -> AddScreen(paddingValues = paddingValues, username = username)
                    3 -> AnalyticsScreen(paddingValues = paddingValues)
                    4 -> ProfileScreen(username = username, paddingValues = paddingValues, onLogout = onLogout)
                }
            }
        }
    }
}
