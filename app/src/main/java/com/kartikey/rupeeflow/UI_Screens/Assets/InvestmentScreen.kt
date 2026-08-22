package com.kartikey.rupeeflow.UI_Screens.Assets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentScreen(
    onBackClick: () -> Unit, 
    username: String, 
    investmentList: List<InvestmentItem>,
    isLoading: Boolean = false, 
    onRefreshClick: () -> Unit = {}
) { 
    val totalInvested = investmentList.sumOf { it.quantity * it.avgBuyPrice }
    val totalCurrent = investmentList.sumOf { it.quantity * it.currentPrice }
    val total1DChange = investmentList.sumOf { it.quantity * it.oneDayChangePrice }
    val totalReturn = totalCurrent - totalInvested
    val totalReturnPercent = if (totalInvested > 0) (totalReturn / totalInvested) * 100 else 0.0
    val total1DPercent = if (totalCurrent - total1DChange > 0) (total1DChange / (totalCurrent - total1DChange)) * 100 else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Investments", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.bounceClick()) { 
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface) 
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                
                InvestmentSummaryCard(
                    itemCount = investmentList.size,
                    totalCurrent = totalCurrent,
                    total1DChange = total1DChange,
                    total1DPercent = total1DPercent,
                    totalReturn = totalReturn,
                    totalReturnPercent = totalReturnPercent,
                    totalInvested = totalInvested,
                    isLoading = isLoading, 
                    onRefreshClick = onRefreshClick
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                ListHeaderRow()
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(investmentList) { item ->
                InvestmentListItem(item)
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), 
                    thickness = 1.dp, 
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun InvestmentSummaryCard(
    itemCount: Int, 
    totalCurrent: Double, 
    total1DChange: Double, 
    total1DPercent: Double, 
    totalReturn: Double, 
    totalReturnPercent: Double, 
    totalInvested: Double,
    isLoading: Boolean, 
    onRefreshClick: () -> Unit = {}
) {
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
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INVESTMENT ($itemCount)", 
                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold, 
                    letterSpacing = 1.sp
                )
                Row {
                    IconButton(onClick = { }, modifier = Modifier.size(32.dp).bounceClick()) {
                        Icon(Icons.Outlined.Visibility, contentDescription = "Hide", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onRefreshClick, modifier = Modifier.size(32.dp).bounceClick()) {
                        Icon(
                            Icons.Outlined.Refresh, 
                            contentDescription = "Refresh", 
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(if (isLoading) angle else 0f) 
                        )
                    }
                    IconButton(onClick = { }, modifier = Modifier.size(32.dp).bounceClick()) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Text(
                text = formatRupee(totalCurrent), 
                fontWeight = FontWeight.ExtraBold, 
                fontSize = 32.sp, 
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            SummaryRow("1D returns", total1DChange, total1DPercent)
            Spacer(modifier = Modifier.height(12.dp))
            
            SummaryRow("Total returns", totalReturn, totalReturnPercent)
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Invested", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Text(formatRupee(totalInvested), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, amount: Double, percent: Double) {
    val isPositive = amount >= 0
    val color = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val sign = if (isPositive) "+" else ""

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(
            text = "$sign${formatRupee(amount)} ($sign${String.format(Locale.US, "%.2f", percent)}%)",
            color = color, 
            fontWeight = FontWeight.Medium, 
            fontSize = 14.sp
        )
    }
}

@Composable
fun ListHeaderRow() {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text("Data", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.8f))
        Text("Market Price\n(1D %)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End, maxLines = 2, modifier = Modifier.weight(1.3f))
        Text("Current\n(Invested)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End, maxLines = 2, modifier = Modifier.weight(1.2f))
        Text("Returns\n(%)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End, maxLines = 2, modifier = Modifier.weight(1f))
    }
}

@Composable
fun InvestmentListItem(item: InvestmentItem) {
    var expanded by remember { mutableStateOf(false) }

    val currentVal = item.quantity * item.currentPrice
    val investedVal = item.quantity * item.avgBuyPrice
    val totalRet = currentVal - investedVal
    val totalRetPct = if (investedVal > 0) (totalRet / investedVal) * 100 else 0.0
    val oneDPct = if (item.currentPrice - item.oneDayChangePrice > 0) (item.oneDayChangePrice / (item.currentPrice - item.oneDayChangePrice)) * 100 else 0.0

    val oneDayColor = if (item.oneDayChangePrice >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val oneDaySign = if (item.oneDayChangePrice >= 0) "+" else ""
    
    val totalRetColor = if (totalRet >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val totalRetSign = if (totalRet >= 0) "+" else ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        // Main Stock Row
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(0.8f)) {
                Text(item.assetName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val qtyDisplay = if (item.quantity % 1.0 == 0.0) "${item.quantity.toInt()} shares" else "${String.format(Locale.US, "%.3f", item.quantity)} units"
                Text(qtyDisplay, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Column(modifier = Modifier.weight(1.3f), horizontalAlignment = Alignment.End) {
                Text(formatRupee(item.currentPrice), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                Text("$oneDaySign${item.oneDayChangePrice} ($oneDaySign${String.format(Locale.US, "%.2f", oneDPct)}%)", fontSize = 11.sp, color = oneDayColor)
            }
            
            Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
                Text(formatRupee(currentVal), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                Text("(${formatRupee(investedVal)})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("$totalRetSign${formatRupee(totalRet)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                Text("($totalRetSign${String.format(Locale.US, "%.2f", totalRetPct)}%)", fontSize = 11.sp, color = totalRetColor)
            }
        }

        // Expandable Purchase History Sub-Table Container (Surface Card Background)
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    // Sub-Table Headers
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "Date",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.7f)
                        )
                        Text(
                            text = "Price (Qty)\n(Invested)",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            maxLines = 2,
                            modifier = Modifier.weight(1.35f)
                        )
                        Text(
                            text = "Brkg",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            modifier = Modifier.weight(0.65f)
                        )
                        Text(
                            text = "P/L (%)\n(Current)",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            maxLines = 2,
                            modifier = Modifier.weight(1.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Sub-Table History Rows
                    item.history.forEach { historyEntry ->
                        val (dateLine1, dateLine2) = formatHistoryDate(historyEntry.date)
                        val trancheQtyDisplay = if (historyEntry.quantity % 1.0 == 0.0) "${historyEntry.quantity.toInt()}" else String.format(Locale.US, "%.3f", historyEntry.quantity)
                        val trancheInvested = historyEntry.quantity * historyEntry.price
                        val trancheCurrent = historyEntry.quantity * item.currentPrice
                        val tranchePL = trancheCurrent - trancheInvested
                        val tranchePct = if (historyEntry.price > 0) ((item.currentPrice - historyEntry.price) / historyEntry.price) * 100 else 0.0

                        val plColor = if (tranchePL >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        val plSign = if (tranchePL >= 0) "+" else ""

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Date Column
                            Column(modifier = Modifier.weight(0.7f)) {
                                Text(
                                    text = dateLine1,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                if (dateLine2.isNotEmpty()) {
                                    Text(
                                        text = dateLine2,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        maxLines = 1
                                    )
                                }
                            }

                            // Price (Qty) & (Invested)
                            Column(modifier = Modifier.weight(1.35f), horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${formatRupee(historyEntry.price)} ($trancheQtyDisplay)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = "(${formatRupee(trancheInvested)})",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }

                            // Brkg Column
                            Column(modifier = Modifier.weight(0.65f), horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (historyEntry.brokerage > 0) formatRupee(historyEntry.brokerage) else "₹0",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }

                            // P/L (%) & (Current)
                            Column(modifier = Modifier.weight(1.6f), horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$plSign${formatRupee(tranchePL)} ($plSign${String.format(Locale.US, "%.2f", tranchePct)}%)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = plColor,
                                    maxLines = 1
                                )
                                Text(
                                    text = "(${formatRupee(trancheCurrent)})",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatHistoryDate(dateStr: String): Pair<String, String> {
    if (dateStr.isBlank()) return Pair("-", "")
    return try {
        val inFormat = if (dateStr.contains("/")) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                       else if (dateStr.contains("-")) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                       else SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val dateObj = inFormat.parse(dateStr)
        if (dateObj != null) {
            val line1 = SimpleDateFormat("dd MMM", Locale.getDefault()).format(dateObj)
            val line2 = SimpleDateFormat("yyyy", Locale.getDefault()).format(dateObj)
            Pair(line1, line2)
        } else {
            Pair(dateStr, "")
        }
    } catch (e: Exception) {
        Pair(dateStr, "")
    }
}

fun formatRupee(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.maximumFractionDigits = 2
    return format.format(amount)
}
