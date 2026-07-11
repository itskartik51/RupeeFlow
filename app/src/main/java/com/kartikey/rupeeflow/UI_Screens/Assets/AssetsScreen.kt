package com.kartikey.rupeeflow.UI_Screens.Assets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AssetsScreen(paddingValues: PaddingValues) {
    // Ye variable track karega ki hum summary pe hain ya detail screen pe
    var currentView by remember { mutableStateOf("Summary") }

    if (currentView == "Summary") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Click card to view detailed portfolio", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
            
            NetWorthSummaryCard(
                totalAssets = 124500.0,
                totalLiabilities = 45000.0,
                netWorthHistory = listOf(50000.0, 55000.0, 62000.0, 59000.0, 71000.0, 79500.0),
                onClick = { 
                    // Card pe click karte hi Investment view khul jayega
                    currentView = "Investments" 
                }
            )
        }
    } else {
        // Investment Screen dikhegi, aur Back button pe wapas Summary aayega
        InvestmentScreen(onBackClick = { currentView = "Summary" })
    }
}

@Composable
fun NetWorthSummaryCard(
    totalAssets: Double,
    totalLiabilities: Double,
    netWorthHistory: List<Double>,
    onClick: () -> Unit
) {
    val netWorth = totalAssets - totalLiabilities
    val isPositive = netWorth >= 0
    val netWorthColor = if (isPositive) Color(0xFF2E7D32) else Color(0xFFD32F2F)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }, 
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Total Assets", color = Color.Gray, fontSize = 12.sp)
                    Text("₹${totalAssets.toInt()}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Liabilities", color = Color.Gray, fontSize = 12.sp)
                    Text("₹${totalLiabilities.toInt()}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("Net Worth", color = Color.Gray, fontSize = 14.sp)
            Text(
                text = "₹${netWorth.toInt()}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 36.sp,
                color = netWorthColor
            )
            Spacer(modifier = Modifier.height(24.dp))
            if (netWorthHistory.isNotEmpty()) {
                Text("Last 6 Months Trend", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(bottom = 8.dp))
                Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                    val max = netWorthHistory.maxOrNull() ?: 1.0
                    val min = netWorthHistory.minOrNull() ?: 0.0
                    val range = if (max == min) 1.0 else (max - min)
                    val stepX = size.width / (netWorthHistory.size - 1).coerceAtLeast(1)
                    val path = Path()

                    netWorthHistory.forEachIndexed { index, value ->
                        val x = index * stepX
                        val normalizedY = ((max - value) / range).toFloat()
                        val y = normalizedY * size.height
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path = path, color = netWorthColor.copy(alpha = 0.6f), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
    }
}
