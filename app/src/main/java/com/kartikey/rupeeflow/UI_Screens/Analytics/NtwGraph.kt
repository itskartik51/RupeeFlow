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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.formatRupeeAmount
import com.kartikey.rupeeflow.UI_Screens.CacheManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round

private data class NetworthSlotPoint(
    val globalSlotIndex: Int, // 0 to 17 (6 Months * 3 Slots)
    val label: String,
    val amount: Double
)

// Compact currency formatter for Right Y-Axis markers (e.g. ₹1.5L, ₹75K)
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
    val appDataState by CacheManager.appDataState.collectAsState()
    val appData = appDataState

    // ⚡ Calculate Current Live Net Worth ⚡
    val currentNetworth = remember(appData) {
        if (appData == null) 0.0
        else {
            val cash = appData.cashData.amount
            val banks = appData.bankList.sumOf { it.currentBalance }
            val fds = appData.fdList.sumOf { it.accruedValue }
            val investments = appData.investmentList.sumOf { it.quantity * it.currentPrice }
            val ccDebt = appData.ccList.sumOf { it.outstanding }
            (cash + banks + fds + investments) - ccDebt
        }
    }

    // ⚡ Generate Fixed 6-Month Rolling Window (Always 6 Months) ⚡
    val (monthKeys, monthDisplayNames) = remember {
        val keys = mutableListOf<String>()
        val names = mutableListOf<String>()
        val keyFormat = SimpleDateFormat("yy-MM", Locale.getDefault())
        val nameFormat = SimpleDateFormat("MMM", Locale.getDefault())

        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -5)
        for (i in 0 until 6) {
            keys.add(keyFormat.format(cal.time))
            names.add(nameFormat.format(cal.time))
            cal.add(Calendar.MONTH, 1)
        }
        Pair(keys, names)
    }

    // ⚡ Map Real Recorded Data to the 18-Slot Grid (No Fake Zeros) ⚡
    val activeTimelinePoints = remember(appData, currentNetworth, monthKeys, monthDisplayNames) {
        val list = mutableListOf<NetworthSlotPoint>()
        val history: Map<String, List<Double>> = appData?.networthHistory ?: emptyMap()

        monthKeys.forEachIndexed { monthIndex, mKey ->
            val slots = history[mKey]
            val mName = monthDisplayNames[monthIndex]
            if (slots != null) {
                slots.forEachIndexed { slotIdx, amount ->
                    if (amount > 0.0) {
                        val slotLabel = when (slotIdx) {
                            0 -> "01-10 $mName"
                            1 -> "11-20 $mName"
                            else -> "21-End $mName"
                        }
                        val globalSlot = (monthIndex * 3) + slotIdx
                        list.add(NetworthSlotPoint(globalSlot, slotLabel, amount))
                    }
                }
            }
        }

        // Fallback: Pin current net worth to current month's active slot if no historical records
        if (list.isEmpty()) {
            val cal = Calendar.getInstance()
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val currentSlot = when {
                day <= 10 -> 0
                day <= 20 -> 1
                else -> 2
            }
            val currentGlobalSlot = (5 * 3) + currentSlot // 5 is the current month index
            val mName = monthDisplayNames.last()
            val slotLabel = when (currentSlot) {
                0 -> "01-10 $mName"
                1 -> "11-20 $mName"
                else -> "21-End $mName"
            }
            list.add(NetworthSlotPoint(currentGlobalSlot, slotLabel, currentNetworth))
        }

        list.sortedBy { it.globalSlotIndex }
    }

    // ⚡ Nice-Stepped Round Number Gridline Physics (Solution 1) ⚡
    val (yMin, yMax, yMid) = remember(activeTimelinePoints, currentNetworth) {
        val values = activeTimelinePoints.map { it.amount }
        val minVal = values.minOrNull() ?: currentNetworth
        val maxVal = values.maxOrNull() ?: currentNetworth
        val span = maxVal - minVal

        val rawStep = if (span > 0) (span / 2.0) else (currentNetworth * 0.05).coerceAtLeast(100.0)
        val mag = 10.0.pow(floor(log10(rawStep.coerceAtLeast(1.0))))
        val norm = rawStep / mag
        val niceStep = when {
            norm <= 1.0 -> 1.0 * mag
            norm <= 2.0 -> 2.0 * mag
            norm <= 5.0 -> 5.0 * mag
            else -> 10.0 * mag
        }

        val centerVal = if (span > 0) (minVal + maxVal) / 2.0 else currentNetworth
        val midIndex = round(centerVal / niceStep)
        val calculatedMid = midIndex * niceStep
        val calculatedMin = calculatedMid - niceStep
        val calculatedMax = calculatedMid + niceStep

        Triple(calculatedMin, calculatedMax, calculatedMid)
    }

    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    val noRippleInteractionSource = remember { MutableInteractionSource() }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(activeTimelinePoints.size) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
        )
    }

    val textMeasurer = rememberTextMeasurer()
    val lineColor = Color(0xFF7C4DFF)
    val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
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

            // --- TOP HEADER ROW: Left Title & Right-Aligned Leftward Expanding Amount ---
            val activePoint = selectedPointIndex?.let { activeTimelinePoints.getOrNull(it) }
            val displayAmount = activePoint?.amount ?: currentNetworth

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NET WORTH",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.50f, fill = false)
                )

                // Right Amount Container (Leftward Growth with max 50% width ceiling)
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(0.50f, fill = false)
                ) {
                    Text(
                        text = formatRupeeAmount(displayAmount),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                    if (activePoint != null) {
                        Text(
                            text = activePoint.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = lineColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- GRAPH CANVAS: 6 Months X-Axis + Nice Stepped Gridlines + Zero Right Gap ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(activeTimelinePoints) {
                            detectTapGestures(
                                onTap = { offset ->
                                    if (activeTimelinePoints.isEmpty()) return@detectTapGestures
                                    val totalGridSlots = 17f
                                    val topLayout = textMeasurer.measure(formatCompactRupee(yMax), TextStyle(fontSize = 9.sp))
                                    val midLayout = textMeasurer.measure(formatCompactRupee(yMid), TextStyle(fontSize = 9.sp))
                                    val botLayout = textMeasurer.measure(formatCompactRupee(yMin), TextStyle(fontSize = 9.sp))
                                    val labelMaxWidth = maxOf(topLayout.size.width, midLayout.size.width, botLayout.size.width).toFloat()
                                    val chartWidth = size.width - labelMaxWidth - 6.dp.toPx()

                                    var closestIdx = -1
                                    var minDistance = Float.MAX_VALUE
                                    activeTimelinePoints.forEachIndexed { index, point ->
                                        val px = (point.globalSlotIndex / totalGridSlots) * chartWidth
                                        val dist = abs(px - offset.x)
                                        if (dist < minDistance) {
                                            minDistance = dist
                                            closestIdx = index
                                        }
                                    }

                                    selectedPointIndex = if (selectedPointIndex == closestIdx) null else closestIdx
                                }
                            )
                        }
                        .pointerInput(activeTimelinePoints) {
                            detectDragGestures(
                                onDragEnd = { },
                                onDragCancel = { selectedPointIndex = null },
                                onDrag = { change, _ ->
                                    if (activeTimelinePoints.isEmpty()) return@detectDragGestures
                                    val totalGridSlots = 17f
                                    val topLayout = textMeasurer.measure(formatCompactRupee(yMax), TextStyle(fontSize = 9.sp))
                                    val midLayout = textMeasurer.measure(formatCompactRupee(yMid), TextStyle(fontSize = 9.sp))
                                    val botLayout = textMeasurer.measure(formatCompactRupee(yMin), TextStyle(fontSize = 9.sp))
                                    val labelMaxWidth = maxOf(topLayout.size.width, midLayout.size.width, botLayout.size.width).toFloat()
                                    val chartWidth = size.width - labelMaxWidth - 6.dp.toPx()

                                    var closestIdx = 0
                                    var minDistance = Float.MAX_VALUE
                                    activeTimelinePoints.forEachIndexed { index, point ->
                                        val px = (point.globalSlotIndex / totalGridSlots) * chartWidth
                                        val dist = abs(px - change.position.x)
                                        if (dist < minDistance) {
                                            minDistance = dist
                                            closestIdx = index
                                        }
                                    }
                                    selectedPointIndex = closestIdx
                                }
                            )
                        }
                ) {
                    val topLayout = textMeasurer.measure(formatCompactRupee(yMax), TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Medium, color = labelColor))
                    val midLayout = textMeasurer.measure(formatCompactRupee(yMid), TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Medium, color = labelColor))
                    val botLayout = textMeasurer.measure(formatCompactRupee(yMin), TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Medium, color = labelColor))
                    val labelMaxWidth = maxOf(topLayout.size.width, midLayout.size.width, botLayout.size.width).toFloat()

                    val chartWidth = size.width - labelMaxWidth - 6.dp.toPx()
                    val bottomXAxisHeight = 18.dp.toPx()
                    val chartHeight = size.height - bottomXAxisHeight

                    val yValRange = (yMax - yMin).toFloat().coerceAtLeast(1f)

                    fun getYCoordinate(value: Double): Float {
                        val normalized = ((value - yMin) / yValRange).toFloat()
                        return chartHeight - (normalized * chartHeight)
                    }

                    // --- 1. Draw 3 Dashed Stepped Gridlines & Right Labels ---
                    val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    val gridLevels = listOf(
                        Pair(yMax, topLayout),
                        Pair(yMid, midLayout),
                        Pair(yMin, botLayout)
                    )

                    gridLevels.forEach { (levelVal, layout) ->
                        val yPos = getYCoordinate(levelVal)
                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, yPos),
                            end = Offset(chartWidth, yPos),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashedEffect
                        )

                        drawText(
                            textLayoutResult = layout,
                            topLeft = Offset(size.width - layout.size.width, yPos - (layout.size.height / 2f))
                        )
                    }

                    // --- 2. Calculate Screen Coordinates on 18-Slot Grid ---
                    val totalGridSlots = 17f
                    val screenPoints = activeTimelinePoints.map { point ->
                        val x = (point.globalSlotIndex / totalGridSlots) * chartWidth
                        val y = getYCoordinate(point.amount)
                        Offset(x, y)
                    }

                    // --- 3. Render Area & Spline Wave ---
                    if (screenPoints.size >= 2) {
                        val strokePath = Path()
                        val fillPath = Path()

                        strokePath.moveTo(screenPoints.first().x, screenPoints.first().y)
                        fillPath.moveTo(screenPoints.first().x, chartHeight)
                        fillPath.lineTo(screenPoints.first().x, screenPoints.first().y)

                        for (i in 0 until screenPoints.size - 1) {
                            val p0 = screenPoints[i]
                            val p1 = screenPoints[i + 1]
                            val cp1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                            val cp2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)

                            strokePath.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p1.x, p1.y)
                            fillPath.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p1.x, p1.y)
                        }

                        fillPath.lineTo(screenPoints.last().x, chartHeight)
                        fillPath.close()

                        val underFillBrush = Brush.verticalGradient(
                            colors = listOf(
                                lineColor.copy(alpha = 0.35f * animProgress.value),
                                lineColor.copy(alpha = 0.06f * animProgress.value),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = chartHeight
                        )
                        drawPath(path = fillPath, brush = underFillBrush)

                        drawPath(
                            path = strokePath,
                            color = lineColor.copy(alpha = animProgress.value),
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // --- 4. Reference Line & Glowing Indicator Dot ---
                    val activeIndex = selectedPointIndex ?: (screenPoints.size - 1)
                    val targetPoint = screenPoints.getOrNull(activeIndex)

                    if (targetPoint != null) {
                        drawLine(
                            color = lineColor.copy(alpha = 0.35f),
                            start = Offset(0f, targetPoint.y),
                            end = Offset(chartWidth, targetPoint.y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashedEffect
                        )

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

                    // --- 5. Always Render Fixed 6 Months on X-Axis ---
                    val mStepX = chartWidth / 5f
                    monthDisplayNames.forEachIndexed { idx, mName ->
                        val xPos = idx * mStepX
                        val mLayout = textMeasurer.measure(
                            text = mName,
                            style = TextStyle(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = labelColor
                            )
                        )
                        val textX = when (idx) {
                            0 -> 0f
                            5 -> (chartWidth - mLayout.size.width)
                            else -> (xPos - (mLayout.size.width / 2f)).coerceIn(0f, chartWidth - mLayout.size.width)
                        }
                        drawText(
                            textLayoutResult = mLayout,
                            topLeft = Offset(textX, size.height - mLayout.size.height)
                        )
                    }
                }
            }
        }
    }
}
