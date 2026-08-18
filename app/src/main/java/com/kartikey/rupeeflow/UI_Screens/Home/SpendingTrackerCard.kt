package com.kartikey.rupeeflow.UI_Screens.Home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.UI_Screens.Add.TransactionModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

fun formatYAxis(value: Double): String {
    return when {
        value >= 10000000 -> String.format(Locale.US, "%.1fCr", value / 10000000)
        value >= 100000 -> String.format(Locale.US, "%.1fL", value / 100000)
        value >= 1000 -> String.format(Locale.US, "%.1fK", value / 1000).replace(".0K", "K")
        else -> value.toInt().toString()
    }
}

fun getRoundedTop(maxVal: Double): Double {
    if (maxVal <= 0) return 1000.0 // 🚀 Fallback scale if expenses are ₹0
    val magnitude = 10.0.pow(floor(log10(maxVal)))
    val normalized = maxVal / magnitude
    val roundedNormal = when {
        normalized <= 1.0 -> 1.0
        normalized <= 2.0 -> 2.0
        normalized <= 2.5 -> 2.5
        normalized <= 5.0 -> 5.0
        else -> 10.0
    }
    return roundedNormal * magnitude
}

fun parseDateToMillis(dateStr: String): Long {
    val formatWithTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val formatOnlyDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val formatDash = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return try { formatWithTime.parse(dateStr)?.time ?: 0L } catch(e: Exception) {
        try { formatOnlyDate.parse(dateStr)?.time ?: 0L } catch(e2: Exception) {
            try { formatDash.parse(dateStr)?.time ?: 0L } catch(e3: Exception) { 0L }
        }
    }
}

@Composable
fun SpendingTrackerCard(
    transactions: List<TransactionModel>,
    modifier: Modifier = Modifier
) {
    var weekOffset by remember { mutableIntStateOf(0) }
    
    // Date Calculations
    val calendar = Calendar.getInstance().apply { firstDayOfWeek = Calendar.SUNDAY }
    val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) 
    
    calendar.add(Calendar.WEEK_OF_YEAR, weekOffset)
    calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val startOfWeekMillis = calendar.timeInMillis
    
    val endCal = calendar.clone() as Calendar
    endCal.add(Calendar.DAY_OF_YEAR, 6)
    endCal.set(Calendar.HOUR_OF_DAY, 23)
    endCal.set(Calendar.MINUTE, 59)
    endCal.set(Calendar.SECOND, 59)
    val endOfWeekMillis = endCal.timeInMillis
    
    val startDay = SimpleDateFormat("d", Locale.getDefault()).format(calendar.time)
    val endDay = SimpleDateFormat("d", Locale.getDefault()).format(endCal.time)
    val startMonthStr = SimpleDateFormat("MMM", Locale.getDefault()).format(calendar.time)
    val endMonthStr = SimpleDateFormat("MMM", Locale.getDefault()).format(endCal.time)
    
    val dateRangeStr = if (startMonthStr == endMonthStr) "$startDay–$endDay $startMonthStr" else "$startDay $startMonthStr – $endDay $endMonthStr"
    
    // Transaction matching and logic
    var oldestMillis = Long.MAX_VALUE
    val dailyTotals = DoubleArray(7) { 0.0 }
    var weekTotal = 0.0
    var maxDaily = 0.0
    
    transactions.forEach { tx ->
        val txMillis = parseDateToMillis(tx.date)
        if (txMillis > 0 && txMillis < oldestMillis) oldestMillis = txMillis
        
        if (txMillis in startOfWeekMillis..endOfWeekMillis) {
            val txCal = Calendar.getInstance().apply { timeInMillis = txMillis }
            val dayIndex = txCal.get(Calendar.DAY_OF_WEEK) - 1 // Sun=0... Sat=6
            if (dayIndex in 0..6) {
                dailyTotals[dayIndex] += tx.amount
                weekTotal += tx.amount
                if (dailyTotals[dayIndex] > maxDaily) maxDaily = dailyTotals[dayIndex]
            }
        }
    }
    
    // 🚀 Edge Cases Logic (Opacity & Boundaries)
    val canGoBack = weekOffset > -4 && (startOfWeekMillis > oldestMillis)
    val canGoForward = weekOffset < 0
    
    val topValue = getRoundedTop(maxDaily)
    val midValue = topValue / 2.0
    
    val formatRupee = java.text.NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }
    val totalStr = formatRupee.format(weekTotal).replace("-₹", "-₹ ")

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Analytics Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Spending Tracker", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "VIEW ANALYTICS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 🚀 Middle Minimalist Header (Total & Dates)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (canGoBack) weekOffset-- }, enabled = canGoBack, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (canGoBack) 1f else 0.3f))
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = totalStr, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = dateRangeStr, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                IconButton(onClick = { if (canGoForward) weekOffset++ }, enabled = canGoForward, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (canGoForward) 1f else 0.3f))
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 🚀 Master Component: Graph Lines + Bars + Y/X Axis combined precisely
            Box(modifier = Modifier.fillMaxWidth()) {
                
                // 1. Subtle Background Grid Lines
                Column(modifier = Modifier.height(100.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                }
                
                // 2. Y-Axis Numbers (Right aligned)
                Column(
                    modifier = Modifier.height(100.dp).align(Alignment.TopEnd),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(text = formatYAxis(topValue), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.offset(y = (-6).dp))
                    Text(text = formatYAxis(midValue), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.offset(y = (-6).dp))
                    Text(text = "", fontSize = 10.sp) // Space holder for baseline
                }
                
                // 3. Graph Bars & X-Axis Texts (Sun-Sat)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(end = 36.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    for (i in 0..6) {
                        val ratio = (dailyTotals[i] / topValue).toFloat().coerceIn(0f, 1f)
                        val isToday = (weekOffset == 0 && (i + 1) == currentDayOfWeek)
                        val textOpacity = if (isToday) 1f else 0.5f
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Bar Component
                            Box(
                                modifier = Modifier.height(100.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                if (ratio > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .width(16.dp)
                                            .fillMaxHeight(ratio)
                                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // Day Component
                            Text(
                                text = dayLabels[i],
                                fontSize = 11.sp,
                                fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = textOpacity)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
