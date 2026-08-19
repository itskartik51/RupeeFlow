package com.kartikey.rupeeflow.UI_Screens.Home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
    todayExpenses: Double = 0.0, 
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
        "Month" -> thisMonthExpenses
        "Year" -> thisYearExpenses
        else -> thisMonthExpenses
    }
    
    val currentBudget = when (selectedPeriod) {
        "Month" -> budgetLimit
        "Year" -> budgetLimit * 12
        else -> budgetLimit
    }
    
    val progressRatio = if (currentBudget > 0) (currentAmount / currentBudget).toFloat().coerceIn(0f, 1f) else 0f
    val percentUsed = if (currentBudget > 0) ((currentAmount / currentBudget) * 100) else 0.0

    var isLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isLoaded = true }

    val animatedProgressRatio by animateFloatAsState(
        targetValue = if (isLoaded) progressRatio else 0f,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "budgetLineAnimation"
    )

    fun formatNumber(amount: Double): String {
        val format = NumberFormat.getNumberInstance(Locale("en", "IN"))
        format.maximumFractionDigits = 2
        return format.format(amount)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(), 
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
                        .bounceClick { if (!isLoadingExpenses) onRefreshExpenses() }, 
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(
                        targetState = isLoadingExpenses,
                        animationSpec = tween(durationMillis = 250),
                        label = "refreshCrossfade"
                    ) { loading ->
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Refresh, 
                                contentDescription = "Refresh", 
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp)) 
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(0.65f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp) 
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (animatedProgressRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgressRatio)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "₹0", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = "${String.format(Locale.US, "%.1f", percentUsed)}% used", 
                            color = MaterialTheme.colorScheme.onSurfaceVariant, 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = "₹${formatNumber(currentBudget)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
                
                Spacer(modifier = Modifier.weight(0.05f))
                
                Box(
                    modifier = Modifier
                        .weight(0.30f)
                        .height(34.dp)
                        .bounceClick { onExpenseCardClick() }
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "View History", 
                        color = MaterialTheme.colorScheme.onPrimary, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
