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
    // Ye dropdown ka state handle karega (Month ya Year)
    var expanded by remember { mutableStateOf(false) }
    var filterText by remember { mutableStateOf("Month") }

    // Dropdown selection ke hisaab se amount dikhayega
    val displayAmount = if (filterText == "Year") thisYearTotal else thisMonthTotal

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        // AIRY RED: Diagnosis card ki tarah ekdum light pastel red background
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), 
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp) // Flat and clean
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Total Expenses", 
                color = Color(0xFFC62828).copy(alpha = 0.8f), // Dark Red
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            if (isLoading) {
                CircularProgressIndicator(color = Color(0xFFC62828), modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = "₹${displayAmount.toInt()}", 
                    fontWeight = FontWeight.ExtraBold, 
                    fontSize = 36.sp, // Amount thoda bada kiya drawing ke hisaab se
                    color = Color(0xFFC62828) 
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Section (View History & Filter Dropdown)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f)) // Ye dono buttons ko Right side push karega

                // View History Button
                Box(
                    modifier = Modifier
                        .background(Color(0xFFC62828).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "View History", 
                        color = Color(0xFFC62828), 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Month / Year Dropdown Box
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
                            color = Color(0xFFC62828), 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select Filter",
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Dropdown Menu (Jo click karne par open hoga)
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
        }
    }
}
