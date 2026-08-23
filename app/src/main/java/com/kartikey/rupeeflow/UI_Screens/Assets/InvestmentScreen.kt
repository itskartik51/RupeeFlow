package com.kartikey.rupeeflow.UI_Screens.Assets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kartikey.rupeeflow.UI_Screens.CacheManager
import com.kartikey.rupeeflow.UI_Screens.CustomDatePicker
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentScreen(
    onBackClick: () -> Unit, 
    username: String, 
    investmentList: List<InvestmentItem>,
    isLoading: Boolean = false, 
    onRefreshClick: () -> Unit = {}
) { 
    val context = LocalContext.current
    var isHidden by remember { mutableStateOf(false) }
    var editingLotInfo by remember { mutableStateOf<Triple<InvestmentItem, Int, InvestmentHistoryItem>?>(null) }
    var deletingAssetTarget by remember { mutableStateOf<InvestmentItem?>(null) }
    var deletingLotTarget by remember { mutableStateOf<Pair<InvestmentItem, Int>?>(null) }

    val totalInvested = investmentList.sumOf { it.quantity * it.avgBuyPrice }[cite: 1]
    val totalCurrent = investmentList.sumOf { it.quantity * it.currentPrice }[cite: 1]
    val total1DChange = investmentList.sumOf { it.quantity * it.oneDayChangePrice }[cite: 1]
    val totalReturn = totalCurrent - totalInvested[cite: 1]
    val totalReturnPercent = if (totalInvested > 0) (totalReturn / totalInvested) * 100 else 0.0[cite: 1]
    val total1DPercent = if (totalCurrent - total1DChange > 0) (total1DChange / (totalCurrent - total1DChange)) * 100 else 0.0[cite: 1]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Investments", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },[cite: 1]
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.bounceClick()) { 
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface) [cite: 1]
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)[cite: 1]
            )
        },
        containerColor = MaterialTheme.colorScheme.background[cite: 1]
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp),[cite: 1]
            contentPadding = PaddingValues(bottom = 90.dp)[cite: 1]
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))[cite: 1]
                
                InvestmentSummaryCard(
                    itemCount = investmentList.size,[cite: 1]
                    totalCurrent = totalCurrent,[cite: 1]
                    total1DChange = total1DChange,[cite: 1]
                    total1DPercent = total1DPercent,[cite: 1]
                    totalReturn = totalReturn,[cite: 1]
                    totalReturnPercent = totalReturnPercent,[cite: 1]
                    totalInvested = totalInvested,[cite: 1]
                    isLoading = isLoading, [cite: 1]
                    isHidden = isHidden,
                    onToggleVisibility = { isHidden = !isHidden },
                    onRefreshClick = onRefreshClick[cite: 1]
                )
                Spacer(modifier = Modifier.height(24.dp))[cite: 1]
                
                ListHeaderRow()[cite: 1]
                Spacer(modifier = Modifier.height(8.dp))[cite: 1]
            }

            items(investmentList, key = { it.assetName }) { item ->
                InvestmentListItem(
                    item = item,
                    isHidden = isHidden,
                    onDeleteAssetClick = { deletingAssetTarget = item },[cite: 1]
                    onEditLotClick = { lotIdx, lot -> editingLotInfo = Triple(item, lotIdx, lot) },[cite: 1]
                    onDeleteLotClick = { lotIdx -> deletingLotTarget = Pair(item, lotIdx) }[cite: 1]
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), [cite: 1]
                    thickness = 1.dp,  [cite: 1]
                    modifier = Modifier.padding(vertical = 8.dp)[cite: 1]
                )
            }
        }
    }

    // Delete Entire Asset Confirmation Dialog
    if (deletingAssetTarget != null) {
        AlertDialog(
            onDismissRequest = { deletingAssetTarget = null },[cite: 1]
            title = { Text("Delete Asset?", fontWeight = FontWeight.Bold) },[cite: 1]
            text = { Text("Do you want to delete it permanently? All purchase history for ${deletingAssetTarget?.assetName} will be removed.") },[cite: 1]
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingAssetTarget?.let {
                            CacheManager.deleteInvestment(context, username, it.assetName)[cite: 1]
                        }
                        deletingAssetTarget = null[cite: 1]
                    },
                    modifier = Modifier.bounceClick()[cite: 1]
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)[cite: 1]
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingAssetTarget = null }, modifier = Modifier.bounceClick()) {[cite: 1]
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)[cite: 1]
                }
            },
            shape = RoundedCornerShape(16.dp),[cite: 1]
            containerColor = MaterialTheme.colorScheme.surface[cite: 1]
        )
    }

    // Delete Lot Confirmation Dialog
    if (deletingLotTarget != null) {
        AlertDialog(
            onDismissRequest = { deletingLotTarget = null },[cite: 1]
            title = { Text("Delete Buy Record?", fontWeight = FontWeight.Bold) },[cite: 1]
            text = { Text("Do you want to delete this purchase entry permanently?") },[cite: 1]
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingLotTarget?.let { (asset, idx) ->
                            CacheManager.deleteHistoryLot(context, username, asset.assetName, idx)[cite: 1]
                        }
                        deletingLotTarget = null[cite: 1]
                    },
                    modifier = Modifier.bounceClick()[cite: 1]
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)[cite: 1]
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingLotTarget = null }, modifier = Modifier.bounceClick()) {[cite: 1]
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)[cite: 1]
                }
            },
            shape = RoundedCornerShape(16.dp),[cite: 1]
            containerColor = MaterialTheme.colorScheme.surface[cite: 1]
        )
    }

    // Edit Lot Popup Dialog
    if (editingLotInfo != null) {
        val (targetAsset, targetLotIndex, targetLot) = editingLotInfo!![cite: 1]
        EditHistoryLotDialog(
            initialLot = targetLot,[cite: 1]
            onDismiss = { editingLotInfo = null },[cite: 1]
            onSave = { updatedLot ->
                CacheManager.editHistoryLot(context, username, targetAsset.assetName, targetLotIndex, updatedLot)[cite: 1]
                editingLotInfo = null[cite: 1]
            }
        )
    }
}

@Composable
fun InvestmentSummaryCard(
    itemCount: Int, 
    totalCurrent: Double, 
    total1DChange: Double, 
    total1DPercent: Double, 
    totalReturn: Double, 
    totalReturnPercent: Double, 
    totalInvested: Double,
    isLoading: Boolean, 
    isHidden: Boolean,
    onToggleVisibility: () -> Unit,
    onRefreshClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "refreshAnim")[cite: 1]
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,[cite: 1]
        targetValue = 360f,[cite: 1]
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),[cite: 1]
            repeatMode = RepeatMode.Restart[cite: 1]
        ),
        label = "spinAnim"[cite: 1]
    )

    Card(
        modifier = Modifier.fillMaxWidth(),[cite: 1]
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),[cite: 1]
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(16.dp)[cite: 1]
    ) {
        Column(modifier = Modifier.padding(20.dp)) {[cite: 1]
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween,[cite: 1]
                verticalAlignment = Alignment.CenterVertically[cite: 1]
            ) {
                Text(
                    text = "INVESTMENT ($itemCount)", [cite: 1]
                    color = MaterialTheme.colorScheme.onSurfaceVariant, [cite: 1]
                    fontSize = 12.sp, [cite: 1]
                    fontWeight = FontWeight.Bold, [cite: 1]
                    letterSpacing = 1.sp[cite: 1]
                )
                
                // Circular Pod Action Icons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Eye Visibility Toggle
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { onToggleVisibility() }
                            .bounceClick(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (isHidden) "Show" else "Hide",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Refresh Button with Spin Animation
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { onRefreshClick() }
                            .bounceClick(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(17.dp)
                                .rotate(if (isLoading) angle else 0f)[cite: 1]
                        )
                    }

                    // 3-Dots More Options
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { }
                            .bounceClick(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isHidden) "•••••" else formatRupee(totalCurrent), 
                fontWeight = FontWeight.ExtraBold, 
                fontSize = 32.sp, 
                color = MaterialTheme.colorScheme.onSurface[cite: 1]
            )
            
            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))[cite: 1]
            Spacer(modifier = Modifier.height(16.dp))[cite: 1]

            SummaryRow("1D returns", total1DChange, total1DPercent, isHidden)
            Spacer(modifier = Modifier.height(12.dp))[cite: 1]
            
            SummaryRow("Total returns", totalReturn, totalReturnPercent, isHidden)
            Spacer(modifier = Modifier.height(12.dp))[cite: 1]
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {[cite: 1]
                Text("Invested", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)[cite: 1]
                Text(
                    text = if (isHidden) "•••••" else formatRupee(totalInvested), 
                    color = MaterialTheme.colorScheme.onSurface, 
                    fontWeight = FontWeight.Medium, 
                    fontSize = 14.sp[cite: 1]
                )
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, amount: Double, percent: Double, isHidden: Boolean) {
    val isPositive = amount >= 0[cite: 1]
    val color = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error[cite: 1]
    val sign = if (isPositive) "+" else ""[cite: 1]

    val amountDisplay = if (isHidden) "•••••" else "$sign${formatRupee(amount)}"

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {[cite: 1]
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)[cite: 1]
        Text(
            text = "$amountDisplay ($sign${String.format(Locale.US, "%.2f", percent)}%)",[cite: 1]
            color = color, 
            fontWeight = FontWeight.Medium, 
            fontSize = 14.sp[cite: 1]
        )
    }
}

@Composable
fun ListHeaderRow() {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {[cite: 1]
        Text("Data", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.8f))[cite: 1]
        Text("Market Price\n(1D %)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End, maxLines = 2, modifier = Modifier.weight(1.3f))[cite: 1]
        Text("Current\n(Invested)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End, maxLines = 2, modifier = Modifier.weight(1.2f))[cite: 1]
        Text("Returns\n(%)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End, maxLines = 2, modifier = Modifier.weight(1f))[cite: 1]
    }
}

@Composable
fun InvestmentListItem(
    item: InvestmentItem,
    isHidden: Boolean,
    onDeleteAssetClick: () -> Unit,
    onEditLotClick: (Int, InvestmentHistoryItem) -> Unit,
    onDeleteLotClick: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }[cite: 1]
    val bannerOffsetX = remember { Animatable(0f) }[cite: 1]
    val coroutineScope = rememberCoroutineScope()[cite: 1]
    val density = LocalDensity.current[cite: 1]
    val maxBannerSwipe = with(density) { 70.dp.toPx() }[cite: 1]

    val currentVal = item.quantity * item.currentPrice[cite: 1]
    val investedVal = item.quantity * item.avgBuyPrice[cite: 1]
    val totalRet = currentVal - investedVal[cite: 1]
    val totalRetPct = if (investedVal > 0) (totalRet / investedVal) * 100 else 0.0[cite: 1]
    val oneDPct = if (item.currentPrice - item.oneDayChangePrice > 0) (item.oneDayChangePrice / (item.currentPrice - item.oneDayChangePrice)) * 100 else 0.0[cite: 1]

    val oneDayColor = if (item.oneDayChangePrice >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error[cite: 1]
    val oneDaySign = if (item.oneDayChangePrice >= 0) "+" else ""[cite: 1]
    
    val totalRetColor = if (totalRet >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error[cite: 1]
    val totalRetSign = if (totalRet >= 0) "+" else ""[cite: 1]

    val currentDisplay = if (isHidden) "•••••" else formatRupee(currentVal)
    val investedDisplay = if (isHidden) "(•••••)" else "(${formatRupee(investedVal)})"
    val retDisplay = if (isHidden) "•••••" else "$totalRetSign${formatRupee(totalRet)}"
    val oneDayPriceDisplay = if (isHidden) "•••••" else "$oneDaySign${String.format(Locale.US, "%.2f", item.oneDayChangePrice)}"

    Column(modifier = Modifier.fillMaxWidth()) {[cite: 1]
        // Main Stock Row (Banner) with Swipe-to-Delete Action Reveal
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))[cite: 1]
        ) {
            // Background Action (Delete Bin)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.errorContainer),[cite: 1]
                contentAlignment = Alignment.CenterEnd[cite: 1]
            ) {
                IconButton(
                    onClick = {
                        coroutineScope.launch { bannerOffsetX.animateTo(0f) }[cite: 1]
                        onDeleteAssetClick()[cite: 1]
                    },
                    modifier = Modifier
                        .padding(end = 12.dp)[cite: 1]
                        .bounceClick()[cite: 1]
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,[cite: 1]
                        contentDescription = "Delete Asset",[cite: 1]
                        tint = MaterialTheme.colorScheme.onErrorContainer[cite: 1]
                    )
                }
            }

            // Foreground Main Stock Banner Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(bannerOffsetX.value.roundToInt(), 0) }[cite: 1]
                    .background(MaterialTheme.colorScheme.background)[cite: 1]
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val newOffset = (bannerOffsetX.value + dragAmount).coerceIn(-maxBannerSwipe, 0f)
                                coroutineScope.launch { bannerOffsetX.snapTo(newOffset) }
                            },
                            onDragEnd = {
                                coroutineScope.launch {
                                    if (bannerOffsetX.value < -maxBannerSwipe / 2) {
                                        bannerOffsetX.animateTo(-maxBannerSwipe, spring(stiffness = Spring.StiffnessMediumLow))
                                    } else {
                                        bannerOffsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                    }
                                }
                            }
                        )
                    }
                    .clickable { 
                        if (bannerOffsetX.value < 0f) {
                            coroutineScope.launch { bannerOffsetX.animateTo(0f) }[cite: 1]
                        } else {
                            expanded = !expanded [cite: 1]
                        }
                    },
                verticalAlignment = Alignment.CenterVertically[cite: 1]
            ) {
                Column(modifier = Modifier.weight(0.8f)) {[cite: 1]
                    Text(item.assetName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)[cite: 1]
                    val qtyDisplay = if (item.quantity % 1.0 == 0.0) "${item.quantity.toInt()} shares" else "${String.format(Locale.US, "%.3f", item.quantity)} units"[cite: 1]
                    Text(qtyDisplay, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)[cite: 1]
                }
                
                Column(modifier = Modifier.weight(1.3f), horizontalAlignment = Alignment.End) {[cite: 1]
                    Text(formatRupee(item.currentPrice), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)[cite: 1]
                    Text(
                        text = "$oneDayPriceDisplay ($oneDaySign${String.format(Locale.US, "%.2f", oneDPct)}%)",
                        fontSize = 11.sp, 
                        color = oneDayColor
                    )
                }
                
                Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {[cite: 1]
                    Text(currentDisplay, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                    Text(investedDisplay, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {[cite: 1]
                    Text(retDisplay, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                    Text("($totalRetSign${String.format(Locale.US, "%.2f", totalRetPct)}%)", fontSize = 11.sp, color = totalRetColor)[cite: 1]
                }
            }
        }

        // Expandable Sub-Banner (Purchase History)
        AnimatedVisibility(
            visible = expanded,[cite: 1]
            enter = expandVertically() + fadeIn(),[cite: 1]
            exit = shrinkVertically() + fadeOut()[cite: 1]
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),[cite: 1]
                shape = RoundedCornerShape(12.dp),[cite: 1]
                color = MaterialTheme.colorScheme.surface,[cite: 1]
                tonalElevation = 0.dp,[cite: 1]
                shadowElevation = 0.dp[cite: 1]
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)[cite: 1]
                ) {
                    // Sub-Table Headers
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {[cite: 1]
                        Text(
                            text = "Date",[cite: 1]
                            fontSize = 10.5.sp,[cite: 1]
                            fontWeight = FontWeight.Bold,[cite: 1]
                            color = MaterialTheme.colorScheme.onSurfaceVariant,[cite: 1]
                            modifier = Modifier.weight(0.7f)[cite: 1]
                        )
                        Text(
                            text = "Price (Qty)\n(Invested)",[cite: 1]
                            fontSize = 10.sp,[cite: 1]
                            color = MaterialTheme.colorScheme.onSurfaceVariant,[cite: 1]
                            textAlign = TextAlign.End,[cite: 1]
                            maxLines = 2,[cite: 1]
                            modifier = Modifier.weight(1.35f)[cite: 1]
                        )
                        Text(
                            text = "Brkg",[cite: 1]
                            fontSize = 10.sp,[cite: 1]
                            color = MaterialTheme.colorScheme.onSurfaceVariant,[cite: 1]
                            textAlign = TextAlign.End,[cite: 1]
                            maxLines = 1,[cite: 1]
                            modifier = Modifier.weight(0.65f)[cite: 1]
                        )
                        Text(
                            text = "P/L (%)\n(Current)",[cite: 1]
                            fontSize = 10.sp,[cite: 1]
                            color = MaterialTheme.colorScheme.onSurfaceVariant,[cite: 1]
                            textAlign = TextAlign.End,[cite: 1]
                            maxLines = 2,[cite: 1]
                            modifier = Modifier.weight(1.6f)[cite: 1]
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))[cite: 1]
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), thickness = 0.8.dp)[cite: 1]
                    Spacer(modifier = Modifier.height(6.dp))[cite: 1]

                    // History Rows with Swipe Actions
                    item.history.forEachIndexed { lotIdx, historyEntry ->
                        SubBannerHistoryRow(
                            lotIdx = lotIdx,[cite: 1]
                            historyEntry = historyEntry,[cite: 1]
                            currentPrice = item.currentPrice,[cite: 1]
                            isHidden = isHidden,
                            onEditClick = { onEditLotClick(lotIdx, historyEntry) },[cite: 1]
                            onDeleteClick = { onDeleteLotClick(lotIdx) }[cite: 1]
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubBannerHistoryRow(
    lotIdx: Int,
    historyEntry: InvestmentHistoryItem,
    currentPrice: Double,
    isHidden: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()[cite: 1]
    val lotOffsetX = remember { Animatable(0f) }[cite: 1]
    val density = LocalDensity.current[cite: 1]
    val maxSubSwipe = with(density) { 120.dp.toPx() }[cite: 1]

    val (dateLine1, dateLine2) = formatHistoryDate(historyEntry.date)[cite: 1]
    val trancheQtyDisplay = if (historyEntry.quantity % 1.0 == 0.0) "${historyEntry.quantity.toInt()}" else String.format(Locale.US, "%.3f", historyEntry.quantity)[cite: 1]
    val trancheInvested = historyEntry.quantity * historyEntry.price[cite: 1]
    val trancheCurrent = historyEntry.quantity * currentPrice[cite: 1]
    val tranchePL = trancheCurrent - trancheInvested[cite: 1]
    val tranchePct = if (historyEntry.price > 0) ((currentPrice - historyEntry.price) / historyEntry.price) * 100 else 0.0[cite: 1]

    val plColor = if (tranchePL >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error[cite: 1]
    val plSign = if (tranchePL >= 0) "+" else ""[cite: 1]

    val trancheInvestedDisplay = if (isHidden) "(•••••)" else "(${formatRupee(trancheInvested)})"
    val trancheCurrentDisplay = if (isHidden) "(•••••)" else "(${formatRupee(trancheCurrent)})"
    val tranchePLDisplay = if (isHidden) "•••••" else "$plSign${formatRupee(tranchePL)}"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))[cite: 1]
            .padding(vertical = 3.dp)[cite: 1]
    ) {
        // Actions Background (Edit + Delete)
        Row(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),[cite: 1]
            horizontalArrangement = Arrangement.End,[cite: 1]
            verticalAlignment = Alignment.CenterVertically[cite: 1]
        ) {
            IconButton(
                onClick = {
                    coroutineScope.launch { lotOffsetX.animateTo(0f) }[cite: 1]
                    onEditClick()[cite: 1]
                },
                modifier = Modifier
                    .size(38.dp)[cite: 1]
                    .bounceClick()[cite: 1]
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,[cite: 1]
                    contentDescription = "Edit Lot",[cite: 1]
                    tint = MaterialTheme.colorScheme.primary,[cite: 1]
                    modifier = Modifier.size(18.dp)[cite: 1]
                )
            }
            IconButton(
                onClick = {
                    coroutineScope.launch { lotOffsetX.animateTo(0f) }[cite: 1]
                    onDeleteClick()[cite: 1]
                },
                modifier = Modifier
                    .size(38.dp)[cite: 1]
                    .bounceClick()[cite: 1]
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,[cite: 1]
                    contentDescription = "Delete Lot",[cite: 1]
                    tint = MaterialTheme.colorScheme.error,[cite: 1]
                    modifier = Modifier.size(18.dp)[cite: 1]
                )
            }
        }

        // Foreground Lot Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(lotOffsetX.value.roundToInt(), 0) }[cite: 1]
                .background(MaterialTheme.colorScheme.surface)[cite: 1]
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = (lotOffsetX.value + dragAmount).coerceIn(-maxSubSwipe, 0f)
                            coroutineScope.launch { lotOffsetX.snapTo(newOffset) }
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                if (lotOffsetX.value < -maxSubSwipe / 2) {
                                    lotOffsetX.animateTo(-maxSubSwipe, spring(stiffness = Spring.StiffnessMediumLow))
                                } else {
                                    lotOffsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                }
                            }
                        }
                    )
                }
                .clickable {
                    if (lotOffsetX.value < 0f) {
                        coroutineScope.launch { lotOffsetX.animateTo(0f) }[cite: 1]
                    }
                }
                .padding(vertical = 4.dp),[cite: 1]
            verticalAlignment = Alignment.CenterVertically[cite: 1]
        ) {
            // Date Column
            Column(modifier = Modifier.weight(0.7f)) {[cite: 1]
                Text(
                    text = dateLine1,[cite: 1]
                    fontSize = 11.sp,[cite: 1]
                    fontWeight = FontWeight.Medium,[cite: 1]
                    color = MaterialTheme.colorScheme.onSurfaceVariant,[cite: 1]
                    maxLines = 1[cite: 1]
                )
                if (dateLine2.isNotEmpty()) {[cite: 1]
                    Text(
                        text = dateLine2,[cite: 1]
                        fontSize = 10.sp,[cite: 1]
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),[cite: 1]
                        maxLines = 1[cite: 1]
                    )
                }
            }

            // Price (Qty) & (Invested)
            Column(modifier = Modifier.weight(1.35f), horizontalAlignment = Alignment.End) {[cite: 1]
                Text(
                    text = "${formatRupee(historyEntry.price)} ($trancheQtyDisplay)",[cite: 1]
                    fontSize = 11.5.sp,[cite: 1]
                    fontWeight = FontWeight.Medium,[cite: 1]
                    color = MaterialTheme.colorScheme.onSurface,[cite: 1]
                    maxLines = 1[cite: 1]
                )
                Text(
                    text = trancheInvestedDisplay,
                    fontSize = 10.sp,[cite: 1]
                    color = MaterialTheme.colorScheme.onSurfaceVariant,[cite: 1]
                    maxLines = 1[cite: 1]
                )
            }

            // Brkg Column
            Column(modifier = Modifier.weight(0.65f), horizontalAlignment = Alignment.End) {[cite: 1]
                Text(
                    text = if (historyEntry.brokerage > 0) formatRupee(historyEntry.brokerage) else "₹0",[cite: 1]
                    fontSize = 11.sp,[cite: 1]
                    color = MaterialTheme.colorScheme.onSurfaceVariant,[cite: 1]
                    maxLines = 1[cite: 1]
                )
            }

            // P/L (%) & (Current)
            Column(modifier = Modifier.weight(1.6f), horizontalAlignment = Alignment.End) {[cite: 1]
                Text(
                    text = "$tranchePLDisplay ($plSign${String.format(Locale.US, "%.2f", tranchePct)}%)",
                    fontSize = 11.5.sp,[cite: 1]
                    fontWeight = FontWeight.Medium,[cite: 1]
                    color = plColor,[cite: 1]
                    maxLines = 1[cite: 1]
                )
                Text(
                    text = trancheCurrentDisplay,
                    fontSize = 10.sp,[cite: 1]
                    color = MaterialTheme.colorScheme.onSurfaceVariant,[cite: 1]
                    maxLines = 1[cite: 1]
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHistoryLotDialog(
    initialLot: InvestmentHistoryItem,
    onDismiss: () -> Unit,
    onSave: (InvestmentHistoryItem) -> Unit
) {
    var qtyText by remember { mutableStateOf(if (initialLot.quantity % 1.0 == 0.0) initialLot.quantity.toInt().toString() else initialLot.quantity.toString()) }[cite: 1]
    var priceText by remember { mutableStateOf(if (initialLot.price % 1.0 == 0.0) initialLot.price.toInt().toString() else initialLot.price.toString()) }[cite: 1]
    var dateText by remember { mutableStateOf(initialLot.date) }[cite: 1]
    var selectedDateMillis by remember { mutableStateOf(parseDateToMillis(initialLot.date)) }[cite: 1]
    var brkgText by remember { mutableStateOf(if (initialLot.brokerage % 1.0 == 0.0) initialLot.brokerage.toInt().toString() else initialLot.brokerage.toString()) }[cite: 1]

    Dialog(onDismissRequest = onDismiss) {[cite: 1]
        Card(
            shape = RoundedCornerShape(20.dp),[cite: 1]
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),[cite: 1]
            modifier = Modifier.fillMaxWidth()[cite: 1]
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),[cite: 1]
                horizontalAlignment = Alignment.CenterHorizontally[cite: 1]
            ) {
                // Row 1: Quantity & Buy Price
                Row(
                    modifier = Modifier.fillMaxWidth(),[cite: 1]
                    horizontalArrangement = Arrangement.spacedBy(12.dp)[cite: 1]
                ) {
                    OutlinedTextField(
                        value = qtyText,[cite: 1]
                        onValueChange = { qtyText = it },[cite: 1]
                        label = { Text("Quantity") },[cite: 1]
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),[cite: 1]
                        modifier = Modifier.weight(1f),[cite: 1]
                        shape = RoundedCornerShape(12.dp),[cite: 1]
                        singleLine = true[cite: 1]
                    )

                    OutlinedTextField(
                        value = priceText,[cite: 1]
                        onValueChange = { priceText = it },[cite: 1]
                        label = { Text("Buy Price") },[cite: 1]
                        prefix = if (priceText.isNotEmpty()) {[cite: 1]
                            { Text("₹", color = MaterialTheme.colorScheme.onSurface) }[cite: 1]
                        } else null,[cite: 1]
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),[cite: 1]
                        modifier = Modifier.weight(1f),[cite: 1]
                        shape = RoundedCornerShape(12.dp),[cite: 1]
                        singleLine = true[cite: 1]
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))[cite: 1]

                // Row 2: Date (60%) & Brokerage (40%)
                Row(
                    modifier = Modifier.fillMaxWidth(),[cite: 1]
                    horizontalArrangement = Arrangement.spacedBy(12.dp),[cite: 1]
                    verticalAlignment = Alignment.CenterVertically[cite: 1]
                ) {
                    CustomDatePicker(
                        label = "Date",[cite: 1]
                        selectedDateMillis = selectedDateMillis,[cite: 1]
                        onDateSelected = { millis ->
                            selectedDateMillis = millis[cite: 1]
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())[cite: 1]
                            dateText = sdf.format(Date(millis))[cite: 1]
                        },
                        modifier = Modifier.weight(0.6f)[cite: 1]
                    )

                    OutlinedTextField(
                        value = brkgText,[cite: 1]
                        onValueChange = { brkgText = it },[cite: 1]
                        label = { Text("Brokerage") },[cite: 1]
                        prefix = if (brkgText.isNotEmpty()) {[cite: 1]
                            { Text("₹", color = MaterialTheme.colorScheme.onSurface) }[cite: 1]
                        } else null,[cite: 1]
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),[cite: 1]
                        modifier = Modifier.weight(0.4f),[cite: 1]
                        shape = RoundedCornerShape(12.dp),[cite: 1]
                        singleLine = true[cite: 1]
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))[cite: 1]

                // Save Investment Button
                Button(
                    onClick = {
                        val parsedQty = qtyText.toDoubleOrNull() ?: initialLot.quantity[cite: 1]
                        val parsedPrice = priceText.toDoubleOrNull() ?: initialLot.price[cite: 1]
                        val parsedBrkg = brkgText.toDoubleOrNull() ?: 0.0[cite: 1]
                        val updated = InvestmentHistoryItem(
                            date = dateText.trim(),[cite: 1]
                            quantity = parsedQty,[cite: 1]
                            price = parsedPrice,[cite: 1]
                            amount = parsedQty * parsedPrice,[cite: 1]
                            brokerage = parsedBrkg[cite: 1]
                        )
                        onSave(updated)[cite: 1]
                    },
                    modifier = Modifier
                        .fillMaxWidth()[cite: 1]
                        .height(50.dp)[cite: 1]
                        .bounceClick(),[cite: 1]
                    shape = RoundedCornerShape(14.dp),[cite: 1]
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF22C55E),[cite: 1]
                        contentColor = Color.Black[cite: 1]
                    )
                ) {
                    Text("Save Investment", fontWeight = FontWeight.Bold, fontSize = 16.sp)[cite: 1]
                }
            }
        }
    }
}

fun parseDateToMillis(dateStr: String): Long? {
    if (dateStr.isBlank()) return null[cite: 1]
    return try {
        val inFormat = if (dateStr.contains("/")) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())[cite: 1]
                       else if (dateStr.contains("-")) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())[cite: 1]
                       else SimpleDateFormat("dd MMM yyyy", Locale.getDefault())[cite: 1]
        inFormat.parse(dateStr)?.time[cite: 1]
    } catch (e: Exception) {
        null[cite: 1]
    }
}

fun formatHistoryDate(dateStr: String): Pair<String, String> {
    if (dateStr.isBlank()) return Pair("-", "")[cite: 1]
    return try {
        val inFormat = if (dateStr.contains("/")) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())[cite: 1]
                       else if (dateStr.contains("-")) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())[cite: 1]
                       else SimpleDateFormat("dd MMM yyyy", Locale.getDefault())[cite: 1]
        val dateObj = inFormat.parse(dateStr)[cite: 1]
        if (dateObj != null) {
            val line1 = SimpleDateFormat("dd MMM", Locale.getDefault()).format(dateObj)[cite: 1]
            val line2 = SimpleDateFormat("yyyy", Locale.getDefault()).format(dateObj)[cite: 1]
            Pair(line1, line2)[cite: 1]
        } else {
            Pair(dateStr, "")[cite: 1]
        }
    } catch (e: Exception) {
        Pair(dateStr, "")[cite: 1]
    }
}

fun formatRupee(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))[cite: 1]
    format.maximumFractionDigits = 2[cite: 1]
    return format.format(amount)[cite: 1]
}
