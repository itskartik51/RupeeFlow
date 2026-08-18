package com.kartikey.rupeeflow.UI_Screens.Home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ExpenseSummaryCard(
    todayExpenses: Double, // 🚀 NEW: Added Today's expenses
    thisMonthExpenses: Double,
    thisYearExpenses: Double,
    budgetLimit: Double,
    isLoadingExpenses: Boolean,
    onRefreshExpenses: () -> Unit,
    onExpenseCardClick: () -> Unit
) {
    var selectedPeriod by remember { mutableStateOf("Month") }
    var expanded by remember { mutableStateOf(false) }

    val currentAmount = when (selectedPeriod) {
        "Today" -> todayExpenses
        "Month" -> thisMonthExpenses
        "Year" -> thisYearExpenses
        else -> thisMonthExpenses
    }
    
    val currentBudget = when (selectedPeriod) {
        "Today" -> budgetLimit / 30 // Approximate daily budget limit
        "Month" -> budgetLimit
        "Year" -> budgetLimit * 12
        else -> budgetLimit
    }
    
    val progressRatio = if (currentBudget > 0) (currentAmount / currentBudget).toFloat().coerceIn(0f, 1f) else 0f
    val percentUsed = if (currentBudget > 0) ((currentAmount / currentBudget) * 100) else 0.0

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

    fun formatNumber(amount: Double): String {
        val format = NumberFormat.getNumberInstance(Locale("en", "IN"))
        format.maximumFractionDigits = 2
        return format.format(amount)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onExpenseCardClick() }, // 🚀 FIX: Card now consumes its own click so inner clicks don't bounce the whole card
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) 
    ) {
        Column(
            modifier = Modifier.padding(16.dp) 
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Expenses", 
                    color = MaterialTheme.colorScheme.onSurface, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 15.sp 
                )
                
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { expanded = true }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = selectedPeriod, 
                            fontSize = 13.sp, 
                            fontWeight = FontWeight.Medium, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown, 
                            contentDescription = "Select", 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Today", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = { selectedPeriod = "Today"; expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Month", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = { selectedPeriod = "Month"; expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Year", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = { selectedPeriod = "Year"; expanded = false }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp)) 
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 30.sp)) {
                            append("₹ ")
                        }
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 30.sp)) {
                            append(formatNumber(currentAmount))
                        }
                    }
                )
                
                Box(
                    modifier = Modifier
                        .size(28.dp) 
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .bounceClick { onRefreshExpenses() }, // 🚀 FIX: Independent bounce
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh, 
                        contentDescription = "Refresh", 
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(18.dp) 
                            .rotate(if (isLoadingExpenses) angle else 0f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp)) 
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp) 
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressRatio)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "₹0", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = "${String.format(Locale.US, "%.1f", percentUsed)}% used", 
                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold
                )
                Text(text = "₹${formatNumber(currentBudget)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            
            Spacer(modifier = Modifier.height(14.dp)) 
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .bounceClick { onExpenseCardClick() } // 🚀 FIX: Independent bounce
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "View History", 
                        color = MaterialTheme.colorScheme.onPrimary, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}
