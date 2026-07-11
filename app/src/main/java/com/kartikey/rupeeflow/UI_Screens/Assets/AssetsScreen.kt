package com.kartikey.rupeeflow.UI_Screens.Assets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
fun NetWorthSummaryCard(
    totalAssets: Double,
    totalLiabilities: Double,
    netWorthHistory: List<Double>,
    onClick: () -> Unit
) {
    // Net Worth Calculation
    val netWorth = totalAssets - totalLiabilities
    val isPositive = netWorth >= 0
    // Positive hai toh Green, Negative hai toh Red
    val netWorthColor = if (isPositive) Color(0xFF2E7D32) else Color(0xFFD32F2F)

    Card(
        // Sirf clickable banaya hai jaisa aapne kaha tha
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }, 
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            
            // --- ROW 1: Assets & Liabilities ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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

            // --- ROW 2: Net Worth (Sabse bada aur bold) ---
            Text("Net Worth", color = Color.Gray, fontSize = 14.sp)
            Text(
                text = "₹${netWorth.toInt()}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 36.sp,
                color = netWorthColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- ROW 3: Line Graph (Last 6 months trend) ---
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
                        // Point ki position calculate kar rahe hain height ke hisaab se
                        val normalizedY = ((max - value) / range).toFloat()
                        val y = normalizedY * size.height

                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    // Line draw karna
                    drawPath(
                        path = path,
                        color = netWorthColor.copy(alpha = 0.6f), // Line ka color Net Worth ke color jaisa hoga
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }
    }
}
