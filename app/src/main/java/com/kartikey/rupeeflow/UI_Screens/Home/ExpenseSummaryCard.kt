package com.kartikey.rupeeflow.UI_Screens.Home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
    onRefresh: () -> Unit = {}, 
    onClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var filterText by remember { mutableStateOf("Month") }

    val displayAmount = if (filterText == "Year") thisYearTotal else thisMonthTotal

    Card(
        modifier = Modifier.fillMaxWidth(), 
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2)), // Pink/Red Theme
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp) 
    ) {
        // PADDING KAM KI (20dp -> 16dp) taaki elements right/edges ke paas shift ho jayein
        Column(modifier = Modifier.padding(16.dp)) {
            
            // --- TOP ROW: Title & Month/Year Dropdown ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically // Dono ki height same kar di
            ) {
                // Top-Left: Total Expenses (Ab Black color mein)
                Text(
                    text = "Total Expenses", 
                    color = Color.Black.copy(alpha = 0.8f), 
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                // Top-Right: Dropdown Button (Minimalistic Box)
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp)) // Corners thode sharp kiye
                            .clickable { expanded = true }
                            .background(Color(0xFFC62828).copy(alpha = 0.08f)) // Background aur light kiya
                            .padding(horizontal = 8.dp, vertical = 4.dp) // Box ko chota/slim kiya
                    ) {
                        Text(
                            text = filterText, 
                            color = Color.Black, 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = "Select Filter",
                            tint = Color.Black, 
                            modifier = Modifier.size(14.dp)
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
            
            // AMOUNT KO UPAR SHIFT KIYA
            Spacer(modifier = Modifier.height(10.dp)) 

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

            Spacer(modifier = Modifier.height(16.dp)) // Bottom row ko niche dhakelne ke liye space

            // --- BOTTOM ROW: Refresh/Budget & View History Button ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically // Sabko same height level par la diya
            ) {
                // Bottom-Left: Refresh Icon aur Budget Text
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Refresh Data",
                        tint = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onRefresh() }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Budget Limit ₹0.00 (0.0%)",
                        color = Color.Black.copy(alpha = 0.7f),
                        fontSize = 11.sp, // Font size badi ki (10 -> 11)
                        fontWeight = FontWeight.Medium
                    )
                }

                // Bottom-Right: View History Button (Minimalistic Box)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp)) // Corners sharp
                        .clickable { onClick() } 
                        .background(Color(0xFFC62828).copy(alpha = 0.08f)) // Background light
                        .padding(horizontal = 10.dp, vertical = 4.dp) // Box ko slim kiya
                ) {
                    Text(
                        text = "View History", 
                        color = Color.Black, 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
