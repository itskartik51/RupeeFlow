package com.kartikey.rupeeflow.UI_Screens.Add

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.UI_Screens.Assets.BankAccountItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.CashItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.CreditCardItem
import com.kartikey.rupeeflow.UI_Screens.bounceClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    username: String,
    showMenu: Boolean,
    onToggleMenu: () -> Unit,
    onExpenseAdded: (TransactionModel) -> Unit,
    onInvestmentAdded: () -> Unit,
    onFinanceAdded: () -> Unit,
    bankList: List<BankAccountItem> = emptyList(),
    ccList: List<CreditCardItem> = emptyList(),
    cashData: CashItem? = null
) {
    var activeAddForm by remember { mutableStateOf<String?>(null) } 
    
    BackHandler(enabled = showMenu || activeAddForm != null) {
        if (activeAddForm != null) {
            activeAddForm = null 
        } else if (showMenu) {
            onToggleMenu() 
        }
    }

    val dimAlpha by animateFloatAsState(targetValue = if (showMenu) 0.4f else 0f, label = "dimBg")
    if (showMenu || dimAlpha > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = dimAlpha))
                .pointerInput(Unit) { detectTapGestures(onTap = { onToggleMenu() }) }
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 90.dp), 
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = showMenu,
                enter = scaleIn(transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f), animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)) + fadeIn(),
                exit = scaleOut(transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)) + fadeOut()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(8.dp),
                        modifier = Modifier.width(220.dp)
                    ) {
                        Column {
                            AddMenuItem("Add Expense", Icons.Outlined.Receipt) {
                                activeAddForm = "Expense"
                                onToggleMenu()
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            AddMenuItem("Add Investment", Icons.Outlined.TrendingUp) {
                                activeAddForm = "Investment"
                                onToggleMenu()
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            AddMenuItem("Add Finance", Icons.Outlined.AccountBalance) {
                                activeAddForm = "Finance"
                                onToggleMenu()
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .offset(y = (-8).dp)
                            .size(16.dp)
                            .rotate(45f)
                            .background(MaterialTheme.colorScheme.surface)
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 16.dp)
                .size(48.dp)
                .bounceClick { onToggleMenu() }
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            val rotation by animateFloatAsState(
                targetValue = if (showMenu) 45f else 0f, 
                animationSpec = tween(300),
                label = "iconRotate"
            )
            Icon(Icons.Outlined.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.rotate(rotation))
        }
    }

    if (activeAddForm != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true) 
        ModalBottomSheet(
            onDismissRequest = { activeAddForm = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .imePadding() 
                    .navigationBarsPadding() 
            ) {
                Text(
                    text = "Add $activeAddForm",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
                )
                when (activeAddForm) {
                    "Expense" -> AddExpenseForm(username, bankList, ccList, cashData, onExpenseAdded = { onExpenseAdded(it) }, onDismiss = { activeAddForm = null })
                    "Investment" -> AddInvestmentForm(username, onInvestmentAdded = { onInvestmentAdded() }, onDismiss = { activeAddForm = null })
                    "Finance" -> AddFinanceForm(username, onFinanceAdded = { onFinanceAdded() }, onDismiss = { activeAddForm = null })
                }
            }
        }
    }
}

@Composable
fun AddMenuItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.95f) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}
