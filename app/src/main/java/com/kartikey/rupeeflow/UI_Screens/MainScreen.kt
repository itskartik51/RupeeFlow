package com.kartikey.rupeeflow.UI_Screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kartikey.rupeeflow.Cloud_Database.Constants

// HOMESCREEN AUR ADD EXPENSE KE IMPORTS
import com.kartikey.rupeeflow.UI_Screens.Home.HomeDashboardDesign
import com.kartikey.rupeeflow.UI_Screens.AddExpense.ExpenseAddScreen
import com.kartikey.rupeeflow.UI_Screens.AddExpense.ExpenseHistoryScreen
import com.kartikey.rupeeflow.UI_Screens.AddExpense.TransactionModel

// NAYE 3 PAGES KE IMPORTS (Jo aapne abhi banaye hain)
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
    
    // NAYA DIAGNOSTICS: Navigation aur Back Button ko track karega
    var dNavState by remember { mutableStateOf("Home") }
    var dBackPresses by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedTab, showExpenseHistory) {
        dNavState = if (showExpenseHistory) "Expense History" else "Tab $selectedTab"
    }

    // BACK BUTTON LOGIC: App band hone se rokenge
    BackHandler(enabled = showExpenseHistory || selectedTab != 0) {
        dBackPresses++ 
        if (showExpenseHistory) {
            showExpenseHistory = false 
        } else if (selectedTab != 0) {
            selectedTab = 0 
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
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
        // YAHI HAI WO NAVIGATION LOGIC JO ABHI AAPNE PUCHA THA
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
            AssetsScreen(paddingValues = paddingValues)
        } else if (selectedTab == 2) {
            ExpenseAddScreen(username = username, paddingValues = paddingValues)
        } else if (selectedTab == 3) {
            AnalyticsScreen(paddingValues = paddingValues)
        } else if (selectedTab == 4) {
            ProfileScreen(username = username, paddingValues = paddingValues, onLogout = onLogout)
        }
    }
}
