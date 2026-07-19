package com.kartikey.rupeeflow.UI_Screens.Assets.Finance

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.CreditCard
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
import com.kartikey.rupeeflow.UI_Screens.QuickUpdateCCDialog

data class CreditCardItem(
    val issuer: String,
    val cardNo: String,
    val type: String,
    val limit: Double,
    val outstanding: Double,
    val available: Double,
    val utilization: Double,
    val cibilStatus: String,
    val billingDay: Int,
    val dueDay: Int,
    val reminderDay: Int,
    val annualFee: Double,
    val joiningFee: Double,
    val lastUsed: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardsScreen(
    onBackClick: () -> Unit,
    username: String,
    ccList: List<CreditCardItem>,
    isLoading: Boolean,
    onRefreshClick: () -> Unit,
    onEditCCClick: (CreditCardItem) -> Unit
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
                title = { Text("Credit Cards", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                actions = { IconButton(onClick = onRefreshClick) { Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", modifier = Modifier.rotate(if (isLoading) angle else 0f)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8F9FA))
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        if (ccList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No Credit Cards Added Yet", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(ccList) { cc ->
                    CCDetailCard(cc = cc, username = username, onEditClick = onEditCCClick, onRefreshRequest = onRefreshClick)
                }
            }
        }
    }
}

@Composable
fun CCDetailCard(cc: CreditCardItem, username: String, onEditClick: (CreditCardItem) -> Unit, onRefreshRequest: () -> Unit) {
    var showQuickUpdate by remember { mutableStateOf(false) }
    val logoRes = Constants.BankLogoMap[cc.issuer]

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).background(Color(0xFFD32F2F).copy(alpha = 0.05f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (logoRes != null) {
                        Image(
                            painter = painterResource(id = logoRes), contentDescription = cc.issuer,
                            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(Icons.Outlined.CreditCard, contentDescription = "Card Fallback", tint = Color(0xFFD32F2F), modifier = Modifier.size(24.dp))
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = cc.issuer.uppercase(), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                    Text(text = "Card: ${cc.cardNo} | ${cc.type}", color = Color.Gray, fontSize = 12.sp, letterSpacing = 1.sp)
                }
                
                IconButton(onClick = { onEditClick(cc) }) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit Card", tint = Color.Gray, modifier = Modifier.size(22.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(text = "Total Outstanding", color = Color.Gray, fontSize = 12.sp)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = formatRupeeAmount(cc.outstanding), fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    val isSafe = cc.cibilStatus == "Safe"
                    val pillColor = if(isSafe) Color(0xFF388E3C) else Color(0xFFD32F2F)
                    Box(modifier = Modifier.background(pillColor.copy(alpha = 0.1f), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(text = "CIBIL: ${cc.cibilStatus}", color = pillColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
                
                IconButton(
                    onClick = { showQuickUpdate = true },
                    modifier = Modifier.size(32.dp).background(Color(0xFFE8F5E9), CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Update Outstanding", tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MetricItem(label = "Total Limit", value = formatRupeeAmount(cc.limit), valueColor = Color.DarkGray, alignment = Alignment.Start)
                    MetricItem(label = "Available", value = formatRupeeAmount(cc.available), valueColor = Color(0xFF1976D2), alignment = Alignment.CenterHorizontally)
                    MetricItem(label = "Utilization", value = "${cc.utilization}%", valueColor = Color.DarkGray, alignment = Alignment.End)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MetricItem(label = "Billing Day", value = "${cc.billingDay}", valueColor = Color.DarkGray, alignment = Alignment.Start)
                    MetricItem(label = "Due Day", value = "${cc.dueDay}", valueColor = Color(0xFFD32F2F), alignment = Alignment.CenterHorizontally)
                    MetricItem(label = "Annual Fee", value = formatRupeeAmount(cc.annualFee), valueColor = Color.DarkGray, alignment = Alignment.End)
                }
            }
        }
    }
    
    if (showQuickUpdate) {
        QuickUpdateCCDialog(
            cc = cc,
            username = username,
            onDismiss = { showQuickUpdate = false },
            onSuccess = { showQuickUpdate = false; onRefreshRequest() }
        )
    }
}
