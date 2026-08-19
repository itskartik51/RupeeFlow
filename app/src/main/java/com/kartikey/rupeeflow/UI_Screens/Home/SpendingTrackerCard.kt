package com.kartikey.rupeeflow.UI_Screens.Home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kartikey.rupeeflow.UI_Screens.Add.TransactionModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

data class WeekData(
    val dateRangeStr: String,
    val weekTotal: Double,
    val dailyTotals: DoubleArray,
    val topValue: Double,
    val startMillis: Long
)

fun getWeekData(transactions: List<TransactionModel>, weekOffset: Int): WeekData {
    val cal = Calendar.getInstance().apply { firstDayOfWeek = Calendar.SUNDAY }
    cal.add(Calendar.WEEK_OF_YEAR, weekOffset)
    cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val startMillis = cal.timeInMillis
    
    val endCal = cal.clone() as Calendar
    endCal.add(Calendar.DAY_OF_YEAR, 6)
    endCal.set(Calendar.HOUR_OF_DAY, 23)
    endCal.set(Calendar.MINUTE, 59)
    endCal.set(Calendar.SECOND, 59)
    val endMillis = endCal.timeInMillis
    
    val startDay = SimpleDateFormat("d", Locale.getDefault()).format(cal.time)
    val endDay = SimpleDateFormat("d", Locale.getDefault()).format(endCal.time)
    val startMonthStr = SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time)
    val endMonthStr = SimpleDateFormat("MMM", Locale.getDefault()).format(endCal.time)
    
    val dateRangeStr = if (startMonthStr == endMonthStr) "$startDay–$endDay $startMonthStr" else "$startDay $startMonthStr – $endDay $endMonthStr"
    
    val dailyTotals = DoubleArray(7) { 0.0 }
    var weekTotal = 0.0
    var maxDaily = 0.0
    
    transactions.forEach { tx ->
        val txMillis = parseDateToMillis(tx.date)
        if (txMillis in startMillis..endMillis) {
            val txCal = Calendar.getInstance().apply { timeInMillis = txMillis }
            val dayIndex = txCal.get(Calendar.DAY_OF_WEEK) - 1 
            if (dayIndex in 0..6) {
                dailyTotals[dayIndex] += tx.amount
                weekTotal += tx.amount
                if (dailyTotals[dayIndex] > maxDaily) maxDaily = dailyTotals[dayIndex]
            }
        }
    }
    
    return WeekData(dateRangeStr, weekTotal, dailyTotals, getRoundedTop(maxDaily), startMillis)
}

fun formatYAxis(value: Double): String {
    return when {
        value >= 10000000 -> String.format(Locale.US, "%.1fCr", value / 10000000)
        value >= 100000 -> String.format(Locale.US, "%.1fL", value / 100000)
        value >= 1000 -> String.format(Locale.US, "%.1fK", value / 1000).replace(".0K", "K")
        else -> value.toInt().toString()
    }
}

fun getRoundedTop(maxVal: Double): Double {
    if (maxVal <= 0) return 1000.0 
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
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { startAnimation = true }
    
    val coroutineScope = rememberCoroutineScope()
    val calendar = Calendar.getInstance().apply { firstDayOfWeek = Calendar.SUNDAY }
    val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) 
    
    val maxAvailableWeeks = remember(transactions) {
        var oldestMillis = Long.MAX_VALUE
        transactions.forEach { tx ->
            val m = parseDateToMillis(tx.date)
            if (m in 1..<oldestMillis) oldestMillis = m
        }
        
        val currCal = Calendar.getInstance().apply { firstDayOfWeek = Calendar.SUNDAY }
        currCal.set(Calendar.HOUR_OF_DAY, 0); currCal.set(Calendar.MINUTE, 0); currCal.set(Calendar.SECOND, 0); currCal.set(Calendar.MILLISECOND, 0)
        currCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val currentWeekStart = currCal.timeInMillis
        
        if (oldestMillis == Long.MAX_VALUE || oldestMillis >= currentWeekStart) {
            1
        } else {
            val oldCal = Calendar.getInstance().apply { timeInMillis = oldestMillis; firstDayOfWeek = Calendar.SUNDAY }
            oldCal.set(Calendar.HOUR_OF_DAY, 0); oldCal.set(Calendar.MINUTE, 0); oldCal.set(Calendar.SECOND, 0); oldCal.set(Calendar.MILLISECOND, 0)
            oldCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            val diffWeeks = ((currentWeekStart - oldCal.timeInMillis) / (7L * 24 * 60 * 60 * 1000L)).toInt()
            (diffWeeks + 1).coerceIn(1, 5)
        }
    }
    
    val pagerState = rememberPagerState(
        initialPage = if (maxAvailableWeeks > 0) maxAvailableWeeks - 1 else 0, 
        pageCount = { maxAvailableWeeks }
    )
    
    LaunchedEffect(maxAvailableWeeks) {
        if (maxAvailableWeeks > 0) {
            pagerState.scrollToPage(maxAvailableWeeks - 1)
        }
    }
    
    val currentDisplayOffset = pagerState.currentPage - (maxAvailableWeeks - 1)
    val headerData = remember(transactions, currentDisplayOffset) { getWeekData(transactions, currentDisplayOffset) }
    
    val canGoBack = pagerState.currentPage > 0
    val canGoForward = pagerState.currentPage < maxAvailableWeeks - 1
    
    val formatRupee = java.text.NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }
    val totalStr = formatRupee.format(headerData.weekTotal).replace("-₹", "-₹ ")

    var touchedBarIndex by remember { mutableIntStateOf(-1) }
    var isScrubbing by remember { mutableStateOf(false) }
    var rowWidthPx by remember { mutableFloatStateOf(1f) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (canGoBack) coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }, 
                    enabled = canGoBack, modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (canGoBack) 1f else 0.3f))
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = totalStr, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = headerData.dateRangeStr, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                IconButton(
                    onClick = { if (canGoForward) coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }, 
                    enabled = canGoForward, modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (canGoForward) 1f else 0.3f))
                }
            }
            
            Spacer(modifier = Modifier.height(36.dp))
            
            Box(modifier = Modifier.fillMaxWidth()) {
                
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
                
                Column(
                    modifier = Modifier
                        .height(80.dp)
                        .width(36.dp)
                        .align(Alignment.TopEnd),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = formatYAxis(headerData.topValue), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.offset(y = (-6).dp)) 
                    Text(text = formatYAxis(headerData.topValue / 2.0), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.offset(y = 0.dp)) 
                    Text(text = "0", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.offset(y = 6.dp)) 
                }
                
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 36.dp)
                        .onGloballyPositioned { rowWidthPx = it.size.width.toFloat() }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val triggerScrub = withTimeoutOrNull(200L) {
                                    var isSwipe = false
                                    do {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.first()
                                        if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                            isSwipe = true
                                            break
                                        }
                                    } while (event.changes.any { it.pressed })
                                    !isSwipe
                                } ?: true

                                if (triggerScrub) {
                                    isScrubbing = true
                                    val sectionWidth = rowWidthPx / 7f
                                    if (sectionWidth > 0) touchedBarIndex = (down.position.x / sectionWidth).toInt().coerceIn(0, 6)

                                    var event = awaitPointerEvent()
                                    while (event.changes.any { it.pressed }) {
                                        val change = event.changes.first()
                                        if (sectionWidth > 0) touchedBarIndex = (change.position.x / sectionWidth).toInt().coerceIn(0, 6)
                                        change.consume()
                                        event = awaitPointerEvent()
                                    }
                                    isScrubbing = false
                                    touchedBarIndex = -1
                                }
                            }
                        }
                ) { pageIndex ->
                    
                    val pageOffset = pageIndex - (maxAvailableWeeks - 1)
                    val pageData = remember(transactions, pageOffset) { getWeekData(transactions, pageOffset) }
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        
                        // --- LAYER 1: BARS & DOTTED LINES ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                            
                            for (i in 0..6) {
                                val targetRatio = if (startAnimation) (pageData.dailyTotals[i] / pageData.topValue).toFloat().coerceIn(0f, 1f) else 0f
                                val animatedRatio by animateFloatAsState(
                                    targetValue = targetRatio,
                                    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                                    label = "barHeightAnim_$i"
                                )
                                
                                val isToday = (pageOffset == 0 && (i + 1) == currentDayOfWeek)
                                val isTouched = touchedBarIndex == i && isScrubbing && pagerState.currentPage == pageIndex
                                val isAnyTouched = touchedBarIndex != -1 && isScrubbing && pagerState.currentPage == pageIndex
                                
                                val barAlpha = if (isAnyTouched && !isTouched) 0.3f else 1f
                                val textOpacity = if (isAnyTouched && !isTouched) 0.3f else if (isToday || isTouched) 1f else 0.5f
                                
                                val primaryColor = MaterialTheme.colorScheme.primary
                                val dynamicTooltipBg = MaterialTheme.colorScheme.onSurface 
                                
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    
                                    Box(
                                        modifier = Modifier
                                            .height(80.dp)
                                            .width(20.dp),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        
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
                                        
                                        if (isTouched) {
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                val barTopY = size.height * (1f - animatedRatio)
                                                
                                                // 🚀 FIX: Dotted Line Starts Exactly from the bottom of Capsule (-40.dp) down to the bottom
                                                drawLine(
                                                    color = dynamicTooltipBg.copy(alpha = 0.9f), // Strong White/Gray for contrast
                                                    start = Offset(size.width / 2f, -40.dp.toPx()), 
                                                    end = Offset(size.width / 2f, size.height),
                                                    strokeWidth = 1.5.dp.toPx(),
                                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f) 
                                                )
                                                
                                                drawCircle(
                                                    color = primaryColor.copy(alpha = 0.3f),
                                                    radius = 10.dp.toPx(),
                                                    center = Offset(size.width / 2f, barTopY),
                                                    style = Fill
                                                )
                                                
                                                drawCircle(
                                                    color = dynamicTooltipBg,
                                                    radius = 4.dp.toPx(),
                                                    center = Offset(size.width / 2f, barTopY),
                                                    style = Fill
                                                )
                                            }
                                        }
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(4.dp)
                                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Text(
                                        text = dayLabels[i],
                                        fontSize = 11.sp,
                                        fontWeight = if (isToday || isTouched) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = textOpacity)
                                    )
                                }
                            }
                        }

                        // --- LAYER 2: THE CLAMPED CAPSULE (TOOLTIP) OVERLAY ---
                        if (isScrubbing && touchedBarIndex in 0..6 && pagerState.currentPage == pageIndex) {
                            var tooltipWidthPx by remember { mutableFloatStateOf(0f) }
                            val density = LocalDensity.current
                            
                            val sectionWidthPx = rowWidthPx / 7f
                            val barCenterXPx = (sectionWidthPx * touchedBarIndex) + (sectionWidthPx / 2f)
                            
                            // Prevents moving out of left (0) or right edges
                            val clampedXPx = (barCenterXPx - (tooltipWidthPx / 2f)).coerceIn(0f, maxOf(0f, rowWidthPx - tooltipWidthPx))
                            val clampedXDp = with(density) { clampedXPx.toDp() }
                            
                            val primaryColor = MaterialTheme.colorScheme.primary
                            val dynamicTooltipBg = MaterialTheme.colorScheme.onSurface 
                            val dynamicTooltipText = MaterialTheme.colorScheme.surface 
                            val dullGreenText = primaryColor.copy(alpha = 0.8f)
                            
                            val touchedDateCal = Calendar.getInstance().apply { 
                                timeInMillis = pageData.startMillis
                                add(Calendar.DAY_OF_YEAR, touchedBarIndex)
                            }
                            val dateStr = SimpleDateFormat("EEE d", Locale.getDefault()).format(touchedDateCal.time)
                            val amtStr = formatRupee.format(pageData.dailyTotals[touchedBarIndex]).replace("-₹", "-₹ ")
                            
                            // 🚀 FIX: Capsule is hard-offset to -40.dp (completely above the graph)
                            // Tail removed, pure pill shape.
                            Box(
                                modifier = Modifier
                                    .offset(x = clampedXDp, y = (-40).dp)
                                    .onGloballyPositioned { tooltipWidthPx = it.size.width.toFloat() }
                                    .background(dynamicTooltipBg, RoundedCornerShape(50)) 
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .zIndex(100f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = amtStr, color = dullGreenText, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "on $dateStr", color = dynamicTooltipText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
