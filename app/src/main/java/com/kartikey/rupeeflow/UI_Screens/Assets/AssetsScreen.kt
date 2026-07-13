package com.kartikey.rupeeflow.UI_Screens.Assets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Yahan Data Class define ki hai taaki saari files isey easily use kar sakein
data class InvestmentItem(
    val assetName: String,
    val quantity: Double,
    val avgBuyPrice: Double,
    val currentPrice: Double,
    val oneDayChangePrice: Double
)

@Composable
fun AssetsScreen(
    paddingValues: PaddingValues, 
    username: String, 
    investmentList: List<InvestmentItem>,
    onRefreshClick: () -> Unit = {}
) { 
    var currentView by remember { mutableStateOf("Main") }

    if (currentView == "Main") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Aapka original Networth Card
            NetworthCard(
                networthAmount = 79500.0,
                isLoading = false,
                onClick = { /* History */ }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Investments", 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 18.sp,
                    color = Color.Black
                )
                TextButton(onClick = { currentView = "InvestmentDetails" }) {
                    Text("More", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        GridItemCard("STOCKS", "₹0", Color(0xFF388E3C))
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        GridItemCard("MUTUAL FUNDS", "₹0", Color(0xFF1976D2))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        GridItemCard("ETF", "₹0", Color(0xFF7B1FA2))
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        GridItemCard("BONDS", "₹0", Color(0xFFF57C00))
                    }
                }
            }
        }
    } else if (currentView == "InvestmentDetails") {
        InvestmentScreen(
            onBackClick = { currentView = "Main" }, 
            username = username, 
            investmentList = investmentList,
            onRefreshClick = onRefreshClick
        )
    }
}

// Aapka Dummy Networth Card (Taki Error na aaye, agar ye kisi aur file me hai to ise hata dena)
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
