package com.kartikey.rupeeflow.UI_Screens.Home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
// Outlined icons ke liye
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.R

@Composable
fun HomeDashboardDesign(
    username: String, paddingValues: PaddingValues, 
    thisMonthExpenses: Double, thisYearExpenses: Double, isLoadingExpenses: Boolean,
    dNavState: String, dBackPresses: Int, 
    onLogout: () -> Unit,
    onRefreshExpenses: () -> Unit = {}, // NAYA PARAMETER (Future connection ke liye)
    onExpenseCardClick: () -> Unit
) {
    var showDiagnostics by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Image(painter = painterResource(id = R.mipmap.ic_launcher), contentDescription = "App Logo", modifier = Modifier.size(44.dp).clip(CircleShape))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("RupeeFlow", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text("Hi, $username", color = Color.Gray, fontSize = 12.sp)
            }
            Text("INR (₹) / USD", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFE8F5E9)), contentAlignment = Alignment.Center) {
                Text(username.take(2).uppercase(), color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        SystemDiagnosisCard(
            testName = "Navigation Mode",
            isExpanded = showDiagnostics,
            onToggle = { showDiagnostics = !showDiagnostics }
        ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text("1. Current Active Route: $dNavState", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text("2. Back Button Saved Exits: $dBackPresses times", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text("3. SuperBoss Architecture: Active & Stable", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
            }
        }

        Spacer(modifier = Modifier.height(16.dp)) 
        
        ExpenseSummaryCard(
            thisMonthTotal = thisMonthExpenses, 
            thisYearTotal = thisYearExpenses, 
            isLoading = isLoadingExpenses,
            onRefresh = onRefreshExpenses, // Yahan connect kar diya
            onClick = onExpenseCardClick
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GridCard(title = "STOCKS", value = "₹0", lineColor = Color(0xFF2E7D32), modifier = Modifier.weight(1f)) 
            GridCard(title = "MUTUAL FUNDS", value = "₹0", lineColor = Color(0xFF039BE5), modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GridCard(title = "BANK / FD", value = "₹0", lineColor = Color(0xFFFFB300), modifier = Modifier.weight(1f))
            GridCard(title = "BUDGET LIMIT", value = "0% Used", lineColor = Color.Transparent, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(24.dp))
        SpendingTrackerCard()
        Spacer(modifier = Modifier.height(16.dp))
        ReminderBanner()
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Recent Transactions", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            TextButton(onClick = onLogout) { Text("Logout", color = Color(0xFFD32F2F)) }
        }
        Spacer(modifier = Modifier.height(60.dp)) 
    }
}

// Universal Component
@Composable
fun SystemDiagnosisCard(
    testName: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    detailsContent: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onToggle() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE1F5FE)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "System Diagnosis ($testName)", 
                    fontWeight = FontWeight.Bold, 
                    color = Color(0xFF0277BD), 
                    fontSize = 14.sp
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = "Toggle Diagnosis",
                    tint = Color(0xFF0277BD)
                )
            }
            
            AnimatedVisibility(visible = isExpanded) {
                detailsContent()
            }
        }
    }
}
