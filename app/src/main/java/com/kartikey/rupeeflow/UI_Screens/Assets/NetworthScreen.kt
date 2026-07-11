package com.kartikey.rupeeflow.UI_Screens.Assets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun NetworthCard(
    networthAmount: Double = 0.0,
    oneDayReturnAmount: Double = 0.0,
    oneDayReturnPercent: Double = 0.0,
    totalReturnAmount: Double = 0.0,
    totalReturnPercent: Double = 0.0,
    isLoading: Boolean = false,
    onRefresh: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    // Visibility state for the Eye icon
    var isVisible by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White), // White Theme
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // --- TOP ROW: Title & Action Icons ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Net Worth",
                    color = Color.DarkGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(
                        imageVector = if (isVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = "Toggle Visibility",
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { isVisible = !isVisible }
                    )
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Refresh Data",
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onRefresh() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- MIDDLE ROW: Main Amount ---
            if (isLoading) {
                CircularProgressIndicator(color = Color(0xFF2E7D32), modifier = Modifier.size(32.dp))
            } else {
                Text(
                    text = if (isVisible) "₹${String.format("%,.2f", networthAmount)}" else "• • • • • • •",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 36.sp,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- BOTTOM ROW: Returns ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Left Column (1D returns)
                Column {
                    Text(
                        text = "1D returns",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val is1DPos = oneDayReturnAmount >= 0
                    val color1D = if (is1DPos) Color(0xFF00C853) else Color(0xFFD32F2F)
                    val sign1D = if (is1DPos) "+" else "-"
                    val formattedAmt1D = String.format("%,.2f", abs(oneDayReturnAmount))
                    val formattedPct1D = String.format("%.2f", abs(oneDayReturnPercent))
                    
                    Text(
                        text = if (isVisible) {
                            "$sign1D₹$formattedAmt1D ($formattedPct1D%)"
                        } else {
                            "• • • • ($formattedPct1D%)"
                        },
                        color = color1D,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Right Column (Total returns)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total returns",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val isTotalPos = totalReturnAmount >= 0
                    val colorTotal = if (isTotalPos) Color(0xFF00C853) else Color(0xFFD32F2F)
                    val signTotal = if (isTotalPos) "+" else "-"
                    val formattedAmtTotal = String.format("%,.2f", abs(totalReturnAmount))
                    val formattedPctTotal = String.format("%.2f", abs(totalReturnPercent))

                    Text(
                        text = if (isVisible) {
                            "$signTotal₹$formattedAmtTotal ($formattedPctTotal%)"
                        } else {
                            "• • • • ($formattedPctTotal%)"
                        },
                        color = colorTotal,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
