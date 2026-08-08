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
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import java.util.Locale

data class CreditCardItem(
    val firebaseKey: String = "", // Added to fix the compiler error
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
                title = { Text("Credit Cards", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = { 
                    IconButton(onClick = onBackClick, modifier = Modifier.bounceClick()) { 
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface) 
                    } 
                },
                actions = { 
                    IconButton(onClick = onRefreshClick, modifier = Modifier.bounceClick()) { 
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.rotate(if (isLoading) angle else 0f)) 
                    } 
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (ccList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No Credit Cards Added Yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).background(Color(0xFF1976D2).copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
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
                    Text(text = cc.issuer.uppercase(), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "Card: ${cc.cardNo} | ${cc.type}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, letterSpacing = 1.sp)
                }
                
                IconButton(onClick = { /* TODO: Notification Settings */ }, modifier = Modifier.bounceClick()) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Reminders", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = { onEditClick(cc) }, modifier = Modifier.bounceClick()) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit Card", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = formatRupeeAmount(cc.limit), fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "Total Limit", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                
                IconButton(
                    onClick = { showQuickUpdate = true },
                    modifier = Modifier.size(36.dp).bounceClick().background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Update Outstanding", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val progressVal = (cc.utilization / 100f).toFloat().coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progressVal },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF1976D2),
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = String.format(Locale.US, "%.2f%%", cc.utilization),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(text = formatRupeeAmount(cc.outstanding), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "Outstanding", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = formatRupeeAmount(cc.available), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1976D2))
                    Text(text = "Available", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem(label = "Billing Day", value = "${cc.billingDay}", valueColor = MaterialTheme.colorScheme.onSurfaceVariant, alignment = Alignment.Start)
                MetricItem(label = "Due Day", value = "${cc.dueDay}", valueColor = MaterialTheme.colorScheme.onSurface, alignment = Alignment.CenterHorizontally)
                MetricItem(label = "Annual Fee", value = formatRupeeAmount(cc.annualFee), valueColor = MaterialTheme.colorScheme.onSurfaceVariant, alignment = Alignment.End)
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
