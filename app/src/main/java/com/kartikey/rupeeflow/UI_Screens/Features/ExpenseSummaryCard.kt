package com.kartikey.rupeeflow.UI_Screens.Features

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
    totalExpense: String = "₹0", // Abhi ke liye default ₹0 hai
    modifier: Modifier = Modifier
) {
    // Dropdown open/close aur selected filter yaad rakhne ke liye
    var expanded by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("This Month") }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), // Light Redish Background
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            
            // Upar wali line: "EXPENSES" aur "This Month v"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EXPENSES", 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = Color(0xFFD32F2F) // Dark Red Text
                )
                
                // Dropdown Menu Box
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { expanded = true }
                    ) {
                        Text(
                            text = selectedFilter, 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = Color(0xFFD32F2F)
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown, // Niche wala arrow (v)
                            contentDescription = "Select Duration",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("This Month", fontWeight = FontWeight.Medium) },
                            onClick = { 
                                selectedFilter = "This Month"
                                expanded = false 
                                // Aage chalkar yahan logic lagayenge data change karne ka
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("This Year", fontWeight = FontWeight.Medium) },
                            onClick = { 
                                selectedFilter = "This Year"
                                expanded = false 
                                // Yahan bhi logic aayega
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp)) // Niche ka extra text hata kar direct Amount
            
            // Main Amount
            Text(
                text = totalExpense, 
                fontSize = 32.sp, 
                fontWeight = FontWeight.ExtraBold, 
                color = Color.Black
            )
        }
    }
}
