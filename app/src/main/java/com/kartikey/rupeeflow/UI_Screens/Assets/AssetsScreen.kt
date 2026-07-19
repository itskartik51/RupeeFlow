package com.kartikey.rupeeflow.UI_Screens.Assets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState 
import androidx.compose.foundation.verticalScroll 
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Money
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.BankAccountsScreen
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.FinanceScreen
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.CashScreen
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.CreditCardsScreen
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.FixedDepositsScreen
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.CashItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.CreditCardItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.FDItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.formatRupeeAmount

data class InvestmentItem(
    val assetName: String,
    val quantity: Double,
    val avgBuyPrice: Double,
    val currentPrice: Double,
    val oneDayChangePrice: Double
)

data class BankAccountItem(
    val bankName: String,
    val accountNo: String,
    val currentBalance: Double,
    val interestRate: Double,
    val qtrInterestPct: Double,
    val expQtrInt: Double,
    val accruedQtrInt: Double,
    val expYrInt: Double,
    val accruedYrInt: Double,
    val oneDayInt: Double
)

@Composable
fun AssetsScreen(
    paddingValues: PaddingValues, 
    username: String, 
    investmentList: List<InvestmentItem>,
    bankList: List<BankAccountItem>, 
    fdList: List<FDItem>,
    ccList: List<CreditCardItem>,
    cashData: CashItem,
    isLoading: Boolean = false, 
    onRefreshClick: () -> Unit = {},
    currentView: String, 
    onViewChange: (String) -> Unit,
    onEditBankClick: (BankAccountItem) -> Unit,
    onEditCCClick: (CreditCardItem) -> Unit,
    onEditFDClick: (FDItem) -> Unit
) { 
    val totalBank = bankList.sumOf { it.currentBalance }
    val totalCash = cashData.amount
    val totalFD = fdList.sumOf { it.accruedValue }
    val totalCC = ccList.sumOf { it.outstanding }
    val totalInv = investmentList.sumOf { it.quantity * it.currentPrice }
    
    // Networth Core Logic
    val networthAmount = totalBank + totalCash + totalFD + totalInv - totalCC

    if (currentView == "Main") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()) 
                .padding(16.dp)
        ) {
            NetworthCard(networthAmount = networthAmount, isLoading = isLoading, onClick = { })
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "My Investments", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                TextButton(onClick = { onViewChange("InvestmentDetails") }) { Text("More", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) { GridItemCard("TOTAL INVESTMENTS", formatRupeeAmount(totalInv)) }
                    Box(modifier = Modifier.weight(1f)) { GridItemCard("MUTUAL FUNDS", "₹0") }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) { GridItemCard("ETF", "₹0") }
                    Box(modifier = Modifier.weight(1f)) { GridItemCard("BONDS", "₹0") }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "My Finance", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                TextButton(onClick = { onViewChange("FinanceDetails") }) { Text("More", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) { 
                        FinanceGridCard("Cash", formatRupeeAmount(totalCash), null, Icons.Outlined.Money, Color(0xFF388E3C)) { onViewChange("DirectCash") } 
                    }
                    Box(modifier = Modifier.weight(1f)) { 
                        FinanceGridCard("Bank Balance", formatRupeeAmount(totalBank), "${bankList.size}", Icons.Outlined.AccountBalance, Color(0xFF1976D2)) { onViewChange("DirectBankAccounts") } 
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) { 
                        FinanceGridCard("Credit Card", formatRupeeAmount(totalCC), "${ccList.size}", Icons.Outlined.CreditCard, Color(0xFFD32F2F)) { onViewChange("DirectCreditCards") } 
                    }
                    Box(modifier = Modifier.weight(1f)) { 
                        FinanceGridCard("FD : Fixed Deposit", formatRupeeAmount(totalFD), "${fdList.size}", Icons.Outlined.Savings, Color(0xFFF57C00)) { onViewChange("DirectFDs") } 
                    }
                }
            }
        }
    } else if (currentView == "InvestmentDetails") {
        InvestmentScreen(onBackClick = { onViewChange("Main") }, username = username, investmentList = investmentList, isLoading = isLoading, onRefreshClick = onRefreshClick)
    } else if (currentView == "FinanceDetails") {
        FinanceScreen(
            onBackClick = { onViewChange("Main") },
            username = username,
            bankList = bankList,
            ccList = ccList,
            fdList = fdList,
            cashData = cashData,
            isLoading = isLoading,
            onRefreshClick = onRefreshClick,
            onEditBankClick = onEditBankClick,
            onEditCCClick = onEditCCClick,
            onEditFDClick = onEditFDClick
        )
    } else if (currentView == "DirectBankAccounts") {
        BankAccountsScreen(onBackClick = { onViewChange("Main") }, username = username, bankList = bankList, isLoading = isLoading, onRefreshClick = onRefreshClick, onEditBankClick = onEditBankClick)
    } else if (currentView == "DirectCreditCards") {
        CreditCardsScreen(onBackClick = { onViewChange("Main") }, username = username, ccList = ccList, isLoading = isLoading, onRefreshClick = onRefreshClick, onEditCCClick = onEditCCClick)
    } else if (currentView == "DirectFDs") {
        FixedDepositsScreen(onBackClick = { onViewChange("Main") }, username = username, fdList = fdList, isLoading = isLoading, onRefreshClick = onRefreshClick, onEditFDClick = onEditFDClick)
    } else if (currentView == "DirectCash") {
        CashScreen(onBackClick = { onViewChange("Main") }, username = username, cashData = cashData, onRefreshRequest = onRefreshClick)
    }
}

@Composable
fun FinanceGridCard(title: String, amount: String, linkCount: String?, icon: ImageVector, iconColor: Color, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), 
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp), 
        elevation = CardDefaults.cardElevation(2.dp), 
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.Start) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = title, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = amount, color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                if (linkCount != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(
                        modifier = Modifier.background(Color(0xFFE3F2FD), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Link, contentDescription = "Link", tint = Color(0xFF1976D2), modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(linkCount, color = Color(0xFF1976D2), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun NetworthCard(networthAmount: Double, isLoading: Boolean, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("NET WORTH", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF2E7D32))
            else Text(formatRupeeAmount(networthAmount), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
        }
    }
}

@Composable
fun GridItemCard(title: String, amount: String) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp), border = BorderStroke(1.dp, Color(0xFFEEEEEE))) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.Start) {
            Text(text = title, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = amount, color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
