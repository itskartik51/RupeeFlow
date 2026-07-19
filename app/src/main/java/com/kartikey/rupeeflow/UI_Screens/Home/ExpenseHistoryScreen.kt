package com.kartikey.rupeeflow.UI_Screens.Home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.UI_Screens.Add.TransactionModel

@Composable
fun ExpenseHistoryScreen(
    paddingValues: PaddingValues,
    history: List<TransactionModel>, // Google Sheet Data
    isLoading: Boolean,              // Loading State for Spinner
    onRefreshClick: () -> Unit,      // Trigger Refresh API
    onBackClick: () -> Unit
) {
    // Smooth Spinning Animation logic for Refresh Icon
    val infiniteTransition = rememberInfiniteTransition(label = "refreshSpinner")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    // Helper: Extract Month and Year (e.g., "July 2026")
    fun getMonthYear(dateStr: String): String {
        try {
            val datePart = dateStr.split(" ")[0]
            val parts = datePart.split("/")
            if (parts.size >= 3) {
                val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
                val monthIdx = parts[1].toIntOrNull()?.minus(1) ?: return datePart
                if (monthIdx in 0..11) {
                    return "${months[monthIdx]} ${parts[2]}"
                }
            }
        } catch (e: Exception) {}
        return "Unknown"
    }

    // Grouping by Month-Year
    val groupedHistory = history.groupBy { getMonthYear(it.date) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // Pure white background
            .padding(paddingValues)
    ) {
        // TOP HEADER
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onBackClick() }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Expense History",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Color.Black
                )
            }
            
            // Refresh Button with Continuous Spin
            IconButton(onClick = onRefreshClick, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Refresh",
                    tint = Color.Black,
                    modifier = Modifier
                        .size(26.dp)
                        .rotate(if (isLoading) angle else 0f)
                )
            }
        }

        // TRANSACTION LIST
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No Transactions Found", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                groupedHistory.forEach { (monthYear, monthTransactions) ->
                    val monthTotal = monthTransactions.sumOf { it.amount }
                    val formattedTotal = String.format("%.0f", monthTotal)

                    // 1. GIANT 28sp MONTH-YEAR HEADER
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F5F5)) // Grey Background Patti
                                .padding(horizontal = 16.dp, vertical = 24.dp), // Thodi extra padding luxury feel ke liye
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = monthYear,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.DarkGray,
                                fontSize = 28.sp
                            )
                            Text(
                                text = "₹ $formattedTotal", 
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black,
                                fontSize = 28.sp
                            )
                        }
                    }

                    // 2. FLAT EXPENSE DETAILS FOR THE MONTH
                    itemsIndexed(monthTransactions) { index, transaction ->
                        TransactionFlatItem(transaction)

                        // 3. Grey Separator Line
                        if (index < monthTransactions.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp), // Kone se jagah chhodi hui
                                color = Color(0xFFEEEEEE),
                                thickness = 1.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

// FLAT, ICON-LESS, PREMIUM LIST ITEM
@Composable
fun TransactionFlatItem(transaction: TransactionModel) {
    // Dynamic Mode Icon
    val modeIcon = when (transaction.mode.trim()) {
        "Cash" -> Icons.Outlined.Payments
        "UPI" -> Icons.Outlined.QrCodeScanner
        "NEFT", "Net Banking" -> Icons.Outlined.AccountBalance
        "Credit Card", "Debit Card" -> Icons.Outlined.CreditCard
        else -> Icons.Outlined.AccountBalanceWallet
    }

    // Extract exactly "19 July"
    fun getDayMonth(dateStr: String): String {
        try {
            val datePart = dateStr.split(" ")[0]
            val parts = datePart.split("/")
            if (parts.size >= 2) {
                val months = listOf("July", "August", "September", "October", "November", "December", "January", "February", "March", "April", "May", "June")
                // Adjusting correctly based on 1-12 indexing
                val standardMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
                val monthIdx = parts[1].toIntOrNull()?.minus(1)
                if (monthIdx != null && monthIdx in 0..11) {
                    return "${parts[0]} ${standardMonths[monthIdx]}"
                }
            }
        } catch (e: Exception) {}
        return ""
    }

    val displayDate = getDayMonth(transaction.date)

    // Build the "Remark 1 > Remark 2" string
    val r1 = transaction.remark1.trim()
    val r2 = transaction.remark2.trim()
    val remarksCombo = when {
        r1.isNotBlank() && r2.isNotBlank() -> "$r1 > $r2"
        r1.isNotBlank() -> r1
        r2.isNotBlank() -> r2
        else -> ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 14.dp), // Clean spacing
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LEFT SIDE: Category & Date
        Column(modifier = Modifier.weight(1.3f)) {
            Text(
                text = transaction.category,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp, // Slightly bigger
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // No gap shadow date
            Text(
                text = displayDate,
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.offset(y = (-2).dp) 
            )
        }

        // MIDDLE: Remarks (Aligned beautifully at the bottom matching the Date)
        Box(
            modifier = Modifier.weight(1f).align(Alignment.Bottom),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (remarksCombo.isNotBlank()) {
                Text(
                    text = remarksCombo,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 2.dp) // Perfect horizontal alignment with Date
                )
            }
        }

        // RIGHT SIDE: Mode Icon + Amount (No Minus Sign)
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = modeIcon,
                contentDescription = transaction.mode,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp) // Small subtle icon
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "₹${transaction.amount}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = Color.Black 
            )
        }
    }
}
