package com.kartikey.rupeeflow.UI_Screens.Home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.UI_Screens.Add.TransactionModel

@Composable
fun ExpenseHistoryScreen(
    paddingValues: PaddingValues,
    history: List<TransactionModel>, // Google Sheet Data
    onBackClick: () -> Unit
) {
    // Helper function to extract Month and Year safely
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
            .background(Color.White) // Pure white background for the whole page
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

                    // GREY MONTH-YEAR HEADER (Highlight Strip)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F5F5)) // Grey Background
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = monthYear,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "₹ $monthTotal", // Direct total amount
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // FLAT EXPENSE DETAILS FOR THE MONTH
                    itemsIndexed(monthTransactions) { index, transaction ->
                        TransactionFlatItem(transaction)

                        // Grey Separator Line (Does not touch ends)
                        if (index < monthTransactions.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp), // Thodi jagah chhod kar line
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

// FLAT & CLEAN LIST ITEM
@Composable
fun TransactionFlatItem(transaction: TransactionModel) {
    val icon: ImageVector = when (transaction.category.trim()) {
        "Food" -> Icons.Outlined.Restaurant
        "Transport" -> Icons.Outlined.DirectionsCar
        "Shopping" -> Icons.Outlined.ShoppingBag
        "Bills" -> Icons.Outlined.Receipt
        else -> Icons.Outlined.Edit
    }

    // Extract Date (Day) and Time from the date string
    val datePart = transaction.date.split(" ")[0]
    val dayStr = if (datePart.contains("/")) datePart.split("/")[0] else ""
    val timeStr = if (transaction.date.contains(" ")) transaction.date.substringAfter(" ") else ""

    // Formatting Subtitle (Ex: 19 • 10:04 AM • UPI • Petrol)
    val subtitleParts = mutableListOf<String>()
    if (dayStr.isNotBlank()) subtitleParts.add(dayStr)
    if (timeStr.isNotBlank()) subtitleParts.add(timeStr)
    if (transaction.mode.isNotBlank()) subtitleParts.add(transaction.mode)
    if (transaction.remark1.isNotBlank()) subtitleParts.add(transaction.remark1)

    val subtitle = subtitleParts.joinToString(" • ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White) // Pure white background for list items
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color(0xFFF5F5F5), shape = RoundedCornerShape(10.dp)), // Light grey icon box
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = transaction.category, tint = Color.Black)
        }
        
        Spacer(modifier = Modifier.width(14.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.category,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.Black
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = "- ₹${transaction.amount}",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.Black 
        )
    }
}
