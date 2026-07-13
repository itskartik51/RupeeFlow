package com.kartikey.rupeeflow.UI_Screens.Home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.UI_Screens.Add.TransactionModel

@Composable
fun ExpenseHistoryScreen(
    paddingValues: PaddingValues,
    history: List<TransactionModel>, // Google Sheet Data
    onBackClick: () -> Unit
) {
    val totalSpent = history.sumOf { it.amount }
    
    // Grouping by Date (WhatsApp Style)
    val groupedHistory = history.groupBy { it.date.take(10) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(paddingValues)
    ) {
        // TOP HEADER
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF2E7D32),
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onBackClick() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Expense History",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Color(0xFF2E7D32)
            )
        }

        // TOTAL SUMMARY CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Total Spent (In this list)", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("₹ $totalSpent", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color.White)
            }
        }

        // TRANSACTION LIST
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No Transactions Found", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                groupedHistory.forEach { (date, dailyTransactions) ->
                    // DATE HEADER
                    item {
                        Text(
                            text = date,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }

                    // DAILY TRANSACTIONS
                    items(dailyTransactions) { transaction ->
                        TransactionItemCard(transaction)
                    }
                }
            }
        }
    }
}

// CLICKABLE SMART CARD
@Composable
fun TransactionItemCard(transaction: TransactionModel) {
    var expanded by remember { mutableStateOf(false) }

    val icon: ImageVector = when (transaction.category.trim()) {
        "Food" -> Icons.Outlined.Restaurant
        "Transport" -> Icons.Outlined.DirectionsCar
        "Shopping" -> Icons.Outlined.ShoppingBag
        "Bills" -> Icons.Outlined.Receipt
        else -> Icons.Outlined.Edit
    }

    // UPDATE: Yahan dynamic Icon set kiya gaya hai payment mode ke basis pe
    val modeIcon = when (transaction.mode.trim()) {
        "Cash" -> Icons.Outlined.Payments
        "UPI" -> Icons.Outlined.QrCodeScanner
        "NEFT", "Net Banking" -> Icons.Outlined.AccountBalance
        "Credit Card", "Debit Card" -> Icons.Outlined.CreditCard
        else -> Icons.Outlined.AccountBalanceWallet
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { expanded = !expanded }, 
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFE8F5E9), shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = transaction.category, tint = Color(0xFF2E7D32))
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = transaction.category, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                    Text(text = transaction.date, fontSize = 12.sp, color = Color.Gray)
                }

                Text(
                    text = "- ₹${transaction.amount}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFFD32F2F)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // UPDATE: Mode of Payment With Icon Integration
                    if (transaction.mode.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                            Icon(imageVector = modeIcon, contentDescription = "Mode", modifier = Modifier.size(16.dp), tint = Color.DarkGray)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Paid via: ${transaction.mode}", fontSize = 13.sp, color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (transaction.remark1.isNotBlank()) {
                        Text(text = "• Remark 1: ${transaction.remark1}", fontSize = 13.sp, color = Color.DarkGray)
                    }
                    if (transaction.remark2.isNotBlank()) {
                        Text(text = "• Remark 2: ${transaction.remark2}", fontSize = 13.sp, color = Color.DarkGray)
                    }
                    if (transaction.remark1.isBlank() && transaction.remark2.isBlank() && transaction.mode.isBlank()) {
                        Text(text = "No remarks or mode added.", fontSize = 13.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}
