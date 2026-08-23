package com.kartikey.rupeeflow.UI_Screens.Assets.Finance

import java.text.NumberFormat
import java.util.Locale

// ==========================================
// SMART RUPEE FORMATTER (0.00 vs 500 vs 500.43)
// ==========================================
fun formatRupeeAmount(amount: Double): String {
    if (amount == 0.0) return "₹0.00"
    
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    
    // Point ke baad zero hone par decimal nahi dikhayega (e.g. 500.0 -> ₹500)
    if (amount % 1.0 == 0.0) {
        format.maximumFractionDigits = 0
        format.minimumFractionDigits = 0
    } else {
        format.maximumFractionDigits = 2
        format.minimumFractionDigits = 2
    }
    
    return format.format(amount)
}
