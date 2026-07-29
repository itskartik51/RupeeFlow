package com.kartikey.rupeeflow.UI_Screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

@Composable
fun MainScreen(username: String, onLogout: () -> Unit) {
    val context = LocalContext.current

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
    var openProfileDetails by remember { mutableStateOf(false) } 

    var userFullName by remember { mutableStateOf("") } 
    var userEmail by remember { mutableStateOf("") } 
    var userMobile by remember { mutableStateOf("") }
    var userPassword by remember { mutableStateOf("") }
    var userDob by remember { mutableStateOf("") }

    var thisMonthExpenses by remember { mutableDoubleStateOf(0.0) }
    var thisYearExpenses by remember { mutableDoubleStateOf(0.0) }
    var isLoadingExpenses by remember { mutableStateOf(true) }
    var budgetLimit by remember { mutableDoubleStateOf(0.0) }

    var transactionList by remember { mutableStateOf(emptyList<TransactionModel>()) }
    var investmentList by remember { mutableStateOf(emptyList<InvestmentItem>()) }
    var bankList by remember { mutableStateOf(emptyList<BankAccountItem>()) } 
    var fdList by remember { mutableStateOf(emptyList<FDItem>()) }
    var ccList by remember { mutableStateOf(emptyList<CreditCardItem>()) }
    var cashData by remember { mutableStateOf(CashItem(0.0, "")) }
    var contriRoomsList by remember { mutableStateOf(emptyList<com.kartikey.rupeeflow.UI_Screens.Home.ContriRoomModel>()) }
    
    var dNavState by remember { mutableStateOf("Connecting to Sheet...") }
    var dBackPresses by remember { mutableIntStateOf(0) }
    
    // Refresh Triggers
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var forceFetchNext by remember { mutableStateOf(false) } // Sirf Contri ke liye flag

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

        // Force fetch flag capture kiya aur reset kar diya
        val shouldForceRefresh = forceFetchNext
        forceFetchNext = false 

        val cachedData = CacheManager.getCachedData(context, username)
        if (cachedData != null && transactionList.isEmpty()) {
            userFullName = cachedData.userFullName
            userEmail = cachedData.userEmail
            userMobile = cachedData.userMobile
            userPassword = cachedData.userPassword
            userDob = cachedData.userDob
            thisMonthExpenses = cachedData.thisMonthExpenses
            thisYearExpenses = cachedData.thisYearExpenses
            budgetLimit = cachedData.budgetLimit
            transactionList = cachedData.transactionList
            investmentList = cachedData.investmentList
            bankList = cachedData.bankList
            cashData = cachedData.cashData
            fdList = cachedData.fdList
            ccList = cachedData.ccList
            contriRoomsList = cachedData.contriRoomsList
        }

        launch(Dispatchers.IO) {
            // Agar Contri se refresh hua hai to shouldForceRefresh true jayega, baki sab me false
            val freshData = CacheManager.fetchAndCacheData(context, username, forceRefresh = shouldForceRefresh)
            withContext(Dispatchers.Main) {
                if (freshData != null) {
                    userFullName = freshData.userFullName
                    userEmail = freshData.userEmail
                    userMobile = freshData.userMobile
                    userPassword = freshData.userPassword
                    userDob = freshData.userDob
                    thisMonthExpenses = freshData.thisMonthExpenses
                    thisYearExpenses = freshData.thisYearExpenses
                    budgetLimit = freshData.budgetLimit
                    transactionList = freshData.transactionList
                    investmentList = freshData.investmentList
                    bankList = freshData.bankList
                    cashData = freshData.cashData
                    fdList = freshData.fdList
                    ccList = freshData.ccList
                    contriRoomsList = freshData.contriRoomsList
                }
                isLoadingExpenses = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                    NavigationBarItem(
                        modifier = Modifier.bounceClick {
                            selectedTab = 0
                            showExpenseHistory = false
                            showContriScreen = false
                        },
                        selected = selectedTab == 0 && !showExpenseHistory && !showContriScreen, 
                        onClick = { }, // Click bounceClick modifier se handle hoga
                        icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") }, 
                        label = { Text("Home") }, 
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                    )
                    NavigationBarItem(
                        modifier = Modifier.bounceClick {
                            if (selectedTab == 1) assetsCurrentView = "Main"
                            selectedTab = 1
                            showExpenseHistory = false
                            showContriScreen = false 
                        },
                        selected = selectedTab == 1, 
                        onClick = { }, // Click bounceClick modifier se handle hoga
                        icon = { Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = "Assets") }, 
                        label = { Text("Assets") }, 
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                    )
                    NavigationBarItem(
                        modifier = Modifier.bounceClick {
                            showAddMenu = !showAddMenu
                        },
                        selected = false, 
                        onClick = { }, // Click bounceClick modifier se handle hoga
                        icon = { Spacer(modifier = Modifier.size(48.dp)) }
                    )
                    NavigationBarItem(
                        modifier = Modifier.bounceClick {
                            selectedTab = 3
                            showExpenseHistory = false
                            showContriScreen = false
                        },
                        selected = selectedTab == 3, 
                        onClick = { }, // Click bounceClick modifier se handle hoga
                        icon = { Icon(Icons.Outlined.PieChart, contentDescription = "Analytics") }, 
                        label = { Text("Analytics") }, 
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), indicatorColor = Color(0xFFE8F5E9))
                    )
                    NavigationBarItem(
                        modifier = Modifier.bounceClick {
                            selectedTab = 4
                            showExpenseHistory = false
                            showContriScreen = false
                        },
                        selected = selectedTab == 4, 
                        onClick = { }, // Click bounceClick modifier se handle hoga
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
                        username = username,
                        contriRooms = contriRoomsList,
                        paddingValues = paddingValues,
                        onBackClick = { showContriScreen = false },
                        onRefresh = { 
                            // YAHAN SIRF CONTRI ME FORCE REFRESH HOGA
                            forceFetchNext = true
                            refreshTrigger++ 
                        } 
                    )
                } else if (isHistoryVisible) {
                    com.kartikey.rupeeflow.UI_Screens.Home.ExpenseHistoryScreen(
                        paddingValues = paddingValues, 
                        history = transactionList, 
                        isLoading = isLoadingExpenses,
                        onRefreshClick = { refreshTrigger++ }, // Normal Fast Cache Refresh
                        onBackClick = { showExpenseHistory = false },
                        onEditClick = { expenseToEdit = it }, 
                        onDeleteClick = { expenseToDelete = it }
                    )
                } else {
                    when (currentTab) {
                        0 -> {
                            val totalInv = investmentList.sumOf { it.quantity * it.currentPrice }
                            val totalBank = bankList.sumOf { it.currentBalance }
                            
                            HomeDashboardDesign(
                                username = username, 
                                userFullName = userFullName, 
                                paddingValues = paddingValues, 
                                thisMonthExpenses = thisMonthExpenses, 
                                thisYearExpenses = thisYearExpenses, 
                                budgetLimit = budgetLimit,
                                isLoadingExpenses = isLoadingExpenses, 
                                dNavState = dNavState, 
                                dBackPresses = dBackPresses, 
                                onLogout = onLogout, 
                                onRefreshExpenses = { refreshTrigger++ }, // Normal Fast Cache Refresh
                                onExpenseCardClick = { showExpenseHistory = true },
                                onContriClick = { showContriScreen = true },
                                onAvatarClick = { 
                                    selectedTab = 4 
                                    openProfileDetails = true 
                                },
                                contriCount = contriRoomsList.size,
                                totalInvestment = totalInv,
                                totalBankBalance = totalBank,
                                onInvestmentClick = {
                                    assetsCurrentView = "InvestmentDetails"
                                    selectedTab = 1
                                },
                                onBankClick = {
                                    assetsCurrentView = "DirectBankAccounts"
                                    selectedTab = 1
                                },
                                onBudgetSaved = { refreshTrigger++ } 
                            )
                        }
                        1 -> AssetsScreen(
                            paddingValues = paddingValues, 
                            username = username, 
                            investmentList = investmentList, 
                            bankList = bankList, 
                            fdList = fdList, 
                            ccList = ccList, 
                            cashData = cashData, 
                            isLoading = isLoadingExpenses, 
                            onRefreshClick = { refreshTrigger++ }, // Normal Fast Cache Refresh
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
                            onProfileRefresh = { refreshTrigger++ }, // Normal Fast Cache Refresh
                            startInDetails = openProfileDetails,
                            onResetDetailsState = { openProfileDetails = false }
                        )
                    }
                }
            }
        }

        AddScreen(
            username = username, 
            showMenu = showAddMenu, 
            onToggleMenu = { showAddMenu = !showAddMenu }, 
            // OPTIMISTIC UI: Expense instantly list me upar append hoga
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
            DeleteExpenseDialog(
                expense = expenseToDelete!!, 
                username = username, 
                onDismiss = { expenseToDelete = null }, 
                onSuccess = { 
                    // OPTIMISTIC UI: Expense list se instantly remove hoga (0ms wait)
                    val targetDate = expenseToDelete?.date
                    expenseToDelete = null
                    if(targetDate != null) {
                        transactionList = transactionList.filter { it.date != targetDate }
                    }
                    refreshTrigger++ 
                }
            ) 
        }
        if (expenseToEdit != null) { 
            EditExpenseDialog(expense = expenseToEdit!!, username = username, bankList = bankList, ccList = ccList, onDismiss = { expenseToEdit = null }, onSuccess = { expenseToEdit = null; refreshTrigger++ }) 
        }
    }
}

// ==========================================
// CUSTOM APPLE-STYLE BOUNCE CLICK ANIMATION
// ==========================================
fun Modifier.bounceClick(
    scaleDown: Float = 0.90f,
    onClick: () -> Unit
) = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "BounceAnimation"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(isPressed) {
            awaitPointerEventScope {
                isPressed = if (isPressed) {
                    waitForUpOrCancellation()
                    false
                } else {
                    awaitFirstDown(false)
                    true
                }
            }
        }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null, // Yeh purana Android Ripple effect (gol ghera) hatayega
            onClick = onClick
        )
}
