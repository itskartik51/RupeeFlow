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
import com.kartikey.rupeeflow.UI_Screens.Assets.BankAccountItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankAccountsScreen(
    onBackClick: () -> Unit,
    bankList: List<BankAccountItem>,
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
                title = { Text("Linked Banks", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = onRefreshClick) {
                        Icon(
                            Icons.Outlined.Refresh, 
                            contentDescription = "Refresh", 
                            modifier = Modifier.rotate(if (isLoading) angle else 0f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8F9FA))
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        if (bankList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No Bank Accounts Added Yet", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(bankList) { bank ->
                    BankDetailCard(bank)
                }
            }
        }
    }
}

@Composable
fun BankDetailCard(bank: BankAccountItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(Color(0xFF1976D2).copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.AccountBalance, contentDescription = "Bank", tint = Color(0xFF1976D2))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = bank.bankName.uppercase(), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                    Text(text = bank.accountNo, color = Color.Gray, fontSize = 12.sp, letterSpacing = 1.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Balance
            Text(text = "Available Balance", color = Color.Gray, fontSize = 12.sp)
            Text(text = formatRupeeAmount(bank.currentBalance), fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = Color.Black)
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(16.dp))
            
            // Premium Metrics Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(text = "Interest Rate", color = Color.Gray, fontSize = 11.sp)
                    Text(text = "${bank.interestRate}% Yr", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.DarkGray)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Quarterly Est.", color = Color.Gray, fontSize = 11.sp)
                    Text(text = "${String.format("%.2f", bank.qtrInterestPct)}%", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFF57C00))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "1-Day Earn", color = Color.Gray, fontSize = 11.sp)
                    Text(text = "+${formatRupeeAmount(bank.oneDayInt)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF388E3C))
                }
            }
        }
    }
}
