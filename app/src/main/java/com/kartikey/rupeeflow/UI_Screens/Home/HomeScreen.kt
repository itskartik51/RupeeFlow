package com.kartikey.rupeeflow.UI_Screens.Home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    onExpenseCardClick: () -> Unit
) {
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

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE1F5FE)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("SYSTEM DIAGNOSTICS (Navigation Mode):", fontWeight = FontWeight.Bold, color = Color(0xFF0277BD), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("1. Current Active Route: $dNavState", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text("2. Back Button Saved Exits: $dBackPresses times", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text("3. SuperBoss Architecture: Active & Stable", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        ExpenseSummaryCard(
            thisMonthTotal = thisMonthExpenses, 
            thisYearTotal = thisYearExpenses, 
            isLoading = isLoadingExpenses,
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

// -------------------------------------------------------------------------
// YAHAN SE NEECHE WOH MISSING DESIGNS HAIN JO DELETE HO GAYE THE
// -------------------------------------------------------------------------

@Composable
fun ExpenseSummaryCard(thisMonthTotal: Double, thisYearTotal: Double, isLoading: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F)), 
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Total Expenses", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("₹${thisMonthTotal.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("This Year", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    Text("₹${thisYearTotal.toInt()}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
                Box(modifier = Modifier.background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("View History", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GridCard(title: String, value: String, lineColor: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            if (lineColor != Color.Transparent) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(lineColor, RoundedCornerShape(50)))
            }
        }
    }
}

@Composable
fun SpendingTrackerCard() {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Monthly Budget", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("0% Used", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2E7D32))
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = { 0f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)), color = Color(0xFF2E7D32), trackColor = Color(0xFFE8F5E9))
        }
    }
}

@Composable
fun ReminderBanner() {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(Color(0xFFFFB300), CircleShape), contentAlignment = Alignment.Center) {
                Text("!", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("No pending bills", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFE65100))
                Text("You're all caught up for this month.", fontSize = 12.sp, color = Color(0xFFEF6C00))
            }
        }
    }
}
