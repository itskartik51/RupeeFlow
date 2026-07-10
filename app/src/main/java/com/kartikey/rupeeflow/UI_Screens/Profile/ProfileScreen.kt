package com.kartikey.rupeeflow.UI_Screens.Profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AnalyticsScreen(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Analytics & Charts", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1565C0))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Spending Graphs Coming Soon...", color = Color.Gray, fontSize = 14.sp)
        }
    }
}
