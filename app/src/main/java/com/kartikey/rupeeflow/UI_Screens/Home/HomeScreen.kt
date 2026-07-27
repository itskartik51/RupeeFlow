package com.kartikey.rupeeflow.UI_Screens.Home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.Cloud_Database.Constants
import com.kartikey.rupeeflow.R
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeDashboardDesign(
    username: String, 
    userFullName: String, 
    paddingValues: PaddingValues, 
    thisMonthExpenses: Double, 
    thisYearExpenses: Double, 
    isLoadingExpenses: Boolean,
    dNavState: String, 
    dBackPresses: Int, 
    onLogout: () -> Unit,
    onRefreshExpenses: () -> Unit = {}, 
    onExpenseCardClick: () -> Unit,
    onContriClick: () -> Unit,
    onAvatarClick: () -> Unit, 
    contriCount: Int = 0,
    // NEW: Asset Values pass karne ke liye
    totalInvestment: Double,
    totalBankBalance: Double,
    onInvestmentClick: () -> Unit,
    onBankClick: () -> Unit
) {
    val context = LocalContext.current

    // Currency Formatting function
    fun formatRupee(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        format.maximumFractionDigits = 2
        return format.format(amount).replace("-₹", "-₹ ")
    }

    Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Image(painter = painterResource(id = R.mipmap.ic_launcher), contentDescription = "App Logo", modifier = Modifier.size(44.dp).clip(CircleShape))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("RupeeFlow", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text("Hi, $username", color = Color.Gray, fontSize = 12.sp)
            }
            
            val displayLetter = if (userFullName.isNotBlank()) userFullName.take(1).uppercase() else username.take(1).uppercase()
            
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9))
                    .clickable { onAvatarClick() }, 
                contentAlignment = Alignment.Center
            ) {
                Text(displayLetter, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        ExpenseSummaryCard(
            thisMonthExpenses = thisMonthExpenses, 
            thisYearExpenses = thisYearExpenses, 
            isLoadingExpenses = isLoadingExpenses,
            onRefreshExpenses = onRefreshExpenses, 
            onExpenseCardClick = onExpenseCardClick
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // SMART 0-CHECK LOGIC FOR CARDS
        val invDisplayValue = if (totalInvestment > 0) formatRupee(totalInvestment) else "Add Details"
        val bankDisplayValue = if (totalBankBalance > 0) formatRupee(totalBankBalance) else "Add Details"

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ContriDashboardCard(
                contriCount = contriCount,
                modifier = Modifier.weight(1f).clickable { onContriClick() }
            ) 
            GridCard(
                title = "TOTAL INVESTMENT", 
                value = invDisplayValue, 
                lineColor = Color.Transparent, // Line Removed
                modifier = Modifier.weight(1f),
                onClick = onInvestmentClick // Linked to My Investments
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GridCard(
                title = "BANK ACCOUNTS", 
                value = bankDisplayValue, 
                lineColor = Color(0xFFFFB300), 
                modifier = Modifier.weight(1f),
                onClick = onBankClick // Linked to Bank Accounts
            )
            GridCard(
                title = "BUDGET LIMIT", 
                value = "Add Details", 
                lineColor = Color.Transparent, 
                modifier = Modifier.weight(1f),
                onClick = { Toast.makeText(context, "Budget feature coming soon!", Toast.LENGTH_SHORT).show() }
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        SpendingTrackerCard()
        Spacer(modifier = Modifier.height(16.dp))
        ReminderBanner()
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Recent Transactions", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            TextButton(onClick = onLogout) { Text("Logout", color = Color(0xFFD32F2F)) }
        }
        Spacer(modifier = Modifier.height(60.dp)) 
    }
}

// ==========================================
// DYNAMIC CONTRI DASHBOARD CARD
// ==========================================
@Composable
fun ContriDashboardCard(
    contriCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "CONTRI",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$contriCount Contri",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val safeCount = contriCount.coerceIn(0, 5)
                for (i in 1..5) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                color = if (i <= safeCount) Color(0xFF2E7D32) else Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }
    }
}
