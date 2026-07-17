package com.kartikey.rupeeflow.UI_Screens.Assets.Finance

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class FDItem(
    val bankName: String,
    val accountNo: String,
    val createDate: String,
    val maturityDate: String,
    val daysToMaturity: Int,
    val investedAmt: Double,
    val interestRate: Double,
    val maturityValue: Double,
    val accruedValue: Double,
    val accruedInt: Double,
    val oneDayInt: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedDepositsScreen(
    onBackClick: () -> Unit,
    username: String,
    fdList: List<FDItem>,
    isLoading: Boolean,
    onRefreshClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "refresh")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "spin"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fixed Deposits", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                actions = { IconButton(onClick = onRefreshClick) { Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", modifier = Modifier.rotate(if (isLoading) angle else 0f)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8F9FA))
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        if (fdList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No Fixed Deposits Available", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(fdList) { fd ->
                    FDetailCard(fd = fd)
                }
            }
        }
    }
}

@Composable
fun FDetailCard(fd: FDItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row (Logo & Details)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                
                // 100% Offline Premium Bank Icon Placeholder
                Box(
                    modifier = Modifier.size(44.dp).background(Color(0xFFF57C00).copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalance, 
                        contentDescription = "Bank", 
                        tint = Color(0xFFF57C00), 
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = fd.bankName.uppercase(), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                    Text(text = "A/C: ${fd.accountNo}", color = Color.Gray, fontSize = 12.sp, letterSpacing = 1.sp)
                }
                
                // Status Pill (Matured or Days Left)
                val isMatured = fd.daysToMaturity <= 0
                val pillColor = if (isMatured) Color(0xFF388E3C) else Color(0xFF1976D2)
                Box(
                    modifier = Modifier.background(pillColor.copy(alpha = 0.1f), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isMatured) "Matured" else "${fd.daysToMaturity} Days Left", 
                        color = pillColor, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 11.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Financial Status Block
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text(text = "Current Value", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = formatRupeeAmount(fd.accruedValue), fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = Color.Black)
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Interest Rate", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "${fd.interestRate}% Yr", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFF57C00))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(16.dp))
            
            // Detailed Metrics Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricCol(label = "Invested", value = formatRupeeAmount(fd.investedAmt), color = Color.DarkGray)
                MetricCol(label = "Maturity Amt", value = formatRupeeAmount(fd.maturityValue), color = Color(0xFF1976D2), align = Alignment.CenterHorizontally)
                MetricCol(label = "Total Int. Earned", value = "+${formatRupeeAmount(fd.accruedInt)}", color = Color(0xFF388E3C), align = Alignment.End)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricCol(label = "Start Date", value = fd.createDate, color = Color.DarkGray)
                MetricCol(label = "End Date", value = fd.maturityDate, color = Color.DarkGray, align = Alignment.CenterHorizontally)
                MetricCol(label = "1-Day Earn", value = if(fd.daysToMaturity <= 0) "₹0.00" else "+${formatRupeeAmount(fd.oneDayInt)}", color = if(fd.daysToMaturity <= 0) Color.Gray else Color(0xFF388E3C), align = Alignment.End)
            }
        }
    }
}

@Composable
fun MetricCol(label: String, value: String, color: Color, align: Alignment.Horizontal = Alignment.Start) {
    Column(horizontalAlignment = align) {
        Text(text = label, color = Color.Gray, fontSize = 11.sp)
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color)
    }
}
