package com.kartikey.rupeeflow.UI_Screens.Assets

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.formatRupeeAmount
import com.kartikey.rupeeflow.UI_Screens.RupeeFlowCard
import com.kartikey.rupeeflow.UI_Screens.bounceClick

@Composable
fun NetworthCard(
    networthAmount: Double = 0.0,
    isLoading: Boolean = false,
    onRefresh: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(true) }

    // Smooth spinning animation for refresh icon during background sync
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

    RupeeFlowCard(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onClick() }
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

            // --- MIDDLE ROW: Main Amount (Instant from Local Memory) ---
            Text(
                text = if (isVisible) formatRupeeAmount(networthAmount) else "••••••••",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
