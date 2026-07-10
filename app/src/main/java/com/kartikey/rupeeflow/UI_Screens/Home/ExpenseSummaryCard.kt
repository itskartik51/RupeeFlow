package com.kartikey.rupeeflow.UI_Screens.Home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExpenseSummaryCard(thisMonthTotal: Double, thisYearTotal: Double, isLoading: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        // Ye soft light red shade hai jo diagnosis card ki tarah light aur airy lagega
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEF9A9A)), 
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp) // Elevation thoda kam kiya hai taki aur light lage
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Total Expenses", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("₹${thisMonthTotal.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("This Year", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                    Text("₹${thisYearTotal.toInt()}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
                Box(modifier = Modifier.background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("View History", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
