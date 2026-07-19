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
import androidx.compose.material.icons.outlined.Notifications
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
import java.util.Locale

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
            
            // --- TOP ROW: IDENTITY & ICONS ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).background(Color(0xFF1976D2).copy(alpha = 0.05f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (logoRes != null) {
                        Image(
                            painter = painterResource(id = logoRes), contentDescription = cc.issuer,
                            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(Icons.Outlined.CreditCard, contentDescription = "Card Fallback", tint = Color(0xFF1976D2), modifier = Modifier.size(24.dp))
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = cc.issuer.uppercase(), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                    Text(text = "Card: ${cc.cardNo} | ${cc.type}", color = Color.Gray, fontSize = 12.sp, letterSpacing = 1.sp)
                }
                
                // Bell Icon (Future Reminders)
                IconButton(onClick = { /* TODO: Notification Settings */ }) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Reminders", tint = Color.Gray, modifier = Modifier.size(22.dp))
                }
                // Edit Icon
                IconButton(onClick = { onEditClick(cc) }) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit Card", tint = Color.Gray, modifier = Modifier.size(22.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // --- MIDDLE ROW: LIMIT & ADD BUTTON ---
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = formatRupeeAmount(cc.limit), fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = Color.Black)
                    Text(text = "Total Limit", color = Color.Gray, fontSize = 12.sp)
                }
                
                IconButton(
                    onClick = { showQuickUpdate = true },
                    modifier = Modifier.size(36.dp).background(Color(0xFFE8F5E9), CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Update Outstanding", tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // --- PROGRESS BAR & UTILIZATION ---
            val progressVal = (cc.utilization / 100f).toFloat().coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progressVal },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF1976D2), // Premium Blue
                trackColor = Color(0xFFEEEEEE),
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Decimal formatting to exactly 2 places
            Text(
                text = String.format(Locale.US, "%.2f%%", cc.utilization),
                fontSize = 11.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // --- OUTSTANDING & AVAILABLE ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(text = formatRupeeAmount(cc.outstanding), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                    Text(text = "Outstanding", color = Color.Gray, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = formatRupeeAmount(cc.available), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1976D2))
                    Text(text = "Available", color = Color.Gray, fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(16.dp))
            
            // --- BOTTOM METRICS ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem(label = "Billing Day", value = "${cc.billingDay}", valueColor = Color.DarkGray, alignment = Alignment.Start)
                MetricItem(label = "Due Day", value = "${cc.dueDay}", valueColor = Color.Black, alignment = Alignment.CenterHorizontally)
                MetricItem(label = "Annual Fee", value = formatRupeeAmount(cc.annualFee), valueColor = Color.DarkGray, alignment = Alignment.End)
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
