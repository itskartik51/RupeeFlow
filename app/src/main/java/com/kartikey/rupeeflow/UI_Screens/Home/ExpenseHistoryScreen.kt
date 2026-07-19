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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.UI_Screens.Add.TransactionModel
import java.text.SimpleDateFormat
import java.util.Locale

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

    // STRICT DATE-TIME PARSER FOR NEW-TO-OLD SORTING
    fun parseDateForSort(dateStr: String): Long {
        val formatWithTime = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val formatOnlyDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return try {
            formatWithTime.parse(dateStr)?.time ?: 0L
        } catch (e: Exception) {
            try { formatOnlyDate.parse(dateStr)?.time ?: 0L } catch (e2: Exception) { 0L }
        }
    }

    // Forcefully sort the list: NEW to OLD strictly by exact Date and Time
    val sortedHistory = history.sortedByDescending { parseDateForSort(it.date) }
    
    // Grouping by Month-Year after sorting
    val groupedHistory = sortedHistory.groupBy { getMonthYear(it.date) }

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
                    // Format to remove decimals if not needed
                    val formattedTotal = if (monthTotal % 1.0 == 0.0) String.format("%.0f", monthTotal) else monthTotal.toString()

                    // 1. HEADER (24.sp Size)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F5F5)) // Grey Background Patti
                                .padding(horizontal = 16.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = monthYear,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.DarkGray,
                                fontSize = 24.sp // Corrected as instructed
                            )
                            Text(
                                text = "₹ $formattedTotal", 
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black,
                                fontSize = 24.sp // Corrected as instructed
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
            .padding(horizontal = 20.dp, vertical = 10.dp), // Gap reduced from 14.dp to 10.dp
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LEFT SIDE: Category & Date
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.category,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Tucked right under category
            Text(
                text = displayDate,
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.offset(y = (-2).dp) 
            )
        }

        // RIGHT SIDE: Amount + Icon (Top) & Remarks (Bottom)
        Column(
            horizontalAlignment = Alignment.End, // Ensures touched to the right edge
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = modeIcon,
                    contentDescription = transaction.mode,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                // Format to keep decimal cleanly
                Text(
                    text = "₹${transaction.amount}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = Color.Black 
                )
            }
            
            // Remarks shifted below amount and right-aligned
            if (remarksCombo.isNotBlank()) {
                Text(
                    text = remarksCombo,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.End,
                    maxLines = 2, // Allow wrapping if it's very long so it shows completely
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
