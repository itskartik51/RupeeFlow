package com.kartikey.rupeeflow.UI_Screens.Analytics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.formatRupeeAmount
import com.kartikey.rupeeflow.UI_Screens.CacheManager
import com.kartikey.rupeeflow.UI_Screens.NetworthDataPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// Compact currency formatter for Right Y-Axis markers (e.g. ₹1.5L, ₹50K)
private fun formatCompactRupee(amount: Double): String {
    val absVal = abs(amount)
    val sign = if (amount < 0) "-" else ""
    return when {
        absVal >= 10_000_000 -> "${sign}₹${String.format(Locale.US, "%.1f", absVal / 10_000_000)}Cr"
        absVal >= 100_000 -> "${sign}₹${String.format(Locale.US, "%.1f", absVal / 100_000)}L"
        absVal >= 1_000 -> "${sign}₹${String.format(Locale.US, "%.0f", absVal / 1_000)}K"
        else -> "${sign}₹${absVal.toInt()}"
    }
}

@Composable
fun NtwGraphCard(modifier: Modifier = Modifier) {
    val appDataState by CacheManager.appDataState.collectAsState()[cite: 1]
    val appData = appDataState

    // ⚡ Calculate Current Live Net Worth ⚡
    val currentNetworth = remember(appData) {
        if (appData == null) 0.0
        else {
            val cash = appData.cashData.amount[cite: 1]
            val banks = appData.bankList.sumOf { it.currentBalance }[cite: 1]
            val fds = appData.fdList.sumOf { it.accruedValue }[cite: 1]
            val investments = appData.investmentList.sumOf { it.quantity * it.currentPrice }[cite: 1]
            val ccDebt = appData.ccList.sumOf { it.outstanding }[cite: 1]
            (cash + banks + fds + investments) - ccDebt
        }
    }

    // ⚡ Build 6-Month Timeline Data Points ⚡
    val timelineData = remember(appData, currentNetworth) {
        val list = mutableListOf<NetworthDataPoint>()
        val monthNameFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val parseFormat = SimpleDateFormat("yy-MM", Locale.getDefault())

        val history = appData?.networthHistory ?: emptyMap()[cite: 1]
        val sortedKeys = history.keys.sorted()

        for (mKey in sortedKeys) {
            val slots = history[mKey] ?: continue
            val monthDate = try { parseFormat.parse(mKey) } catch (e: Exception) { null }
            val mName = if (monthDate != null) monthNameFormat.format(monthDate) else mKey

            slots.forEachIndexed { index, amount ->
                if (amount > 0.0) {
                    val label = when (index) {
                        0 -> "01-10 $mName"
                        1 -> "11-20 $mName"
                        else -> "21-End $mName"
                    }
                    list.add(NetworthDataPoint(mKey, index, label, amount))[cite: 1]
                }
            }
        }

        // Fallback if history is empty
        if (list.isEmpty()) {
            val cal = Calendar.getInstance()
            val currMonth = monthNameFormat.format(cal.time)
            val currKey = SimpleDateFormat("yy-MM", Locale.getDefault()).format(cal.time)
            list.add(NetworthDataPoint(currKey, 0, "01-10 $currMonth", currentNetworth))[cite: 1]
            list.add(NetworthDataPoint(currKey, 1, "11-20 $currMonth", currentNetworth))[cite: 1]
            list.add(NetworthDataPoint(currKey, 2, "21-End $currMonth", currentNetworth))[cite: 1]
        }
        list
    }

    // ⚡ 1. Dynamic Min-Max Scaling Engine with 15% Buffer ⚡
    val (yMin, yMax, yMid) = remember(timelineData) {
        val values = timelineData.map { it.amount }
        val minVal = values.minOrNull() ?: 0.0
        val maxVal = values.maxOrNull() ?: 0.0
        val range = maxVal - minVal

        if (range <= 0.0001) {
            // Edge Case: Flat Data
            val fallbackMin = if (maxVal != 0.0) maxVal * 0.95 else -100.0
            val fallbackMax = if (maxVal != 0.0) maxVal * 1.05 else 100.0
            Triple(fallbackMin, fallbackMax, (fallbackMin + fallbackMax) / 2.0)
        } else {
            // 15% Range Buffer Formula
            val padding = range * 0.15
            val calculatedMin = minVal - padding
            val calculatedMax = maxVal + padding
            Triple(calculatedMin, calculatedMax, (calculatedMin + calculatedMax) / 2.0)
        }
    }

    // Interactive Touch State
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    val noRippleInteractionSource = remember { MutableInteractionSource() }

    // Wave Reveal Animation
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(timelineData.size) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
        )
    }

    val textMeasurer = rememberTextMeasurer()
    val lineColor = Color(0xFF7C4DFF) // Electric Radiant Violet
    val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = noRippleInteractionSource,
                indication = null
            ) {
                selectedPointIndex = null
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 14.dp)) {

            // --- HEADER: Net Worth Title & Amount (Left-Aligned) ---
            val activePoint = selectedPointIndex?.let { timelineData.getOrNull(it) }
            val displayAmount = activePoint?.amount ?: currentNetworth
            val displayLabel = activePoint?.label ?: "Current"

            Text(
                text = "NET WORTH",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatRupeeAmount(displayAmount),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "($displayLabel)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (activePoint != null) lineColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- GRAPH CANVAS: Smooth Cubic Spline + 3-Gridlines + Gradient Under-Fill ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(timelineData) {
                            detectTapGestures(
                                onTap = { offset ->
                                    val yAxisLabelWidth = 52.dp.toPx()
                                    val chartWidth = size.width - yAxisLabelWidth
                                    if (timelineData.size > 1 && offset.x <= chartWidth) {
                                        val stepX = chartWidth / (timelineData.size - 1)
                                        val tappedIndex = ((offset.x + stepX / 2f) / stepX).toInt().coerceIn(0, timelineData.size - 1)
                                        selectedPointIndex = if (selectedPointIndex == tappedIndex) null else tappedIndex
                                    } else {
                                        selectedPointIndex = null
                                    }
                                }
                            )
                        }
                        .pointerInput(timelineData) {
                            detectDragGestures(
                                onDragEnd = { /* retain selection or clear */ },
                                onDragCancel = { selectedPointIndex = null },
                                onDrag = { change, _ ->
                                    val yAxisLabelWidth = 52.dp.toPx()
                                    val chartWidth = size.width - yAxisLabelWidth
                                    if (timelineData.size > 1 && change.position.x <= chartWidth) {
                                        val stepX = chartWidth / (timelineData.size - 1)
                                        val draggedIndex = ((change.position.x + stepX / 2f) / stepX).toInt().coerceIn(0, timelineData.size - 1)
                                        selectedPointIndex = draggedIndex
                                    }
                                }
                            )
                        }
                ) {
                    val yAxisLabelWidth = 52.dp.toPx()
                    val chartWidth = size.width - yAxisLabelWidth
                    val bottomXAxisHeight = 18.dp.toPx()
                    val chartHeight = size.height - bottomXAxisHeight

                    val yValRange = (yMax - yMin).toFloat().coerceAtLeast(1f)

                    fun getYCoordinate(value: Double): Float {
                        val normalized = ((value - yMin) / yValRange).toFloat()
                        return chartHeight - (normalized * chartHeight)
                    }

                    // --- 1. Draw 3 Faint Dashed Gridlines & Right Y-Labels ---
                    val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    val gridLevels = listOf(yMax, yMid, yMin)

                    gridLevels.forEach { levelVal ->
                        val yPos = getYCoordinate(levelVal)
                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, yPos),
                            end = Offset(chartWidth, yPos),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashedEffect
                        )

                        // Y-Axis Right Label
                        val labelText = formatCompactRupee(levelVal)
                        val textLayout = textMeasurer.measure(
                            text = labelText,
                            style = TextStyle(
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = labelColor
                            )
                        )
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(chartWidth + 6.dp.toPx(), yPos - (textLayout.size.height / 2f))
                        )
                    }

                    if (timelineData.isEmpty()) return@Canvas

                    // --- 2. Calculate Screen Coordinates for Data Points ---
                    val stepX = if (timelineData.size > 1) chartWidth / (timelineData.size - 1) else chartWidth
                    val points = timelineData.mapIndexed { index, point ->
                        val x = index * stepX
                        val y = getYCoordinate(point.amount)
                        Offset(x, y)
                    }

                    // --- 3. Build Smooth Cubic Spline (Monotone Bezier Path) ---
                    val strokePath = Path()
                    val fillPath = Path()

                    if (points.isNotEmpty()) {
                        strokePath.moveTo(points.first().x, points.first().y)
                        fillPath.moveTo(points.first().x, chartHeight)
                        fillPath.lineTo(points.first().x, points.first().y)

                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val cp1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                            val cp2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)

                            strokePath.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p1.x, p1.y)
                            fillPath.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p1.x, p1.y)
                        }

                        fillPath.lineTo(points.last().x, chartHeight)
                        fillPath.close()
                    }

                    // --- 4. Render Gradient Under-Fill ---
                    val underFillBrush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.35f * animProgress.value),
                            lineColor.copy(alpha = 0.08f * animProgress.value),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = chartHeight
                    )
                    drawPath(path = fillPath, brush = underFillBrush)

                    // --- 5. Render Glowing Stroke Wave ---
                    drawPath(
                        path = strokePath,
                        color = lineColor.copy(alpha = animProgress.value),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // --- 6. Reference Line & End/Selected Glow Dot ---
                    val activeIndex = selectedPointIndex ?: (points.size - 1)
                    val targetPoint = points.getOrNull(activeIndex)

                    if (targetPoint != null) {
                        // Dashed horizontal reference line crossing the dot
                        drawLine(
                            color = lineColor.copy(alpha = 0.40f),
                            start = Offset(0f, targetPoint.y),
                            end = Offset(chartWidth, targetPoint.y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashedEffect
                        )

                        // Outer Glowing Ring Dot
                        drawCircle(
                            color = lineColor.copy(alpha = 0.25f),
                            radius = 7.dp.toPx(),
                            center = targetPoint
                        )
                        drawCircle(
                            color = lineColor,
                            radius = 4.dp.toPx(),
                            center = targetPoint
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = targetPoint
                        )
                    }

                    // --- 7. Bottom X-Axis Month Markers ---
                    val distinctMonths = timelineData.map { it.label.substringAfterLast(" ") }.distinct()
                    if (distinctMonths.isNotEmpty()) {
                        val mStepX = if (distinctMonths.size > 1) chartWidth / (distinctMonths.size - 1) else chartWidth / 2f
                        distinctMonths.forEachIndexed { idx, mName ->
                            val xPos = idx * mStepX
                            val mLayout = textMeasurer.measure(
                                text = mName,
                                style = TextStyle(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = labelColor
                                )
                            )
                            drawText(
                                textLayoutResult = mLayout,
                                topLeft = Offset(
                                    (xPos - (mLayout.size.width / 2f)).coerceIn(0f, chartWidth - mLayout.size.width),
                                    size.height - mLayout.size.height
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
