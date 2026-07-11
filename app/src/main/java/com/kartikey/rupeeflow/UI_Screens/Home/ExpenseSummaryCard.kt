package com.kartikey.rupeeflow.UI_Screens.Home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExpenseSummaryCard(
    thisMonthTotal: Double, 
    thisYearTotal: Double, 
    isLoading: Boolean, 
    onClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var filterText by remember { mutableStateOf("Month") }

    val displayAmount = if (filterText == "Year") thisYearTotal else thisMonthTotal

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2)), 
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp) 
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            
            // --- TOP ROW: Title & Month/Year Dropdown ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Top-Left: Total Expenses (Red Text)
                Text(
                    text = "Total Expenses", 
                    color = Color(0xFFC62828).copy(alpha = 0.8f), 
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Top-Right: Dropdown Button (Black Text & Icon)
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { expanded = true }
                            .background(Color(0xFFC62828).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filterText, 
                            color = Color.Black, 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select Filter",
                            tint = Color.Black, 
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Month", color = Color.Black) },
                            onClick = { filterText = "Month"; expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Year", color = Color.Black) },
                            onClick = { filterText = "Year"; expanded = false }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // --- BOTTOM ROW: Amount & View History Button ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Bottom-Left: Amount (Black Text)
                if (isLoading) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "₹${displayAmount.toInt()}", 
                        fontWeight = FontWeight.ExtraBold, 
                        fontSize = 36.sp, 
                        color = Color.Black
                    )
                }

                // Bottom-Right: View History Button (Black Text)
                Box(
                    modifier = Modifier
                        .background(Color(0xFFC62828).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "View History", 
                        color = Color.Black, 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
