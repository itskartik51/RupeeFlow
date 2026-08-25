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
    Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        
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
                )
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text("RupeeFlow", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hi, $username", 
                            color = MaterialTheme.colorScheme.onSurfaceVariant, 
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified User",
                            tint = Color(0xFF1D9BF0),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                
                ProfileAvatar(
                    name = userFullName.ifBlank { username },
                    profilePicUrl = profilePicUrl,
                    size = 42.dp,
                    fontSize = 18.sp,
                    onClick = onAvatarClick
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            
            ExpenseSummaryCard(
                todayExpenses = todayExpenses, 
                thisMonthExpenses = thisMonthExpenses, 
                thisYearExpenses = thisYearExpenses, 
                budgetLimit = budgetLimit,
                isLoadingExpenses = isLoadingExpenses,
                onRefreshExpenses = onRefreshExpenses, 
                onExpenseCardClick = onExpenseCardClick,
            )
            
            Spacer(modifier = Modifier.height(16.dp))

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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            SpendingTrackerCard(transactions = transactionList) 
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ReminderBanner()
            
            Spacer(modifier = Modifier.height(30.dp)) 
        }
    }
}
