package com.kartikey.rupeeflow.UI_Screens.Assets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentScreen(onBackClick: () -> Unit, username: String, investmentList: List<InvestmentItem>) { 
    // API loading logic hatakar directly passed list use ki hai for instant load
    
    val totalInvested = investmentList.sumOf { it.quantity * it.avgBuyPrice }
    val totalCurrent = investmentList.sumOf { it.quantity * it.currentPrice }
    val total1DChange = investmentList.sumOf { it.quantity * it.oneDayChangePrice }
    val totalReturn = totalCurrent - totalInvested
    val totalReturnPercent = if (totalInvested > 0) (totalReturn / totalInvested) * 100 else 0.0
    val total1DPercent = if (totalCurrent - total1DChange > 0) (total1DChange / (totalCurrent - total1DChange)) * 100 else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Investments", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8F9FA))
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp)
        ) {
            item {
                InvestmentSummaryCard(
                    itemCount = investmentList.size,
                    totalCurrent = totalCurrent,
                    total1DChange = total1DChange,
                    total1DPercent = total1DPercent,
                    totalReturn = totalReturn,
                    totalReturnPercent = totalReturnPercent,
                    totalInvested = totalInvested
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                ListHeaderRow()
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(investmentList) { item ->
                InvestmentListItem(item)
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@Composable
fun InvestmentSummaryCard(
    itemCount: Int, totalCurrent: Double, total1DChange: Double, total1DPercent: Double, 
    totalReturn: Double, totalReturnPercent: Double, totalInvested: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("INVESTMENT ($itemCount)", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Row {
                    IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Visibility, contentDescription = "Hide", tint = Color.DarkGray, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.BarChart, contentDescription = "Analytics", tint = Color.DarkGray, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.DarkGray, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Text(text = formatRupee(totalCurrent), fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, color = Color.Black)
            
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(16.dp))

            SummaryRow("1D returns", total1DChange, total1DPercent)
            Spacer(modifier = Modifier.height(12.dp))
            
            SummaryRow("Total returns", totalReturn, totalReturnPercent)
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Invested", color = Color.DarkGray, fontSize = 14.sp)
                Text(formatRupee(totalInvested), color = Color.Black, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, amount: Double, percent: Double) {
    val isPositive = amount >= 0
    val color = if (isPositive) Color(0xFF00A36C) else Color(0xFFD32F2F)
    val sign = if (isPositive) "+" else ""

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.DarkGray, fontSize = 14.sp)
        Text(
            text = "$sign${formatRupee(amount)} ($sign${String.format("%.2f", percent)}%)",
            color = color, fontWeight = FontWeight.Medium, fontSize = 14.sp
        )
    }
}

@Composable
fun ListHeaderRow() {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text("Data", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(0.8f))
        Text("Market Price\n(1D %)", fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.End, maxLines = 2, modifier = Modifier.weight(1.3f))
        Text("Current\n(Invested)", fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.End, maxLines = 2, modifier = Modifier.weight(1.2f))
        Text("Returns\n(%)", fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.End, maxLines = 2, modifier = Modifier.weight(1f))
    }
}

@Composable
fun InvestmentListItem(item: InvestmentItem) {
    val currentVal = item.quantity * item.currentPrice
    val investedVal = item.quantity * item.avgBuyPrice
    val totalRet = currentVal - investedVal
    val totalRetPct = if (investedVal > 0) (totalRet / investedVal) * 100 else 0.0
    val oneDPct = if (item.currentPrice - item.oneDayChangePrice > 0) (item.oneDayChangePrice / (item.currentPrice - item.oneDayChangePrice)) * 100 else 0.0

    val oneDayColor = if (item.oneDayChangePrice >= 0) Color(0xFF00A36C) else Color(0xFFD32F2F)
    val oneDaySign = if (item.oneDayChangePrice >= 0) "+" else ""
    
    val totalRetColor = if (totalRet >= 0) Color(0xFF00A36C) else Color(0xFFD32F2F)
    val totalRetSign = if (totalRet >= 0) "+" else ""

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(0.8f)) {
            Text(item.assetName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${item.quantity.toInt()} shares", fontSize = 11.sp, color = Color.Gray)
        }
        
        Column(modifier = Modifier.weight(1.3f), horizontalAlignment = Alignment.End) {
            Text(formatRupee(item.currentPrice), fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.Medium)
            Text("$oneDaySign${item.oneDayChangePrice} ($oneDaySign${String.format("%.2f", oneDPct)}%)", fontSize = 11.sp, color = oneDayColor)
        }
        
        Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
            Text(formatRupee(currentVal), fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.Medium)
            Text("(${formatRupee(investedVal)})", fontSize = 11.sp, color = Color.Gray)
        }
        
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text("$totalRetSign${formatRupee(totalRet)}", fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.Medium)
            Text("($totalRetSign${String.format("%.2f", totalRetPct)}%)", fontSize = 11.sp, color = totalRetColor)
        }
    }
}

fun formatRupee(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.maximumFractionDigits = 2
    return format.format(amount).replace("-₹", "-₹ ") 
}
