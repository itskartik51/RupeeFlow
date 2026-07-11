package com.kartikey.rupeeflow.UI_Screens.Assets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

@Composable
fun AssetsScreen(paddingValues: PaddingValues) {
    // Ye variable track karega ki hume main page dikhana hai ya aage ka section
    var currentView by remember { mutableStateOf("Main") }

    if (currentView == "Main") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA)) // Halka off-white background
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 1. Apni Networth Screen ka Green Card
            NetworthCard(
                networthAmount = 79500.0,
                isLoading = false,
                onClick = { 
                    /* Networth ki history yahan open hogi */ 
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Investment 4-Grid Section
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
                // "More" Button jo clickable hai
                TextButton(onClick = { currentView = "InvestmentDetails" }) {
                    Text("More", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4 Grid Boxes
            Column(modifier = Modifier.fillMaxWidth()) {
                // Row 1: Stock & Mutual Funds
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        GridItemCard("STOCKS", "₹0", Color(0xFF388E3C)) // Green line
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        GridItemCard("MUTUAL FUNDS", "₹0", Color(0xFF1976D2)) // Blue line
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Row 2: ETF & Bonds
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        GridItemCard("ETF", "₹0", Color(0xFF7B1FA2)) // Purple line
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        GridItemCard("BONDS", "₹0", Color(0xFFF57C00)) // Orange line
                    }
                }
            }
        }
    } else if (currentView == "InvestmentDetails") {
        // Yahan par aage chalkar humari detailed Investment list (jo J2 se U2 tak ka data legi) aayegi.
        // Abhi ke liye bas ek temporary text dikha rahe hain aur wapas aane ka button hai.
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Investment Details Section Opened!", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { currentView = "Main" },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("Go Back")
            }
        }
    }
}

// Ye un 4 chote boxes ka design hai (Same to same screenshot jaisa)
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
            crossAxisAlignment = CrossAxisAlignment.Start
        ) {
            Text(text = title, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = amount, color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(12.dp))
            // Niche wali colored line
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .fillMaxWidth(0.6f) // Line thodi choti rakhi hai design ke hisaab se
                    .background(lineColor, RoundedCornerShape(2.dp))
            )
        }
    }
}
