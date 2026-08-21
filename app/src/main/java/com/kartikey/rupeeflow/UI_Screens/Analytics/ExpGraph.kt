package com.kartikey.rupeeflow.UI_Screens.Analytics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.formatRupeeAmount
import com.kartikey.rupeeflow.UI_Screens.CacheManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.sqrt

data class CategorySpendItem(
    val category: String,
    val totalAmount: Double,
    val percentage: Double,
    val color: Color,
    val startAngle: Float = 0f,
    val sweepAngle: Float = 0f
)

// ⚡ High-Contrast Vibrant Palette (Optimized for both Light & Dark themes) ⚡
private val NeonColorPalette = listOf(
    Color(0xFF00B4D8), // Vibrant Cyan Blue
    Color(0xFFFF6D00), // Neon Deep Orange
    Color(0xFF00C853), // Electric Vivid Green
    Color(0xFFD500F9), // Neon Magenta
    Color(0xFFFFAB00), // Rich Amber Gold
    Color(0xFFFF1744), // Electric Vivid Red
    Color(0xFF7C4DFF), // Electric Violet
    Color(0xFF00BFA5), // Deep Neon Aqua Teal
    Color(0xFFFF4081), // Vivid Hot Pink
    Color(0xFF64DD17), // Vivid Electric Lime
    Color(0xFF0091EA), // Rich Sky Blue
    Color(0xFFFF9100), // Pure Amber Glow
    Color(0xFF651FFF), // Deep Electric Indigo
    Color(0xFF00897B), // Rich Teal Glow
    Color(0xFFFF8F00), // Radiant Deep Gold
    Color(0xFFF50057)  // Deep Rose Neon
)

@Composable
fun ExpGraphCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appDataState by CacheManager.appDataState.collectAsState()
    val transactions = appDataState?.transactionList ?: emptyList()

    var selectedFilter by remember { mutableStateOf("This Month") }
    var isCapsuleExpanded by remember { mutableStateOf(false) }
    val filterOptions = listOf("Today", "This Month", "Last 30 Days", "This Year", "Last 365 Days")

    var selectedCategoryName by remember { mutableStateOf<String?>(null) }
    val emptyRingColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val noRippleInteractionSource = remember { MutableInteractionSource() }

    // ⚡ Real-Time Filter Aggregation (Sorted by Highest Spend First) ⚡
    val (filteredCategories, grandTotal) = remember(transactions, selectedFilter) {
        val now = Calendar.getInstance()
        val currDayStr = String.format(Locale.US, "%02d", now.get(Calendar.DAY_OF_MONTH))
        val currMonthStr = String.format(Locale.US, "%02d", now.get(Calendar.MONTH) + 1)
        val currYearStr = now.get(Calendar.YEAR).toString()
        val todaySlash = "$currDayStr/$currMonthStr/$currYearStr"
        val todayDash = "$currYearStr-$currMonthStr-$currDayStr"

        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val yearAgoDays = System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000)

        val formatSlash = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val formatDash = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val validTx = transactions.filter { tx ->
            val dStr = tx.date.split(" ")[0].trim()
            when (selectedFilter) {
                "Today" -> dStr.startsWith(todaySlash) || dStr.startsWith(todayDash)
                "This Month" -> dStr.contains("/$currMonthStr/$currYearStr") || dStr.startsWith("$currYearStr-$currMonthStr")
                "Last 30 Days" -> {
                    val txTime = try {
                        if (dStr.contains("/")) formatSlash.parse(dStr)?.time
                        else formatDash.parse(dStr)?.time
                    } catch (e: Exception) { null } ?: 0L
                    txTime >= thirtyDaysAgo
                }
                "This Year" -> dStr.contains(currYearStr)
                "Last 365 Days" -> {
                    val txTime = try {
                        if (dStr.contains("/")) formatSlash.parse(dStr)?.time
                        else formatDash.parse(dStr)?.time
                    } catch (e: Exception) { null } ?: 0L
                    txTime >= yearAgoDays
                }
                else -> true
            }
        }

        val grouped = validTx.groupBy { it.category.ifBlank { "Custom" } }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .filter { it.value > 0.0 }

        val total = grouped.values.sum()
        var currentAngle = -90f

        // Highest spend sorted to the top
        val list = grouped.entries.sortedByDescending { it.value }.mapIndexed { index, entry ->
            val pct = if (total > 0) (entry.value / total) * 100.0 else 0.0
            val sweep = ((pct / 100.0) * 360.0).toFloat()
            val color = NeonColorPalette[index % NeonColorPalette.size]
            val item = CategorySpendItem(
                category = entry.key,
                totalAmount = entry.value,
                percentage = pct,
                color = color,
                startAngle = currentAngle,
                sweepAngle = sweep
            )
            currentAngle += sweep
            item
        }

        Pair(list, total)
    }

    // Smooth Sweep Animation
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(selectedFilter, filteredCategories.size) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = noRippleInteractionSource,
                indication = null
            ) {
                // Card-wide Tap-Outside Reset for selected slice
                selectedCategoryName = null
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)) {

            // --- TOP ROW: Title & Floating Capsule Dropdown Anchor ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SPENDING BREAKDOWN",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                // ⚡ Floating Capsule Filter (Zero Layout Shift) ⚡
                Box {
                    Row(
                        modifier = Modifier
                            .width(132.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { isCapsuleExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedFilter,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (isCapsuleExpanded) {
                        Popup(
                            alignment = Alignment.TopEnd,
                            onDismissRequest = { isCapsuleExpanded = false },
                            properties = PopupProperties(focusable = true)
                        ) {
                            Surface(
                                modifier = Modifier.width(132.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 8.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                            ) {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    filterOptions.forEach { option ->
                                        val isSelected = selectedFilter == option
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedFilter = option
                                                    selectedCategoryName = null
                                                    isCapsuleExpanded = false
                                                }
                                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                                                .padding(horizontal = 12.dp, vertical = 7.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = option,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (option == selectedFilter) {
                                                Icon(
                                                    imageVector = Icons.Outlined.KeyboardArrowUp,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- GRAPH & RIGHT LEGEND SECTION ---
            if (filteredCategories.isEmpty() || grandTotal <= 0.0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(155.dp)) {
                        drawCircle(
                            color = emptyRingColor,
                            style = Stroke(width = 22.dp.toPx())
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No Expenses", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(selectedFilter, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                val activeCategory = filteredCategories.find { it.category == selectedCategoryName }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // --- LEFT: 55% Donut Chart ---
                    Box(
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(
                            modifier = Modifier
                                .size(155.dp)
                                .pointerInput(filteredCategories) {
                                    detectTapGestures { tapOffset ->
                                        val center = Offset(size.width / 2f, size.height / 2f)
                                        val dx = tapOffset.x - center.x
                                        val dy = tapOffset.y - center.y
                                        val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                                        val strokeWidthPx = 22.dp.toPx()
                                        val outerRadiusPx = size.minDimension / 2f
                                        val innerRadiusPx = outerRadiusPx - strokeWidthPx

                                        if (dist in (innerRadiusPx - 10.dp.toPx())..(outerRadiusPx + 10.dp.toPx())) {
                                            val touchAngle = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f
                                            val normalizedTouch = (touchAngle + 90f) % 360f

                                            var cumulative = 0f
                                            var matched: String? = null
                                            for (item in filteredCategories) {
                                                if (normalizedTouch >= cumulative && normalizedTouch <= cumulative + item.sweepAngle) {
                                                    matched = item.category
                                                    break
                                                }
                                                cumulative += item.sweepAngle
                                            }

                                            selectedCategoryName = if (selectedCategoryName == matched) null else matched
                                        } else {
                                            selectedCategoryName = null
                                        }
                                    }
                                }
                        ) {
                            val strokeWidth = 22.dp.toPx()
                            val diameter = size.minDimension - strokeWidth
                            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                            val arcSize = Size(diameter, diameter)

                            filteredCategories.forEach { item ->
                                val isSelected = item.category == selectedCategoryName
                                val currentStroke = if (isSelected) strokeWidth + 5.dp.toPx() else strokeWidth

                                drawArc(
                                    color = item.color,
                                    startAngle = item.startAngle,
                                    sweepAngle = item.sweepAngle * animationProgress.value,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = currentStroke, cap = StrokeCap.Butt)
                                )
                            }
                        }

                        // Center Text
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(horizontal = 14.dp)
                                .clickable(
                                    interactionSource = noRippleInteractionSource,
                                    indication = null
                                ) {
                                    selectedCategoryName = null
                                }
                        ) {
                            if (activeCategory != null) {
                                Text(
                                    text = activeCategory.category,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = activeCategory.color,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatRupeeAmount(activeCategory.totalAmount),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", activeCategory.percentage)}%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "Total Spent",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatRupeeAmount(grandTotal),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = selectedFilter,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // --- 5% GAP ---
                    Spacer(modifier = Modifier.weight(0.05f))

                    // --- RIGHT: Compact Bottom-Aligned Legends (Strict Straight Line) ---
                    Column(
                        modifier = Modifier
                            .weight(0.40f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.Start
                    ) {
                        val legendItems = filteredCategories.take(5)
                        legendItems.forEach { item ->
                            val isSelected = item.category == selectedCategoryName
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelected) item.color.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable(
                                        interactionSource = noRippleInteractionSource,
                                        indication = null
                                    ) {
                                        selectedCategoryName = if (isSelected) null else item.category
                                    }
                                    .padding(vertical = 2.5.dp, horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                // Strictly vertically aligned micro square
                                Box(
                                    modifier = Modifier
                                        .size(6.5.dp)
                                        .background(item.color, RoundedCornerShape(1.5.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.category,
                                    fontSize = 9.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) item.color else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (filteredCategories.size > 5) {
                            Text(
                                text = "+${filteredCategories.size - 5} more",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 12.5.dp, top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
