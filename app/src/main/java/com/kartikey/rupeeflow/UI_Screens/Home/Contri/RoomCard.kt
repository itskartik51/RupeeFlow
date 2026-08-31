package com.kartikey.rupeeflow.UI_Screens.Home.Contri

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import java.text.SimpleDateFormat
import java.util.Locale

// ==========================================
// DATA MODEL FOR CONTRI ROOMS
// ==========================================
data class ContriRoomModel(
    val roomName: String,
    val roomCode: String,
    val lastUpdated: String,
    val pin: String = "123456"
)

// ==========================================
// ACTIVE ROOM CARD
// ==========================================
@Composable
fun ActiveRoomCard(
    room: ContriRoomModel, 
    onClick: () -> Unit, 
    onQrClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = room.roomName, 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.ExtraBold, 
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Update, 
                        contentDescription = "Last Updated", 
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatToDayMonth(room.lastUpdated), 
                        fontSize = 12.sp, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant, 
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .bounceClick { onQrClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.QrCode2, 
                    contentDescription = "Show QR", 
                    tint = MaterialTheme.colorScheme.onSurface, 
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ==========================================
// CONTRI GRID ACTION CARD (CREATE / JOIN)
// ==========================================
@Composable
fun ContriGridCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.bounceClick { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(bgColor, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(10.dp))
            
            Text(
                text = title, 
                fontSize = 14.sp, 
                fontWeight = FontWeight.ExtraBold, 
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,          
                softWrap = false,      
                overflow = TextOverflow.Ellipsis 
            )
        }
    }
}

// ==========================================
// DATE HELPER FORMATTER
// ==========================================
fun formatToDayMonth(dateStr: String): String {
    if (dateStr.isEmpty()) return "Newly Created"
    return try {
        val inputFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMMM", Locale.getDefault())
        val date = inputFormat.parse(dateStr)
        if (date != null) outputFormat.format(date) else dateStr
    } catch (e: Exception) {
        dateStr
    }
}
