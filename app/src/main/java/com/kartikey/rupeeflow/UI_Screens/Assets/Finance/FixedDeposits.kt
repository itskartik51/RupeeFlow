package com.kartikey.rupeeflow.UI_Screens.Assets.Finance

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.Cloud_Database.Constants
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import java.text.SimpleDateFormat
import java.util.Locale

data class FDItem(
    val firebaseKey: String = "",
    val bankName: String = "",
    val accountNo: String = "",
    val createDate: String = "",
    val maturityDate: String = "",
    val investedAmt: Double = 0.0,
    val interestRate: Double = 0.0,
    val daysToMaturity: Int = 0,
    val maturityValue: Double = 0.0,
    val accruedValue: Double = 0.0,
    val accruedInt: Double = 0.0,
    val oneDayInt: Double = 0.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedDepositsScreen(
    onBackClick: () -> Unit,
    username: String,
    fdList: List<FDItem>,
    isLoading: Boolean,
    onRefreshClick: () -> Unit,
    onEditFDClick: (FDItem) -> Unit
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
                title = { Text("Fixed Deposits", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = { 
                    IconButton(onClick = onBackClick, modifier = Modifier.bounceClick(scaleDown = 0.94f)) { 
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface) 
                    } 
                },
                actions = { 
                    IconButton(onClick = onRefreshClick, modifier = Modifier.bounceClick(scaleDown = 0.94f)) { 
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.rotate(if (isLoading) angle else 0f)) 
                    } 
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (fdList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No Fixed Deposits Available", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(fdList) { fd ->
                    FDetailCard(fd = fd, onEditClick = onEditFDClick)
                }
            }
        }
    }
}

@Composable
fun FDetailCard(fd: FDItem, onEditClick: (FDItem) -> Unit) {
    val logoRes = Constants.BankLogoMap[fd.bankName]

    // ========================================================
    // DYNAMIC FRONTEND COMPOUND INTEREST CALCULATION
    // ========================================================
    val todayMillis = remember { System.currentTimeMillis() }
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    val createMillis = remember(fd.createDate) {
        try { sdf.parse(fd.createDate)?.time ?: 0L } catch (e: Exception) { 0L }
    }
    val maturMillis = remember(fd.maturityDate) {
        try { sdf.parse(fd.maturityDate)?.time ?: 0L } catch (e: Exception) { 0L }
    }

    val totalDays = if (createMillis > 0 && maturMillis > createMillis) maxOf(0L, (maturMillis - createMillis) / (1000 * 60 * 60 * 24)) else 0L
    val daysToMat = if (maturMillis > 0) maxOf(0L, (maturMillis - todayMillis) / (1000 * 60 * 60 * 24)).toInt() else fd.daysToMaturity
    val daysPassed = if (createMillis > 0) maxOf(0L, (minOf(todayMillis, maturMillis) - createMillis) / (1000 * 60 * 60 * 24)) else 0L

    val matVal = if (totalDays > 0) fd.investedAmt * Math.pow(1 + (fd.interestRate / 100.0), totalDays / 365.0) else fd.investedAmt
    val accVal = if (daysPassed > 0) fd.investedAmt * Math.pow(1 + (fd.interestRate / 100.0), daysPassed / 365.0) else fd.investedAmt
    val accInt = accVal - fd.investedAmt
    val oneDayInt = if (todayMillis >= maturMillis || todayMillis < createMillis) 0.0 else (accVal * (fd.interestRate / 100.0)) / 365.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                
                Box(
                    modifier = Modifier.size(44.dp).background(Color(0xFFF57C00).copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (logoRes != null) {
                        Image(
                            painter = painterResource(id = logoRes), contentDescription = fd.bankName,
                            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(Icons.Outlined.AccountBalance, contentDescription = "Bank Fallback", tint = Color(0xFFF57C00), modifier = Modifier.size(24.dp))
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = fd.bankName.uppercase(), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "A/C: ${fd.accountNo}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, letterSpacing = 1.sp)
                }
                
                val isMatured = daysToMat <= 0
                val pillColor = if (isMatured) Color(0xFF388E3C) else Color(0xFF1976D2)
                Box(modifier = Modifier.background(pillColor.copy(alpha = 0.15f), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(text = if (isMatured) "Matured" else "$daysToMat Days Left", color = pillColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { onEditClick(fd) }, modifier = Modifier.bounceClick(scaleDown = 0.94f)) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit FD", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text(text = "Current Value", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = formatRupeeAmount(accVal), fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Interest Rate", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "${fd.interestRate}% Yr", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFF57C00))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricCol(label = "Invested", value = formatRupeeAmount(fd.investedAmt), color = MaterialTheme.colorScheme.onSurfaceVariant)
                MetricCol(label = "Maturity Amt", value = formatRupeeAmount(matVal), color = Color(0xFF1976D2), align = Alignment.CenterHorizontally)
                MetricCol(label = "Total Int. Earned", value = "+${formatRupeeAmount(accInt)}", color = Color(0xFF388E3C), align = Alignment.End)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricCol(label = "Start Date", value = fd.createDate, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MetricCol(label = "End Date", value = fd.maturityDate, color = MaterialTheme.colorScheme.onSurfaceVariant, align = Alignment.CenterHorizontally)
                MetricCol(label = "1-Day Earn", value = if(daysToMat <= 0) "₹0.00" else "+${formatRupeeAmount(oneDayInt)}", color = if(daysToMat <= 0) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF388E3C), align = Alignment.End)
            }
        }
    }
}

@Composable
fun MetricCol(label: String, value: String, color: Color, align: Alignment.Horizontal = Alignment.Start) {
    Column(horizontalAlignment = align) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color)
    }
}
