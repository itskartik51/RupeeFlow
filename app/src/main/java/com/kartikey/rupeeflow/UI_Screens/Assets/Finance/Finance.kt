package com.kartikey.rupeeflow.UI_Screens.Assets.Finance

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

// ==========================================
// SMART RUPEE FORMATTER (0.00 vs 500 vs 500.43)
// ==========================================
fun formatRupeeAmount(amount: Double): String {
    if (amount == 0.0) return "₹0.00"
    
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    
    if (amount % 1.0 == 0.0) {
        format.maximumFractionDigits = 0
        format.minimumFractionDigits = 0
    } else {
        format.maximumFractionDigits = 2
        format.minimumFractionDigits = 2
    }
    
    return format.format(amount)
}

@Composable
fun MetricItem(label: String, value: String, valueColor: Color, alignment: Alignment.Horizontal) {
    Column(horizontalAlignment = alignment) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = valueColor)
    }
}
