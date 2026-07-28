package com.kartikey.rupeeflow.UI_Screens.Home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ExpenseSummaryCard(
    thisMonthExpenses: Double,
    thisYearExpenses: Double,
    budgetLimit: Double,
    isLoadingExpenses: Boolean,
    onRefreshExpenses: () -> Unit,
    onExpenseCardClick: () -> Unit
) {
    var selectedPeriod by remember { mutableStateOf("Month") }
    var expanded by remember { mutableStateOf(false) }

    val currentAmount = if (selectedPeriod == "Month") thisMonthExpenses else thisYearExpenses
    val currentBudget = if (selectedPeriod == "Month") budgetLimit else budgetLimit * 12
    
    val progressRatio = if (currentBudget > 0) (currentAmount / currentBudget).toFloat().coerceIn(0f, 1f) else 0f
    val percentUsed = if (currentBudget > 0) ((currentAmount / currentBudget) * 100) else 0.0

    val infiniteTransition = rememberInfiniteTransition(label = "refreshAnim")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAnim"
    )

    var isPressed by remember { mutableStateOf(false) }
    val buttonScale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "btnScale")

    fun formatNumber(amount: Double): String {
        val format = NumberFormat.getNumberInstance(Locale("en", "IN"))
        format.maximumFractionDigits = 2
        return format.format(amount)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) 
    ) {
        Column(
            modifier = Modifier.padding(16.dp) 
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Expenses", 
                    color = Color.Black, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 15.sp 
                )
                
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { expanded = true }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = selectedPeriod, 
                            fontSize = 13.sp, 
                            fontWeight = FontWeight.Medium, 
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown, 
                            contentDescription = "Select", 
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Month", color = Color.Black) },
                            onClick = { selectedPeriod = "Month"; expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Year", color = Color.Black) },
                            onClick = { selectedPeriod = "Year"; expanded = false }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp)) 
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildAnnotatedString {
                        // FIX: FontWeight.SemiBold lagaya gaya hai dono me "halka sa bold" effect ke liye
                        withStyle(style = SpanStyle(color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold, fontSize = 30.sp)) {
                            append("₹ ")
                        }
                        withStyle(style = SpanStyle(color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 30.sp)) {
                            append(formatNumber(currentAmount))
                        }
                    }
                )
                
                Box(
                    modifier = Modifier
                        .size(28.dp) 
                        .clip(CircleShape)
                        .background(Color(0xFFF5F5F5))
                        .clickable { onRefreshExpenses() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh, 
                        contentDescription = "Refresh", 
                        tint = Color.DarkGray,
                        modifier = Modifier
                            .size(18.dp) 
                            .rotate(if (isLoadingExpenses) angle else 0f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp)) 
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp) 
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFEEEEEE))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressRatio)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF2E7D32))
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "₹0", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = "${String.format(Locale.US, "%.1f", percentUsed)}% used", 
                    color = Color.DarkGray, 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold
                )
                Text(text = "₹${formatNumber(currentBudget)}", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            
            Spacer(modifier = Modifier.height(14.dp)) 
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onExpenseCardClick,
                    modifier = Modifier
                        .height(34.dp) 
                        .scale(buttonScale)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isPressed = true
                                    tryAwaitRelease()
                                    isPressed = false
                                }
                            )
                        },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp), 
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(50) 
                ) {
                    Text("View History", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
