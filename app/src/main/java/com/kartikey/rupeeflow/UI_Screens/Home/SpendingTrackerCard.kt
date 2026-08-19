package com.kartikey.rupeeflow.UI_Screens.Home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
    if (maxVal <= 0) return 1000.0 // Fallback scale if expenses are ₹0
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
    
    // Start Animation trigger for first load
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        startAnimation = true
    }
    
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
    
    // Edge Cases Logic (Opacity & Boundaries)
    val canGoBack = weekOffset > -4 && (startOfWeekMillis > oldestMillis)
    val canGoForward = weekOffset < 0
    
    val topValue = getRoundedTop(maxDaily)
    val midValue = topValue / 2.0
    
    val formatRupee = java.text.NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }
    val totalStr = formatRupee.format(weekTotal).replace("-₹", "-₹ ")

    // 🚀 Interaction State Variables
    var touchedBarIndex by remember { mutableIntStateOf(-1) }
    var rowWidthPx by remember { mutableFloatStateOf(1f) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            
            // Minimalist Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (canGoBack) weekOffset-- }, enabled = canGoBack, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (canGoBack) 1f else 0.3f))
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = totalStr, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = dateRangeStr, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                IconButton(onClick = { if (canGoForward) weekOffset++ }, enabled = canGoForward, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (canGoForward) 1f else 0.3f))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Master Component: Graph
            Box(modifier = Modifier.fillMaxWidth()) {
                
                // Subtle Background Grid Lines
                Column(
                    modifier = Modifier
                        .height(80.dp)
                        .padding(end = 36.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                }
                
                // Y-Axis Numbers
                Column(
                    modifier = Modifier
                        .height(80.dp)
                        .width(36.dp)
                        .align(Alignment.TopEnd),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = formatYAxis(topValue), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.offset(y = (-6).dp)) 
                    Text(text = formatYAxis(midValue), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.offset(y = 0.dp)) 
                    Text(text = "0", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.offset(y = 6.dp)) 
                }
                
                // Graph Bars & X-Axis Texts (Sun-Sat)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 36.dp)
                        .onGloballyPositioned { rowWidthPx = it.size.width.toFloat() } // Track width for touch events
                        .pointerInput(Unit) {
                            // 🚀 Smart Touch Event Engine (Slides over bars without breaking scroll)
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitFirstDown(requireUnconsumed = false)
                                    val sectionWidth = rowWidthPx / 7f
                                    if (sectionWidth > 0) {
                                        touchedBarIndex = (event.position.x / sectionWidth).toInt().coerceIn(0, 6)
                                    }
                                    
                                    do {
                                        val nextEvent = awaitPointerEvent()
                                        nextEvent.changes.firstOrNull()?.let { change ->
                                            if (change.pressed && sectionWidth > 0) {
                                                touchedBarIndex = (change.position.x / sectionWidth).toInt().coerceIn(0, 6)
                                            }
                                        }
                                    } while (nextEvent.changes.any { it.pressed })
                                    
                                    // Touch clear
                                    touchedBarIndex = -1
                                }
                            }
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    
                    for (i in 0..6) {
                        val targetRatio = if (startAnimation) (dailyTotals[i] / topValue).toFloat().coerceIn(0f, 1f) else 0f
                        val animatedRatio by animateFloatAsState(
                            targetValue = targetRatio,
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                            label = "barHeightAnim_$i"
                        )
                        
                        val isToday = (weekOffset == 0 && (i + 1) == currentDayOfWeek)
                        val isTouched = touchedBarIndex == i
                        val isAnyTouched = touchedBarIndex != -1
                        
                        // Opacity Logic
                        val barAlpha = if (isAnyTouched && !isTouched) 0.3f else 1f
                        val textOpacity = if (isAnyTouched && !isTouched) 0.3f else if (isToday || isTouched) 1f else 0.5f
                        
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val tooltipBg = MaterialTheme.colorScheme.surfaceVariant
                        val dotInsideColor = MaterialTheme.colorScheme.surface
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            
                            // 🚀 Bar Component + Canvas Micro-Visuals + Tooltip Popup
                            Box(
                                modifier = Modifier
                                    .height(80.dp)
                                    .width(20.dp), // Width given to capture touch safely around bar
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                
                                // 1. Solid Bar
                                if (animatedRatio > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .width(16.dp)
                                            .fillMaxHeight(animatedRatio)
                                            .background(
                                                color = primaryColor.copy(alpha = barAlpha),
                                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                            )
                                    )
                                }
                                
                                // 2. Micro-Visuals (Dotted Line & Hollow Dot)
                                if (isTouched) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val barTopY = size.height * (1f - animatedRatio)
                                        
                                        // Vertical Dotted Line
                                        drawLine(
                                            color = primaryColor,
                                            start = Offset(size.width / 2f, barTopY),
                                            end = Offset(size.width / 2f, size.height),
                                            strokeWidth = 1.5.dp.toPx(),
                                            pathEffect = PathEffect.dashPath(floatArrayOf(12f, 10f), 0f)
                                        )
                                        
                                        // Hollow Dot
                                        drawCircle(
                                            color = dotInsideColor,
                                            radius = 5.dp.toPx(),
                                            center = Offset(size.width / 2f, barTopY),
                                            style = Fill
                                        )
                                        drawCircle(
                                            color = primaryColor,
                                            radius = 5.dp.toPx(),
                                            center = Offset(size.width / 2f, barTopY),
                                            style = Stroke(width = 2.dp.toPx())
                                        )
                                    }
                                }
                                
                                // 3. Capsular Tooltip
                                if (isTouched) {
                                    val touchedDateCal = Calendar.getInstance().apply { 
                                        timeInMillis = startOfWeekMillis
                                        add(Calendar.DAY_OF_YEAR, i)
                                    }
                                    val dateStr = SimpleDateFormat("EEE d", Locale.getDefault()).format(touchedDateCal.time)
                                    val amtStr = formatRupee.format(dailyTotals[i]).replace("-₹", "-₹ ")
                                    
                                    Box(
                                        modifier = Modifier
                                            .offset(y = (-((animatedRatio * 80) + 12)).dp) // Offset above the dot
                                            .zIndex(100f),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            
                                            // Edge-case Shift: Prevents tooltip from cutting at screen edges
                                            val xShift = when (i) {
                                                0 -> 24.dp
                                                6 -> (-24).dp
                                                else -> 0.dp
                                            }
                                            
                                            // Capsule Body
                                            Row(
                                                modifier = Modifier
                                                    .offset(x = xShift) 
                                                    .background(tooltipBg, RoundedCornerShape(50)) // Capsule Shape
                                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = amtStr, color = primaryColor, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(text = "on $dateStr", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                            }
                                            
                                            // Tooltip Tail (Triangle pointing to the dot)
                                            Canvas(modifier = Modifier.size(width = 12.dp, height = 6.dp)) {
                                                val path = Path().apply {
                                                    moveTo(0f, 0f)
                                                    lineTo(size.width, 0f)
                                                    lineTo(size.width / 2f, size.height)
                                                    close()
                                                }
                                                drawPath(path, color = tooltipBg)
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Bottom Tick Line
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(4.dp)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Day Label
                            Text(
                                text = dayLabels[i],
                                fontSize = 11.sp,
                                fontWeight = if (isToday || isTouched) FontWeight.ExtraBold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = textOpacity)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
