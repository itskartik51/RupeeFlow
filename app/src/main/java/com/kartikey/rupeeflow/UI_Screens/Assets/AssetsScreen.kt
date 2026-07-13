package com.kartikey.rupeeflow.UI_Screens.Assets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState // NEW IMPORT
import androidx.compose.foundation.verticalScroll // NEW IMPORT
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.FinanceScreen

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
    val oneDayInt: Double
)

@Composable
fun AssetsScreen(
    paddingValues: PaddingValues, 
    username: String, 
    investmentList: List<InvestmentItem>,
    bankList: List<BankAccountItem>, 
    isLoading: Boolean = false, 
    onRefreshClick: () -> Unit = {}
) { 
    var currentView by remember { mutableStateOf("Main") }
    
    val totalBankBalance = bankList.sumOf { it.currentBalance }

    if (currentView == "Main") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()) // YAHAN SCROLL ADD KIYA HAI
                .padding(16.dp)
        ) {
            NetworthCard(
                networthAmount = 79500.0,
                isLoading = isLoading,
                onClick = { /* History */ }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ==========================================
            // SECTION 1: MY INVESTMENTS
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "My Investments", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                TextButton(onClick = { currentView = "InvestmentDetails" }) {
                    Text("More", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) { GridItemCard("STOCKS", "₹0", Color(0xFF388E3C)) }
                    Box(modifier = Modifier.weight(1f)) { GridItemCard("MUTUAL FUNDS", "₹0", Color(0xFF1976D2)) }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) { GridItemCard("ETF", "₹0", Color(0xFF7B1FA2)) }
                    Box(modifier = Modifier.weight(1f)) { GridItemCard("BONDS", "₹0", Color(0xFFF57C00)) }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // ==========================================
            // SECTION 2: MY FINANCE / ACCOUNTS
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "My Finance", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                TextButton(onClick = { currentView = "FinanceDetails" }) {
                    Text("More", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) { FinanceGridCard("Cash", "₹0", Icons.Outlined.Money, Color(0xFF388E3C)) }
                    Box(modifier = Modifier.weight(1f)) { FinanceGridCard("Bank Balance", "₹${totalBankBalance.toInt()}", Icons.Outlined.AccountBalance, Color(0xFF1976D2)) }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) { FinanceGridCard("Credit Card", "₹0", Icons.Outlined.CreditCard, Color(0xFFD32F2F)) }
                    Box(modifier = Modifier.weight(1f)) { FinanceGridCard("FD : Fixed Deposit", "₹0", Icons.Outlined.Savings, Color(0xFFF57C00)) }
                }
            }
        }
    } else if (currentView == "InvestmentDetails") {
        InvestmentScreen(
            onBackClick = { currentView = "Main" }, 
            username = username, 
            investmentList = investmentList,
            isLoading = isLoading, 
            onRefreshClick = onRefreshClick
        )
    } else if (currentView == "FinanceDetails") {
        FinanceScreen(
            onBackClick = { currentView = "Main" },
            username = username,
            bankList = bankList,
            isLoading = isLoading,
            onRefreshClick = onRefreshClick
        )
    }
}

@Composable
fun FinanceGridCard(title: String, amount: String, icon: ImageVector, iconColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = title, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = amount, color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .fillMaxWidth(0.6f)
                    .background(iconColor, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun NetworthCard(networthAmount: Double, isLoading: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("NET WORTH", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF2E7D32))
            } else {
                Text("₹$networthAmount", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
            }
        }
    }
}

@Composable
fun GridItemCard(title: String, amount: String, lineColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = title, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = amount, color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .fillMaxWidth(0.6f)
                    .background(lineColor, RoundedCornerShape(2.dp))
            )
        }
    }
}
