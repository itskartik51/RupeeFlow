package com.kartikey.rupeeflow.UI_Screens.Assets

import androidx.compose.foundation.BorderStroke // Fix 1: Missing Import Added
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AssetsScreen(paddingValues: PaddingValues) {
    var currentView by remember { mutableStateOf("Main") }

    if (currentView == "Main") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(paddingValues)
                .padding(16.dp)
        ) {
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
        InvestmentScreen(onBackClick = { currentView = "Main" })
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
            horizontalAlignment = Alignment.Start // Fix 2: Changed from crossAxisAlignment
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
