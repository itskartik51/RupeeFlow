package com.kartikey.rupeeflow.UI_Screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import com.kartikey.rupeeflow.UI_Screens.Profile.checkIsUpdateAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MainScreen(
    username: String, 
    themeMode: Int, 
    onThemeChange: (Int) -> Unit, 
    onLogout: () -> Unit
) {
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
    var profilePicUrl by remember { mutableStateOf("") } // 🚀 NEW
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
    
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var forceFetchNext by remember { mutableStateOf(false) } 
    
    var isUpdateAvailable by remember { mutableStateOf(false) } 

    LaunchedEffect(Unit) {
        isUpdateAvailable = checkIsUpdateAvailable(context)
    }

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
        val shouldForceRefresh = forceFetchNext
        forceFetchNext = false 

        val cachedData = CacheManager.getCachedData(context, username)
        if (cachedData != null && transactionList.isEmpty()) {
            userFullName = cachedData.userFullName
            userEmail = cachedData.userEmail
            userMobile = cachedData.userMobile
            profilePicUrl = cachedData.profilePicUrl // 🚀 NEW
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
            val freshData = CacheManager.fetchAndCacheData(context, username, forceRefresh = shouldForceRefresh)
            withContext(Dispatchers.Main) {
                if (freshData != null) {
                    userFullName = freshData.userFullName
                    userEmail = freshData.userEmail
                    userMobile = freshData.userMobile
                    profilePicUrl = freshData.profilePicUrl // 🚀 NEW
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
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface, 
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        modifier = Modifier.bounceClick(),
                        selected = selectedTab == 0 && !showExpenseHistory && !showContriScreen, 
                        onClick = { selectedTab = 0; showExpenseHistory = false; showContriScreen = false }, 
                        icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") }, 
                        label = { Text("Home") }, 
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary, 
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    NavigationBarItem(
                        modifier = Modifier.bounceClick(),
                        selected = selectedTab == 1, 
                        onClick = { if (selectedTab == 1) assetsCurrentView = "Main"; selectedTab = 1; showExpenseHistory = false; showContriScreen = false }, 
                        icon = { Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = "Assets") }, 
                        label = { Text("Assets") }, 
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary, 
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    NavigationBarItem(
                        modifier = Modifier.bounceClick(),
                        selected = false, 
                        onClick = { showAddMenu = !showAddMenu }, 
                        icon = { Spacer(modifier = Modifier.size(48.dp)) }
                    )
                    NavigationBarItem(
                        modifier = Modifier.bounceClick(),
                        selected = selectedTab == 3, 
                        onClick = { selectedTab = 3; showExpenseHistory = false; showContriScreen = false }, 
                        icon = { Icon(Icons.Outlined.PieChart, contentDescription = "Analytics") }, 
                        label = { Text("Analytics") }, 
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary, 
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    NavigationBarItem(
                        modifier = Modifier.bounceClick(),
                        selected = selectedTab == 4, 
                        onClick = { selectedTab = 4; showExpenseHistory = false; showContriScreen = false }, 
                        icon = { 
                            Box {
                                Icon(Icons.Outlined.Person, contentDescription = "Profile")
                                if (isUpdateAvailable) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .align(Alignment.TopEnd)
                                            .offset(x = 2.dp, y = (-2).dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    )
                                }
                            }
                        }, 
                        label = { Text("Profile") }, 
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary, 
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        ) { paddingValues ->
            AnimatedContent(
                targetState = Triple(selectedTab, showExpenseHistory, showContriScreen),
                transitionSpec = {
                    fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)).togetherWith(
                        fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing))
                    )
                }, 
                label = "Clean Fade Screen Transition"
            ) { state ->
                val (currentTab, isHistoryVisible, isContriVisible) = state
                if (isContriVisible) {
                    com.kartikey.rupeeflow.UI_Screens.Home.ContriScreen(
                        username = username, contriRooms = contriRoomsList, paddingValues = paddingValues,
                        onBackClick = { showContriScreen = false }, onRefresh = { forceFetchNext = true; refreshTrigger++ } 
                    )
                } else if (isHistoryVisible) {
                    com.kartikey.rupeeflow.UI_Screens.Home.ExpenseHistoryScreen(
                        paddingValues = paddingValues, history = transactionList, isLoading = isLoadingExpenses,
                        onRefreshClick = { refreshTrigger++ }, onBackClick = { showExpenseHistory = false },
                        onEditClick = { expenseToEdit = it }, onDeleteClick = { expenseToDelete = it }
                    )
                } else {
                    when (currentTab) {
                        0 -> {
                            val totalInv = investmentList.sumOf { it.quantity * it.currentPrice }
                            val totalBank = bankList.sumOf { it.currentBalance }
                            
                            HomeDashboardDesign(
                                username = username, userFullName = userFullName, 
                                profilePicUrl = profilePicUrl, // 🚀 NEW
                                paddingValues = paddingValues, 
                                thisMonthExpenses = thisMonthExpenses, thisYearExpenses = thisYearExpenses, budgetLimit = budgetLimit,
                                isLoadingExpenses = isLoadingExpenses, dNavState = dNavState, dBackPresses = dBackPresses, 
                                onLogout = onLogout, onRefreshExpenses = { refreshTrigger++ },
                                onExpenseCardClick = { showExpenseHistory = true }, onContriClick = { showContriScreen = true },
                                onAvatarClick = { selectedTab = 4; openProfileDetails = true },
                                contriCount = contriRoomsList.size, totalInvestment = totalInv, totalBankBalance = totalBank,
                                onInvestmentClick = { assetsCurrentView = "InvestmentDetails"; selectedTab = 1 },
                                onBankClick = { assetsCurrentView = "DirectBankAccounts"; selectedTab = 1 },
                                onBudgetSaved = { refreshTrigger++ } 
                            )
                        }
                        1 -> AssetsScreen(
                            paddingValues = paddingValues, username = username, investmentList = investmentList, 
                            bankList = bankList, fdList = fdList, ccList = ccList, cashData = cashData, 
                            isLoading = isLoadingExpenses, onRefreshClick = { refreshTrigger++ },
                            currentView = assetsCurrentView, onViewChange = { assetsCurrentView = it }, 
                            onEditBankClick = { bankToEdit = it }, onEditCCClick = { ccToEdit = it }, onEditFDClick = { fdToEdit = it }
                        )
                        3 -> AnalyticsScreen(paddingValues = paddingValues)
                        4 -> ProfileScreen(
                            username = username, name = userFullName, email = userEmail, mobile = userMobile, 
                            profilePicUrl = profilePicUrl, // 🚀 NEW
                            dob = userDob, paddingValues = paddingValues, 
                            themeMode = themeMode, onThemeChange = onThemeChange, 
                            isUpdateAvailable = isUpdateAvailable,
                            onLogout = onLogout, onProfileRefresh = { refreshTrigger++ },
                            startInDetails = openProfileDetails, onResetDetailsState = { openProfileDetails = false }
                        )
                    }
                }
            }
        }

        AddScreen(
            username = username, showMenu = showAddMenu, onToggleMenu = { showAddMenu = !showAddMenu }, 
            onExpenseAdded = { newEntry -> transactionList = listOf(newEntry) + transactionList }, 
            onInvestmentAdded = { refreshTrigger++ }, onFinanceAdded = { refreshTrigger++ }, 
            bankList = bankList, ccList = ccList, cashData = cashData
        )

        if (bankToEdit != null) { EditBankDialog(bank = bankToEdit!!, username = username, onDismiss = { bankToEdit = null }, onUpdateSuccess = { bankToEdit = null; refreshTrigger++ }) }
        if (ccToEdit != null) { EditCreditCardDialog(cc = ccToEdit!!, username = username, onDismiss = { ccToEdit = null }, onUpdateSuccess = { ccToEdit = null; refreshTrigger++ }) }
        if (fdToEdit != null) { EditFDDialog(fd = fdToEdit!!, username = username, onDismiss = { fdToEdit = null }, onUpdateSuccess = { fdToEdit = null; refreshTrigger++ }) }
        if (expenseToDelete != null) { 
            DeleteExpenseDialog(
                expense = expenseToDelete!!, username = username, onDismiss = { expenseToDelete = null }, 
                onSuccess = { 
                    val targetDate = expenseToDelete?.date
                    expenseToDelete = null
                    if(targetDate != null) { transactionList = transactionList.filter { it.date != targetDate } }
                    refreshTrigger++ 
                }
            ) 
        }
        if (expenseToEdit != null) { EditExpenseDialog(expense = expenseToEdit!!, username = username, bankList = bankList, ccList = ccList, onDismiss = { expenseToEdit = null }, onSuccess = { expenseToEdit = null; refreshTrigger++ }) }
    }
}

fun Modifier.bounceClick(
    scaleDown: Float = 0.90f,
    onClick: (() -> Unit)? = null 
) = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium 
        ),
        label = "BounceAnimation"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
            } else { Modifier }
        )
        .pointerInput(Unit) { 
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(false)
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
        }
}
