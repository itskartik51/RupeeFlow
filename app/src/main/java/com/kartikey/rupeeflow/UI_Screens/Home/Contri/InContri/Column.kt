package com.kartikey.rupeeflow.UI_Screens.Home.Contri.InContri

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kartikey.rupeeflow.UI_Screens.bounceClick

// ==========================================
// REUSABLE UI COMPONENT: DYNAMIC LEDGER VIEW
// ==========================================
@Composable
fun DynamicLedgerView(
    currentUserId: String,
    ledgers: List<MemberLedger>,
    onEditExpense: (ContriExpense) -> Unit,
    onDeleteExpense: (ContriExpense) -> Unit
) {
    val memberCount = ledgers.size
    val isScrollable = memberCount > 3
    val fixedColumnWidth = 110.dp
    var revealedExpenseKey by remember { mutableStateOf<String?>(null) }
    val allFullNames = ledgers.map { it.memberName }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isScrollable) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
            .padding(horizontal = 16.dp)
    ) {
        Row(modifier = if (!isScrollable) Modifier.fillMaxWidth() else Modifier) {
            ledgers.forEach { ledger ->
                val isCurrentUser = ledger.userId == currentUserId
                val displayName = if (isCurrentUser) "You" else formatMemberDisplayName(ledger.memberName, allFullNames)

                Column(
                    modifier = if (isScrollable) Modifier.width(fixedColumnWidth) else Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = displayName, 
                            fontSize = 15.sp, 
                            fontWeight = FontWeight.ExtraBold, 
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (ledger.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Verified",
                                tint = Color(0xFF1DA1F2),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "₹${ledger.totalSpent.toInt()}", 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        val dividerModifier = if (isScrollable) Modifier.width(fixedColumnWidth * memberCount) else Modifier.fillMaxWidth()
        HorizontalDivider(modifier = dividerModifier.padding(vertical = 8.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))

        Row(modifier = if (!isScrollable) Modifier.fillMaxWidth() else Modifier) {
            ledgers.forEach { ledger ->
                val isCurrentUser = ledger.userId == currentUserId
                Column(
                    modifier = if (isScrollable) Modifier.width(fixedColumnWidth) else Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ledger.expenses.forEach { expense ->
                        val isRevealed = revealedExpenseKey == expense.key

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .zIndex(if (isRevealed) 50f else 1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = isCurrentUser) {
                                        revealedExpenseKey = if (isRevealed) null else expense.key
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = expense.itemName, 
                                        fontSize = 17.sp, 
                                        fontWeight = FontWeight.Bold, 
                                        color = MaterialTheme.colorScheme.onBackground, 
                                        maxLines = 1, 
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "₹${expense.amount.toInt()}", 
                                            fontSize = 13.sp, 
                                            fontWeight = FontWeight.Bold, 
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = expense.date, 
                                            fontSize = 11.sp, 
                                            color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                if (isCurrentUser) {
                                    AnimatedVisibility(
                                        visible = isRevealed,
                                        enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(tween(180)),
                                        exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut(tween(180))
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.surface,
                                            shadowElevation = 6.dp,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp, 
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                                            ),
                                            modifier = Modifier.padding(start = 6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                                        .bounceClick {
                                                            revealedExpenseKey = null
                                                            onEditExpense(expense)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Edit,
                                                        contentDescription = "Edit",
                                                        tint = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFFF5252).copy(alpha = 0.15f))
                                                        .bounceClick {
                                                            revealedExpenseKey = null
                                                            onDeleteExpense(expense)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Delete,
                                                        contentDescription = "Delete",
                                                        tint = Color(0xFFFF5252),
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SMART NAME FORMATTER
// ==========================================
fun formatMemberDisplayName(fullName: String, allFullNames: List<String>): String {
    val trimmed = fullName.trim()
    if (trimmed.isEmpty()) return "Unknown"
    val parts = trimmed.split("\\s+".toRegex())
    val firstName = parts.firstOrNull() ?: trimmed
    val lastName = if (parts.size > 1) parts.last() else ""

    val sameFirstNameCount = allFullNames.count { name ->
        val nameParts = name.trim().split("\\s+".toRegex())
        val otherFirst = nameParts.firstOrNull() ?: name.trim()
        otherFirst.equals(firstName, ignoreCase = true)
    }

    return if (sameFirstNameCount > 1 && lastName.isNotEmpty()) {
        "${lastName.first().uppercaseChar()} $firstName"
    } else {
        firstName
    }
}
