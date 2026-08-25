package com.kartikey.rupeeflow.UI_Screens.Home

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
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
import com.kartikey.rupeeflow.R
import com.kartikey.rupeeflow.UI_Screens.Profile.ProfileAvatar 

@Composable
fun HomeDashboardDesign(
    username: String, 
    userFullName: String, 
    profilePicUrl: String, 
    paddingValues: PaddingValues, 
    todayExpenses: Double, 
    thisMonthExpenses: Double, 
    thisYearExpenses: Double, 
    budgetLimit: Double,
    transactionList: List<com.kartikey.rupeeflow.UI_Screens.Add.TransactionModel>,
    isLoadingExpenses: Boolean,
    dNavState: String, 
    dBackPresses: Int, 
    onLogout: () -> Unit,
    onRefreshExpenses: () -> Unit = {}, 
    onExpenseCardClick: () -> Unit,
    onContriClick: () -> Unit,
    onAvatarClick: () -> Unit, 
    contriCount: Int = 0,
    totalInvestment: Double,
    totalBankBalance: Double,
    onInvestmentClick: () -> Unit,
    onBankClick: () -> Unit,
    onBudgetSaved: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {[cite: 3]
        
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher), 
                    contentDescription = "App Logo", 
                    modifier = Modifier.size(46.dp).clip(CircleShape)
                )[cite: 3]
                Spacer(modifier = Modifier.width(12.dp))[cite: 3]
                
                Column(modifier = Modifier.weight(1f)) {[cite: 3]
                    Text("RupeeFlow", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)[cite: 3]
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hi, $username", 
                            color = MaterialTheme.colorScheme.onSurfaceVariant, 
                            fontSize = 13.sp
                        )[cite: 3]
                        Spacer(modifier = Modifier.width(4.dp))[cite: 3]
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified User",
                            tint = Color(0xFF1D9BF0),
                            modifier = Modifier.size(13.dp)
                        )[cite: 3]
                    }
                }
                
                ProfileAvatar(
                    name = userFullName.ifBlank { username },[cite: 3]
                    profilePicUrl = profilePicUrl,[cite: 3]
                    size = 42.dp,[cite: 3]
                    fontSize = 18.sp,[cite: 3]
                    onClick = onAvatarClick[cite: 3]
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))[cite: 3]
            
            ExpenseSummaryCard(
                todayExpenses = todayExpenses, 
                thisMonthExpenses = thisMonthExpenses, 
                thisYearExpenses = thisYearExpenses, 
                budgetLimit = budgetLimit,
                isLoadingExpenses = isLoadingExpenses,
                onRefreshExpenses = onRefreshExpenses, 
                onExpenseCardClick = onExpenseCardClick,
            )[cite: 3]
            
            Spacer(modifier = Modifier.height(16.dp))[cite: 3]

            // 🚀 Encapsulated Clean Cards Grid (All logic handled in GridCard.kt)
            HomeQuickCardsGrid(
                contriCount = contriCount,
                totalInvestment = totalInvestment,
                totalBankBalance = totalBankBalance,
                budgetLimit = budgetLimit,
                thisMonthExpenses = thisMonthExpenses,
                username = username,
                onContriClick = onContriClick,
                onInvestmentClick = onInvestmentClick,
                onBankClick = onBankClick,
                onBudgetSaved = onBudgetSaved
            )
            
            Spacer(modifier = Modifier.height(16.dp))[cite: 3]
            
            SpendingTrackerCard(transactions = transactionList) [cite: 3]
            
            Spacer(modifier = Modifier.height(16.dp))[cite: 3]
            
            ReminderBanner()[cite: 3]
            
            Spacer(modifier = Modifier.height(30.dp)) [cite: 3]
        }
    }
}
