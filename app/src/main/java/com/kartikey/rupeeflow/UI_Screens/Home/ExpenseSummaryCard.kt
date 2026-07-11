package com.kartikey.rupeeflow.UI_Screens.Home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
// NAYA IMPORT: Outlined icons ke liye
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExpenseSummaryCard(
    thisMonthTotal: Double, 
    thisYearTotal: Double, 
    isLoading: Boolean, 
    onRefresh: () -> Unit = {}, // NAYA: Refresh action ke liye pass kiya
    onClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var filterText by remember { mutableStateOf("Month") }

    val displayAmount = if (filterText == "Year") thisYearTotal else thisMonthTotal

    Card(
        modifier = Modifier.fillMaxWidth(), // Yahan se .clickable HATA diya
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

                // Top-Right: Dropdown Button 
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { expanded = true }
                            .background(Color(0xFFC62828).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp) // PADDING KAM KI (Button chota)
                    ) {
                        Text(
                            text = filterText, 
                            color = Color.Black, 
                            fontSize = 11.sp, // TEXT SIZE KAM KIYA
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = "Select Filter",
                            tint = Color.Black, 
                            modifier = Modifier.size(14.dp) // ICON CHOTA KIYA
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
            
            Spacer(modifier = Modifier.height(6.dp)) // GAP KAM KIYA (Amount ko upar sarkane ke liye)

            // --- BOTTOM ROW: Amount, Refresh & View History Button ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Bottom-Left: Amount aur Refresh/Budget Text
                Column {
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
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // NAYA: Refresh Icon aur Budget Limit
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh Data",
                            tint = Color.Black.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onRefresh() }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Budget Limit ₹0.00 (0.0%)",
                            color = Color.Black.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Bottom-Right: View History Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onClick() } // AB SIRF YAHI CLICKABLE HAI
                        .background(Color(0xFFC62828).copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp) // PADDING KAM KI
                ) {
                    Text(
                        text = "View History", 
                        color = Color.Black, 
                        fontSize = 11.sp, // TEXT SIZE KAM KIYA
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
