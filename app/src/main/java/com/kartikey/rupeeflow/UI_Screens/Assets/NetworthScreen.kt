package com.kartikey.rupeeflow.UI_Screens.Assets

import android.content.Context
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.formatRupeeAmount
import com.kartikey.rupeeflow.UI_Screens.CacheManager
import com.kartikey.rupeeflow.UI_Screens.NetworthDataPoint
import com.kartikey.rupeeflow.UI_Screens.RupeeFlowCard
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object NetworthManager {
    fun getTimelineNetworth(context: Context, username: String): List<NetworthDataPoint> {
        val appData = CacheManager.getCachedData(context, username) ?: return emptyList()
        val timelineList = mutableListOf<NetworthDataPoint>()

        val monthNameFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val parseFormat = SimpleDateFormat("yy-MM", Locale.getDefault())

        val sortedMonths = appData.networthHistory.keys.sorted() 
        for (mKey in sortedMonths) {
            val slots = appData.networthHistory[mKey] ?: continue
            val monthDate = try { parseFormat.parse(mKey) } catch (e: Exception) { null }
            val mName = if (monthDate != null) monthNameFormat.format(monthDate) else mKey

            slots.forEachIndexed { index, amount ->
                if (amount > 0.0) {
                    val label = when (index) {
                        0 -> "01-10 $mName"
                        1 -> "11-20 $mName"
                        else -> "21-End $mName"
                    }
                    timelineList.add(NetworthDataPoint(mKey, index, label, amount))
                }
            }
        }
        return timelineList
    }

    fun syncNetworthSlot(context: Context, username: String, currentNetworth: Double) {
        if (currentNetworth <= 0.0) return
        val cached = CacheManager.getCachedData(context, username) ?: return
        
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val monthKey = SimpleDateFormat("yy-MM", Locale.getDefault()).format(cal.time)
        
        val slotIndex = when {
            day <= 10 -> 0
            day <= 20 -> 1
            else -> 2
        }

        val historyMap = cached.networthHistory.toMutableMap()
        val existingSlots = historyMap[monthKey]?.toMutableList() ?: mutableListOf(0.0, 0.0, 0.0)
        
        while (existingSlots.size < 3) {
            existingSlots.add(0.0)
        }

        existingSlots[slotIndex] = currentNetworth
        historyMap[monthKey] = existingSlots

        if (historyMap.size > 6) {
            val sortedKeys = historyMap.keys.sorted()
            val keysToRemove = sortedKeys.take(historyMap.size - 6)
            keysToRemove.forEach { historyMap.remove(it) }
        }

        val updatedData = cached.copy(networthHistory = historyMap)
        CacheManager.updateOptimisticCache(context, username, updatedData)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                if (!userQuery.isEmpty) {
                    val userRef = userQuery.documents[0].reference
                    userRef.set(mapOf("ntworth" to historyMap), SetOptions.merge()).await()
                }
            } catch (e: Exception) {}
        }
    }
}

@Composable
fun NetworthCard(
    networthAmount: Double = 0.0,
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

    RupeeFlowCard(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.94f) { onClick() }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

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

            Text(
                text = if (isVisible) formatRupeeAmount(networthAmount) else "••••••••",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
