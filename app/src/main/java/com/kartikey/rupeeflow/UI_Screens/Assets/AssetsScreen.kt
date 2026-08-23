package com.kartikey.rupeeflow.UI_Screens.Assets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState 
import androidx.compose.foundation.verticalScroll 
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Money
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.BankAccountsScreen
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.CashScreen
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.CreditCardsScreen
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.FixedDepositsScreen
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.CashItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.CreditCardItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.FDItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.formatRupeeAmount
import com.kartikey.rupeeflow.UI_Screens.CacheManager
import com.kartikey.rupeeflow.UI_Screens.RupeeFlowCard
import com.kartikey.rupeeflow.UI_Screens.bounceClick

// --- UPDATED DATA CLASSES ---
data class InvestmentHistoryItem(
    val date: String,
    val quantity: Double,
    val price: Double,
    val amount: Double,
    val brokerage: Double
)

data class InvestmentItem(
    val assetName: String,
    val assetType: String, 
    val quantity: Double,
    val avgBuyPrice: Double,
    val currentPrice: Double,
    val oneDayChangePrice: Double,
    val history: List<InvestmentHistoryItem> = emptyList()
)

data class BankAccountItem(
    val firebaseKey: String = "",
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
    val context = LocalContext.current

    if (currentView == "Main") {
        val totalBank = bankList.sumOf { it.currentBalance }
        val totalCash = cashData.amount
        val totalFD = fdList.sumOf { it.accruedValue }
        val totalCC = ccList.sumOf { it.outstanding }
        val totalInv = investmentList.sumOf { it.quantity * it.currentPrice }
        
        val networthAmount = totalBank + totalCash + totalFD + totalInv - totalCC

        // Auto-sync current 10-day slot into networth history
        LaunchedEffect(networthAmount) {
            if (networthAmount > 0.0) {
                CacheManager.syncNetworthSlot(context, username, networthAmount)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()) 
                .padding(16.dp)
        ) {
            NetworthCard(
                networthAmount = networthAmount,
                isLoading = isLoading, 
                onRefresh = onRefreshClick,
                onClick = onRefreshClick
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(text = "My Investments", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 4.dp))
            Spacer(modifier = Modifier.height(12.dp))
            
            val sharesList = investmentList.filter { it.assetType.equals("Stock", true) || it.assetType.equals("Share", true) }
            val hasShares = sharesList.isNotEmpty()
            val totalShares = if (hasShares) sharesList.sumOf { it.quantity * it.currentPrice } else 0.0
            val sharesTitle = if (hasShares) "SHARES (${sharesList.size})" else "SHARES"

            val mfList = investmentList.filter { it.assetType.equals("Mutual Fund", true) || it.assetType.equals("MF", true) }
            val hasMF = mfList.isNotEmpty()
            val totalMF = if (hasMF) mfList.sumOf { it.quantity * it.currentPrice } else 0.0
            val mfTitle = if (hasMF) "MUTUAL FUNDS (${mfList.size})" else "MUTUAL FUNDS"

            val etfList = investmentList.filter { it.assetType.equals("ETF", true) }
            val hasETF = etfList.isNotEmpty()
            val totalETF = if (hasETF) etfList.sumOf { it.quantity * it.currentPrice } else 0.0
            val etfTitle = if (hasETF) "ETF (${etfList.size})" else "ETF"

            val bondsList = investmentList.filter { it.assetType.equals("Bond", true) }
            val hasBonds = bondsList.isNotEmpty()
            val totalBonds = if (hasBonds) bondsList.sumOf { it.quantity * it.currentPrice } else 0.0
            val bondsTitle = if (hasBonds) "BONDS (${bondsList.size})" else "BONDS"

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) { 
                        InvestmentGridCard(sharesTitle, totalShares, hasShares, "+ Add Share") { onViewChange("InvestmentDetails") } 
                    }
                    Box(modifier = Modifier.weight(1f)) { 
                        InvestmentGridCard(mfTitle, totalMF, hasMF, "+ Add MF") { onViewChange("InvestmentDetails") } 
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) { 
                        InvestmentGridCard(etfTitle, totalETF, hasETF, "+ Add ETF") { onViewChange("InvestmentDetails") } 
                    }
                    Box(modifier = Modifier.weight(1f)) { 
                        InvestmentGridCard(bondsTitle, totalBonds, hasBonds, "+ Add Bond") { onViewChange("InvestmentDetails") } 
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(text = "My Finance", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 4.dp))
            Spacer(modifier = Modifier.height(12.dp))
            
            val hasCash = cashData.amount > 0.0
            val hasBank = bankList.isNotEmpty()
            val hasCC = ccList.isNotEmpty()
            val hasFD = fdList.isNotEmpty()

            val cashTitle = "CASH"
            val bankTitle = if (hasBank) "BANK BALANCE (${bankList.size})" else "BANK BALANCE"
            val ccTitle = if (hasCC) "CREDIT CARD (${ccList.size})" else "CREDIT CARD"
            val fdTitle = if (hasFD) "FD (${fdList.size})" else "FD : FIXED DEPOSIT"

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) { 
                        FinanceGridCard(cashTitle, totalCash, hasCash, "+ Add Cash", Icons.Outlined.Money, Color(0xFF388E3C)) { onViewChange("DirectCash") } 
                    }
                    Box(modifier = Modifier.weight(1f)) { 
                        FinanceGridCard(bankTitle, totalBank, hasBank, "+ Add Bank", Icons.Outlined.AccountBalance, Color(0xFF1976D2)) { onViewChange("DirectBankAccounts") } 
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) { 
                        FinanceGridCard(ccTitle, totalCC, hasCC, "+ Add Card", Icons.Outlined.CreditCard, Color(0xFFD32F2F)) { onViewChange("DirectCreditCards") } 
                    }
                    Box(modifier = Modifier.weight(1f)) { 
                        FinanceGridCard(fdTitle, totalFD, hasFD, "+ Add FD", Icons.Outlined.Savings, Color(0xFFF57C00)) { onViewChange("DirectFDs") } 
                    }
                }
            }
        }
    } else if (currentView == "InvestmentDetails") {
        InvestmentScreen(onBackClick = { onViewChange("Main") }, username = username, investmentList = investmentList, isLoading = isLoading, onRefreshClick = onRefreshClick)
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
fun InvestmentGridCard(title: String, amount: Double, hasData: Boolean, addText: String, onClick: () -> Unit) {
    RupeeFlowCard(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(text = title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(8.dp))
            
            if (hasData) {
                Text(
                    text = formatRupeeAmount(amount), 
                    color = MaterialTheme.colorScheme.onSurface, 
                    fontSize = 19.sp, 
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(text = addText, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FinanceGridCard(
    title: String, 
    amount: Double, 
    hasData: Boolean, 
    addText: String, 
    icon: ImageVector, 
    iconColor: Color, 
    onClick: () -> Unit
) {
    RupeeFlowCard(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.Start) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            if (hasData) {
                Text(
                    text = formatRupeeAmount(amount), 
                    color = MaterialTheme.colorScheme.onSurface, 
                    fontSize = 19.sp, 
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(text = addText, color = iconColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
