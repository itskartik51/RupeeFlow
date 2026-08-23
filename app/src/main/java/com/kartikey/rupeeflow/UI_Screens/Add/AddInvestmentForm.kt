package com.kartikey.rupeeflow.UI_Screens.Add

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.kartikey.rupeeflow.UI_Screens.CustomDatePicker
import com.kartikey.rupeeflow.UI_Screens.MarketEngine
import com.kartikey.rupeeflow.UI_Screens.MarketSearchResult
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInvestmentForm(username: String, onInvestmentAdded: () -> Unit, onDismiss: () -> Unit) { 
    var assetType by remember { mutableStateOf("") }
    var assetName by remember { mutableStateOf("") }
    
    var selectedSymbol by remember { mutableStateOf("") }
    var sheetTicker by remember { mutableStateOf("") }
    
    var quantity by remember { mutableStateOf("") }
    var buyPrice by remember { mutableStateOf("") }
    
    var selectedDateMillis by remember { mutableStateOf<Long?>(System.currentTimeMillis()) }
    var brokerage by remember { mutableStateOf("") }
    
    var typeExpanded by remember { mutableStateOf(false) }
    
    var searchResults by remember { mutableStateOf<List<MarketSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // ⚡ CLEAN SEARCH & PRICING POWERED BY STANDALONE MARKET ENGINE ⚡
    LaunchedEffect(assetName, assetType) {
        if (assetName.isBlank() || selectedSymbol.isNotEmpty() || assetType.isEmpty()) {
            searchResults = emptyList()
            return@LaunchedEffect
        }
        
        delay(500) 
        isSearching = true
        
        val results = MarketEngine.searchAssets(query = assetName, assetType = assetType)
        searchResults = results
        isSearching = false
    }

    Card(
        modifier = Modifier.fillMaxWidth(), 
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
            
            Text(
                text = "Choose Investment Type", 
                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                fontSize = 12.sp, 
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = !typeExpanded }
            ) {
                OutlinedTextField(
                    value = if (assetType.isEmpty()) "Select Asset Type" else assetType,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    maxLines = 1,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary, 
                        unfocusedBorderColor = if (assetType.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), 
                        focusedTextColor = MaterialTheme.colorScheme.onSurface, 
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    listOf("Stock", "Mutual Fund", "ETF", "Bond").forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            onClick = {
                                assetType = selectionOption
                                assetName = "" 
                                selectedSymbol = ""
                                sheetTicker = ""
                                quantity = ""
                                buyPrice = ""
                                selectedDateMillis = System.currentTimeMillis()
                                brokerage = ""
                                searchResults = emptyList()
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            if (assetType.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = assetName,
                    onValueChange = { 
                        assetName = it 
                        selectedSymbol = "" 
                        sheetTicker = ""
                    },
                    label = { Text("Search $assetType Name", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    maxLines = 1,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary, 
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), 
                        focusedTextColor = MaterialTheme.colorScheme.onSurface, 
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    trailingIcon = { 
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                        } else if (assetName.isNotEmpty()) {
                            IconButton(onClick = { 
                                assetName = ""
                                selectedSymbol = ""
                                sheetTicker = ""
                                searchResults = emptyList()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                )

                if (assetName.isNotEmpty() && searchResults.isNotEmpty() && selectedSymbol.isEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            searchResults.forEachIndexed { index, row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val prefix = if (row.rawSymbol.endsWith(".NS")) "NSE:" else if (row.rawSymbol.endsWith(".BO")) "BOM:" else "NSE:"
                                            
                                            assetName = if (assetType == "Mutual Fund") {
                                                if (row.fullName.length > 25) row.fullName.take(25) + "..." else row.fullName
                                            } else {
                                                row.cleanSymbol
                                            }
                                            
                                            selectedSymbol = if (assetType == "Mutual Fund") row.rawSymbol else row.cleanSymbol
                                            sheetTicker = prefix + row.cleanSymbol
                                            
                                            if (row.livePrice > 0.0) {
                                                buyPrice = String.format(Locale.US, "%.2f", row.livePrice)
                                            }
                                            searchResults = emptyList()
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(0.7f)) {
                                        if (assetType == "Mutual Fund") {
                                            Text(
                                                text = row.fullName.ifEmpty { "Mutual Fund" }, 
                                                fontWeight = FontWeight.Bold, 
                                                fontSize = 14.5.sp, 
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        } else {
                                            Text(
                                                text = row.cleanSymbol, 
                                                fontWeight = FontWeight.Bold, 
                                                fontSize = 14.5.sp, 
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            
                                            if (row.fullName.isNotBlank() && !row.fullName.equals(row.cleanSymbol, ignoreCase = true)) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                val cleanBracketName = if (row.fullName.length > 22) "${row.fullName.take(20)}..." else row.fullName
                                                Text(
                                                    text = "($cleanBracketName)", 
                                                    fontSize = 11.5.sp, 
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                                    maxLines = 1, 
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                    
                                    Text(
                                        text = if (row.livePrice > 0.0) "₹ ${String.format(Locale.US, "%.2f", row.livePrice)}" else "-",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.5.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(0.3f),
                                        textAlign = TextAlign.End,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (index < searchResults.size - 1) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val isMutualFund = assetType == "Mutual Fund"
                val qtyLabel = if (isMutualFund) "Total Units" else "Quantity"
                val priceLabel = if (isMutualFund) "Average NAV" else "Buy Price"

                // --- QUANTITY & BUY PRICE ---
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = quantity, 
                        onValueChange = { 
                            if (isMutualFund) {
                                quantity = it 
                            } else {
                                if (it.all { char -> char.isDigit() }) quantity = it 
                            }
                        },
                        label = { Text(qtyLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        keyboardOptions = KeyboardOptions(keyboardType = if (isMutualFund) KeyboardType.Decimal else KeyboardType.Number),
                        modifier = Modifier.weight(1f), 
                        singleLine = true,
                        maxLines = 1,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                    )
                    OutlinedTextField(
                        value = buyPrice, onValueChange = { buyPrice = it },
                        label = { Text(priceLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), 
                        singleLine = true,
                        maxLines = 1,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- DATE PICKER & BROKERAGE (60:40 RATIO WITH STRICT SINGLE LINE) ---
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CustomDatePicker(
                        label = "Date",
                        selectedDateMillis = selectedDateMillis,
                        onDateSelected = { selectedDateMillis = it },
                        modifier = Modifier.weight(0.6f),
                        restrictToCurrentMonth = false
                    )
                    OutlinedTextField(
                        value = brokerage,
                        onValueChange = { brokerage = it },
                        label = { Text("Brokerage", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(0.4f),
                        singleLine = true,
                        maxLines = 1,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                val currentQty = quantity.toDoubleOrNull() ?: 0.0
                val currentPrice = buyPrice.toDoubleOrNull() ?: 0.0
                val totalAmount = currentQty * currentPrice
                if (totalAmount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Total Invested: ₹${String.format(Locale.US, "%.2f", totalAmount)}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- SAVE BUTTON ---
                Button(
                    onClick = {
                        val qty = quantity.toDoubleOrNull() ?: 0.0
                        val price = buyPrice.toDoubleOrNull() ?: 0.0
                        val brkrgVal = brokerage.toDoubleOrNull() ?: 0.0
                        val dateVal = selectedDateMillis ?: System.currentTimeMillis()
                        
                        if (selectedSymbol.isNotBlank() && qty > 0 && price > 0) {
                            onInvestmentAdded()
                            onDismiss() 
                            
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val db = FirebaseFirestore.getInstance()
                                    val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                                    
                                    if (!userQuery.isEmpty) {
                                        val userDoc = userQuery.documents[0]
                                        val userRef = userDoc.reference
                                        
                                        val rawInvestMap = userDoc.get("invest") as? Map<String, Any> ?: emptyMap()
                                        
                                        var existingKey: String? = null
                                        var existingHolding: Map<String, Any>? = null

                                        for ((key, value) in rawInvestMap) {
                                            val holding = value as? Map<String, Any>
                                            if (holding != null) {
                                                val existingName = holding["name"] as? String ?: ""
                                                val existingType = holding["type"] as? String ?: ""
                                                if (existingName.equals(selectedSymbol, ignoreCase = true) && 
                                                    existingType.equals(assetType, ignoreCase = true)) {
                                                    existingKey = key
                                                    existingHolding = holding
                                                    break
                                                }
                                            }
                                        }

                                        val newHistoryEntry = mapOf(
                                            "dt" to Timestamp(Date(dateVal)),
                                            "qnt" to qty,
                                            "prc" to price,
                                            "amnt" to (qty * price),
                                            "brkrg" to brkrgVal
                                        )

                                        if (existingKey != null && existingHolding != null) {
                                            val oldQty = (existingHolding["qnt"] as? Number)?.toDouble() ?: 0.0
                                            val oldAvg = (existingHolding["avg"] as? Number)?.toDouble() ?: 0.0

                                            val combinedQty = oldQty + qty
                                            val combinedTotalAmount = (oldQty * oldAvg) + (qty * price)
                                            val newCalculatedAvg = if (combinedQty > 0) combinedTotalAmount / combinedQty else 0.0

                                            val existingHistoryMap = (existingHolding["history"] as? Map<String, Any>)?.toMutableMap() ?: mutableMapOf()
                                            val maxHistoryKey = existingHistoryMap.keys.mapNotNull { it.toIntOrNull() }.maxOrNull() ?: 0
                                            val newHistoryKey = String.format(Locale.US, "%03d", maxHistoryKey + 1)
                                            existingHistoryMap[newHistoryKey] = newHistoryEntry

                                            val updatedInvestment = mapOf(
                                                "name" to selectedSymbol,
                                                "type" to assetType,
                                                "qnt" to combinedQty,
                                                "avg" to newCalculatedAvg,
                                                "amnt" to combinedTotalAmount,
                                                "history" to existingHistoryMap
                                            )

                                            userRef.update("invest.$existingKey", updatedInvestment).await()
                                        } else {
                                            val maxKey = rawInvestMap.keys.mapNotNull { it.toIntOrNull() }.maxOrNull() ?: 0
                                            val newKey = String.format(Locale.US, "%03d", maxKey + 1)
                                            
                                            val initialHistoryMap = mapOf("001" to newHistoryEntry)

                                            val newInvestment = mapOf(
                                                "name" to selectedSymbol, 
                                                "type" to assetType,
                                                "qnt" to qty,
                                                "avg" to price,
                                                "amnt" to (qty * price),
                                                "history" to initialHistoryMap
                                            )
                                            
                                            userRef.update("invest.$newKey", newInvestment).await()
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                                MarketEngine.registerTickerInSheet(sheetTicker, assetName, assetType)
                            }
                        } else {
                            Toast.makeText(context, "Please select an Asset and enter valid Details", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .bounceClick(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Investment", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
