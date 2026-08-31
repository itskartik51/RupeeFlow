package com.kartikey.rupeeflow.UI_Screens.Home.Contri.InContri

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.UI_Screens.bounceClick

// ==========================================
// CONTRI ROOM SUMMARY & ACTION CARD
// ==========================================
@Composable
fun ContriInfoCard(
    totalGroupExpense: Double,
    isSyncing: Boolean,
    actionProcessing: Boolean,
    currentRotation: Float,
    isAdmin: Boolean,
    roomCode: String,
    roomPin: String,
    onSyncClick: () -> Unit,
    onSettleClick: () -> Unit,
    onNewCycleClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹", 
                        fontSize = 22.sp, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant, 
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = totalGroupExpense.toInt().toString(), 
                        fontSize = 28.sp, 
                        fontWeight = FontWeight.ExtraBold, 
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    
                    Icon(
                        imageVector = Icons.Outlined.Sync,
                        contentDescription = "Sync",
                        tint = if (isSyncing || actionProcessing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer { scaleX = -1f }
                            .rotate(currentRotation)
                            .bounceClick { onSyncClick() }
                            .padding(2.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .bounceClick { onSettleClick() }
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Settle-up", 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    if (isAdmin) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .bounceClick { onNewCycleClick() }
                                .clip(RoundedCornerShape(50))
                                .border(1.2.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(50))
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "New", 
                                fontSize = 12.sp, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .bounceClick {
                                clipboardManager.setText(AnnotatedString("Join my RupeeFlow Contri!\nCode: $roomCode\nPin: $roomPin"))
                                Toast.makeText(context, "Code Copied!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy, 
                            contentDescription = "Copy", 
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = roomCode, 
                        fontSize = 17.sp, 
                        fontWeight = FontWeight.ExtraBold, 
                        color = MaterialTheme.colorScheme.onSurface, 
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Pin: $roomPin", 
                    fontSize = 13.sp, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
