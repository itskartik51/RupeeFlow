package com.kartikey.rupeeflow.UI_Screens.Home

import androidx.compose.animation.*
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
import androidx.compose.runtime.*
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
    history: List<TransactionModel>, 
    isLoading: Boolean,              
    onRefreshClick: () -> Unit,      
    onBackClick: () -> Unit,
    onEditClick: (TransactionModel) -> Unit,   
    onDeleteClick: (TransactionModel) -> Unit  
) {
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

    fun parseDateForSort(dateStr: String): Long {
        val formatWithTime = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val formatOnlyDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return try {
            formatWithTime.parse(dateStr)?.time ?: 0L
        } catch (e: Exception) {
            try { formatOnlyDate.parse(dateStr)?.time ?: 0L } catch (e2: Exception) { 0L }
        }
    }

    val sortedHistory = history.sortedByDescending { parseDateForSort(it.date) }
    val groupedHistory = sortedHistory.groupBy { getMonthYear(it.date) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) 
            .padding(paddingValues)
    ) {
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
                    val formattedTotal = if (monthTotal % 1.0 == 0.0) String.format("%.0f", monthTotal) else monthTotal.toString()

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F5F5)) 
                                .padding(horizontal = 16.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = monthYear,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.DarkGray,
                                fontSize = 24.sp 
                            )
                            Text(
                                text = "₹ $formattedTotal", 
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black,
                                fontSize = 24.sp 
                            )
                        }
                    }

                    itemsIndexed(monthTransactions) { index, transaction ->
                        TransactionFlatItem(
                            transaction = transaction,
                            onEditClick = { onEditClick(transaction) },
                            onDeleteClick = { onDeleteClick(transaction) }
                        )

                        if (index < monthTransactions.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp), 
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

@Composable
fun TransactionFlatItem(
    transaction: TransactionModel,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val modeIcon = when (transaction.mode.trim()) {
        "Cash" -> Icons.Outlined.Payments
        "UPI" -> Icons.Outlined.QrCodeScanner
        "NEFT", "Net Banking" -> Icons.Outlined.AccountBalance
        "Credit Card", "Debit Card" -> Icons.Outlined.CreditCard
        else -> Icons.Outlined.AccountBalanceWallet
    }

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

    val r1 = transaction.remark1.trim()
    val r2 = transaction.remark2.trim()
    val remarksCombo = when {
        r1.isNotBlank() && r2.isNotBlank() -> "$r1 > $r2"
        r1.isNotBlank() -> r1
        r2.isNotBlank() -> r2
        else -> ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable { expanded = !expanded }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.category,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = displayDate,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.offset(y = (-2).dp) 
                )
            }

            Column(
                horizontalAlignment = Alignment.End, 
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
                    Text(
                        text = "₹${transaction.amount}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color.Black 
                    )
                }
                
                if (remarksCombo.isNotBlank()) {
                    Text(
                        text = remarksCombo,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.End,
                        maxLines = 2, 
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        AnimatedVisibility(visible = expanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 20.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit, 
                    contentDescription = "Edit", 
                    tint = Color(0xFF1976D2),
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onEditClick() }
                )
                Spacer(modifier = Modifier.width(20.dp))
                Icon(
                    imageVector = Icons.Outlined.Delete, 
                    contentDescription = "Delete", 
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onDeleteClick() }
                )
            }
        }
    }
}
