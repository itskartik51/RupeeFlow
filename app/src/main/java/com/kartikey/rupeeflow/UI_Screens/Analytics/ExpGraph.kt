package com.kartikey.rupeeflow.UI_Screens.Analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.formatRupeeAmount
import com.kartikey.rupeeflow.UI_Screens.CacheManager
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.atan2

data class CategorySpendItem(
    val category: String,
    val totalAmount: Double,
    val percentage: Double,
    val color: Color,
    val startAngle: Float = 0f,
    val sweepAngle: Float = 0f
)

private val CategoryColorPalette = mapOf(
    "Food" to Color(0xFFFF7043),
    "Groceries" to Color(0xFF4CAF50),
    "Transport" to Color(0xFF42A5F5),
    "Fuel" to Color(0xFFFFA726),
    "Shopping" to Color(0xFFAB47BC),
    "Bills" to Color(0xFFEF5350),
    "Rent" to Color(0xFF8D6E63),
    "EMI" to Color(0xFF26A69A),
    "Subscription" to Color(0xFFEC407A),
    "Gift" to Color(0xFF7E57C2),
    "Personal Care" to Color(0xFF26C6DA),
    "Health" to Color(0xFF66BB6A),
    "Education" to Color(0xFF5C6BC0),
    "Entertainment" to Color(0xFFFFCA28),
    "Custom" to Color(0xFF78909C)
)

@Composable
fun ExpGraphCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appDataState by CacheManager.appDataState.collectAsState()
    val transactions = appDataState?.transactionList ?: emptyList()

    var selectedFilter by remember { mutableStateOf("This Month") }
    val filterOptions = listOf("This Month", "Last 30 Days", "This Year")

    var selectedCategoryName by remember { mutableStateOf<String?>(null) }
    var isExpanded by remember { mutableStateOf(false) }

    // ⚡ Real-Time In-Memory Aggregation ⚡
    val (filteredCategories, grandTotal) = remember(transactions, selectedFilter) {
        val now = Calendar.getInstance()
        val currMonthStr = String.format(Locale.US, "%02d", now.get(Calendar.MONTH) + 1)
        val currYearStr = now.get(Calendar.YEAR).toString()
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)

        val formatSlash = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val formatDash = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val validTx = transactions.filter { tx ->
            val dStr = tx.date.split(" ")[0].trim()
            when (selectedFilter) {
                "This Month" -> {
                    dStr.contains("/$currMonthStr/$currYearStr") || dStr.startsWith("$currYearStr-$currMonthStr")
                }
                "Last 30 Days" -> {
                    val txTime = try {
                        if (dStr.contains("/")) formatSlash.parse(dStr)?.time
                        else formatDash.parse(dStr)?.time
                    } catch (e: Exception) { null } ?: 0L
                    txTime >= thirtyDaysAgo
                }
                "This Year" -> {
                    dStr.contains(currYearStr)
                }
                else -> true
            }
        }

        val grouped = validTx.groupBy { it.category.ifBlank { "Custom" } }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .filter { it.value > 0.0 }

        val total = grouped.values.sum()
        var currentAngle = -90f // Start from top (12 o'clock)

        val list = grouped.entries.sortedByDescending { it.value }.map { entry ->
            val pct = if (total > 0) (entry.value / total) * 100.0 else 0.0
            val sweep = ((pct / 100.0) * 360.0).toFloat()
            val color = CategoryColorPalette[entry.key] ?: Color(0xFF78909C)
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

    // Smooth Sweep Animation on Filter Change
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(selectedFilter, filteredCategories.size) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // --- HEADER & FILTER CHIPS ---
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

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    filterOptions.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .clickable {
                                    selectedFilter = filter
                                    selectedCategoryName = null
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = filter,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (filteredCategories.isEmpty() || grandTotal <= 0.0) {
                // Empty State Placeholder Ring
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(180.dp)) {
                        drawCircle(
                            color = Color(0xFFEEEEEE),
                            style = Stroke(width = 24.dp.toPx())
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No Expenses", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("In $selectedFilter", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                // --- INTERACTIVE DONUT CHART CANVAS ---
                val activeCategory = filteredCategories.find { it.category == selectedCategoryName }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(190.dp)
                            .pointerInput(filteredCategories) {
                                detectTapGestures { tapOffset ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val dx = tapOffset.x - center.x
                                    val dy = tapOffset.y - center.y
                                    var touchAngle = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f

                                    // Match touch angle with slice angle (-90deg offset shift)
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
                                }
                            }
                    ) {
                        val strokeWidth = 26.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                        val arcSize = Size(diameter, diameter)

                        filteredCategories.forEach { item ->
                            val isSelected = item.category == selectedCategoryName
                            val currentStroke = if (isSelected) strokeWidth + 6.dp.toPx() else strokeWidth

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

                    // Dynamic Center Text
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .clickable { selectedCategoryName = null }
                    ) {
                        if (activeCategory != null) {
                            Text(
                                text = activeCategory.category,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeCategory.color,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatRupeeAmount(activeCategory.totalAmount),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", activeCategory.percentage)}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Total Spent",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatRupeeAmount(grandTotal),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = selectedFilter,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // --- RANKED CATEGORY BREAKDOWN LIST ---
                val displayedItems = if (isExpanded) filteredCategories else filteredCategories.take(4)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    displayedItems.forEach { item ->
                        val isSelected = item.category == selectedCategoryName
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) item.color.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable {
                                    selectedCategoryName = if (isSelected) null else item.category
                                }
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(item.color, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = item.category,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatRupeeAmount(item.totalAmount),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "(${String.format(Locale.US, "%.1f", item.percentage)}%)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Show All / Show Less Button
                if (filteredCategories.size > 4) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick { isExpanded = !isExpanded }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isExpanded) "Show Less" else "Show All (${filteredCategories.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
