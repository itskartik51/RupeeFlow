package com.kartikey.rupeeflow.UI_Screens.Home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GridCard(
    title: String, 
    value: AnnotatedString, 
    lineColor: Color, 
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {} 
) {
    Card(
        modifier = modifier.clickable { onClick() }, 
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            
            val isAddDetails = value.text.equals("Add Details", ignoreCase = true)
            Text(
                text = value, 
                fontSize = if (isAddDetails) 14.sp else 18.sp, 
                fontWeight = if (isAddDetails) FontWeight.Bold else FontWeight.ExtraBold,
                color = if (isAddDetails) Color(0xFF1976D2) else Color.Black
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            if (lineColor != Color.Transparent) {
                Box(modifier = Modifier.fillMaxWidth(0.6f).height(3.dp).background(lineColor, RoundedCornerShape(50)))
            }
        }
    }
}
