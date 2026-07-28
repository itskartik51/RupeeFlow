package com.kartikey.rupeeflow.UI_Screens.Home

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExpenseSummaryCard(
    thisMonthExpenses: Double,
    thisYearExpenses: Double,
    isLoadingExpenses: Boolean,
    onRefreshExpenses: () -> Unit,
    onExpenseCardClick: () -> Unit
) {
    // PREMIUM UI: Smooth continuous rotation animation jab isLoading true ho
    val infiniteTransition = rememberInfiniteTransition(label = "refreshAnim")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAnim"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpenseCardClick() }, 
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32)), 
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header & Refresh Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Expense Summary", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                
                IconButton(onClick = onRefreshExpenses, modifier = Modifier.size(24.dp)) {
                    // Yahan se mota CircularProgressIndicator hata kar smooth rotate laga diya gaya hai
                    Icon(
                        imageVector = Icons.Outlined.Refresh, 
                        contentDescription = "Refresh", 
                        tint = Color.White,
                        modifier = Modifier.rotate(if (isLoadingExpenses) angle else 0f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Monthly & Yearly Expenses
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("This Month", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    Text("₹ $thisMonthExpenses", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("This Year", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    Text("₹ $thisYearExpenses", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))
            
            // View History Prompt
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.History, contentDescription = "History", tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tap to View Full History", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}
