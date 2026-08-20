package com.kartikey.rupeeflow.UI_Screens.Assets

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.formatRupeeAmount
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import java.util.Locale
import kotlin.math.abs

@Composable
fun NetworthCard(
    networthAmount: Double = 0.0,
    oneDayReturnAmount: Double = 0.0,
    oneDayReturnPercent: Double = 0.0,
    totalReturnAmount: Double = 0.0,
    totalReturnPercent: Double = 0.0,
    isLoading: Boolean = false,
    onRefresh: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(true) }

    val infiniteTransition = rememberInfiniteTransition(label = "networthRefreshAnim")
    val rotateAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngle"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // --- TOP ROW: Title & Action Icons ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NET WORTH",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(
                        imageVector = if (isVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = "Toggle Visibility",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { isVisible = !isVisible }
                    )
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Refresh Data",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(if (isLoading) rotateAngle else 0f)
                            .clickable { onRefresh() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- MIDDLE ROW: Main Amount ---
            Text(
                text = if (isVisible) formatRupeeAmount(networthAmount) else "••••••••",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            // --- BOTTOM ROW: Returns ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Left Column (1D returns)
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "1D Returns",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val is1DPos = oneDayReturnAmount >= 0
                    val color1D = if (is1DPos) Color(0xFF388E3C) else Color(0xFFD32F2F)
                    val sign1D = if (is1DPos) "+" else "-"
                    val formattedAmt1D = formatRupeeAmount(abs(oneDayReturnAmount))
                    val formattedPct1D = String.format(Locale.US, "%.2f", abs(oneDayReturnPercent))
                    
                    Text(
                        text = if (isVisible) {
                            "$sign1D$formattedAmt1D ($formattedPct1D%)"
                        } else {
                            "•••• ($formattedPct1D%)"
                        },
                        color = color1D,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Right Column (Total returns)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Returns",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val isTotalPos = totalReturnAmount >= 0
                    val colorTotal = if (isTotalPos) Color(0xFF388E3C) else Color(0xFFD32F2F)
                    val signTotal = if (isTotalPos) "+" else "-"
                    val formattedAmtTotal = formatRupeeAmount(abs(totalReturnAmount))
                    val formattedPctTotal = String.format(Locale.US, "%.2f", abs(totalReturnPercent))

                    Text(
                        text = if (isVisible) {
                            "$signTotal$formattedAmtTotal ($formattedPctTotal%)"
                        } else {
                            "•••• ($formattedPctTotal%)"
                        },
                        color = colorTotal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
