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
import com.google.firebase.firestore.FirebaseFirestore
import com.kartikey.rupeeflow.Cloud_Database.Constants
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
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
    
    var typeExpanded by remember { mutableStateOf(false) }
    
    var searchResults by remember { mutableStateOf<List<SearchRow>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // ⚡ Robust Search Engine (Groww API Primary + Multi-key Fallback) ⚡
    LaunchedEffect(assetName, assetType) {
        if (assetName.isBlank() || selectedSymbol.isNotEmpty() || assetType.isEmpty()) {
            searchResults = emptyList()
            return@LaunchedEffect
        }
        
        delay(400) // Fast debounce
        isSearching = true
        
        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                
                // Target category mapping
                val targetTypes = when (assetType) {
                    "Mutual Fund" -> listOf("MF", "MUTUAL_FUND", "MUTUALFUND")
                    "ETF" -> listOf("ETF")
                    "Bond" -> listOf("BOND", "SGB", "STOCK", "EQUITY")
                    else -> listOf("STOCK", "EQUITY")
                }

                // 1. Primary Attempt: Groww Native Entity Search
                val growwUrl = "https://groww.in/v1/api/search/v1/derived/entity?app=false&entityType=ALL&page=0&query=${assetName.replace(" ", "%20")}&size=20"
                val growwRequest = Request.Builder()
                    .url(growwUrl)
                    .header("User-Agent", userAgent)
                    .header("Accept", "application/json")
                    .get()
                    .build()
                    
                val response = client.newCall(growwRequest).execute()
                val responseData = response.body?.string() ?: ""
                val parsedList = mutableListOf<SearchRow>()

                if (response.isSuccessful && responseData.isNotEmpty()) {
                    val jsonResponse = JSONObject(responseData)
                    val contentArr = jsonResponse.optJSONArray("content") 
                        ?: jsonResponse.optJSONObject("data")?.optJSONArray("content")
                        ?: jsonResponse.optJSONArray("data")
                    
                    if (contentArr != null) {
                        for (i in 0 until contentArr.length()) {
                            val item = contentArr.getJSONObject(i)
                            val qType = item.optString("entity_type", item.optString("type", "")).uppercase(Locale.getDefault())
                            
                            if (targetTypes.any { qType.contains(it) } || qType.isEmpty()) {
                                val rawSym = item.optString("search_id", item.optString("nse_scrip_code", item.optString("bse_scrip_code", "")))
                                val fullName = item.optString("title", item.optString("scheme_name", item.optString("name", "")))
                                val livePrice = item.optDouble("live_price", item.optDouble("nav", item.optDouble("current_price", 0.0)))
                                
                                if (rawSym.isNotBlank() || fullName.isNotBlank()) {
                                    val cleanSymbol = rawSym.replace(".NS", "").replace(".BO", "").ifEmpty { fullName.take(10).uppercase() }
                                    parsedList.add(
                                        SearchRow(
                                            cleanSymbol = cleanSymbol,
                                            rawSymbol = rawSym.ifEmpty { cleanSymbol },
                                            fullName = fullName,
                                            livePrice = livePrice
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Secondary Fallback: If primary list is empty, hit Yahoo Search
                if (parsedList.isEmpty()) {
                    val yahooUrl = "https://query2.finance.yahoo.com/v1/finance/search?q=${assetName.replace(" ", "%20")}&quotesCount=15"
                    val yahooReq = Request.Builder()
                        .url(yahooUrl)
                        .header("User-Agent", userAgent)
                        .get()
                        .build()
                    val yResp = client.newCall(yahooReq).execute()
                    val yData = yResp.body?.string() ?: ""
                    
                    if (yResp.isSuccessful && yData.isNotEmpty()) {
                        val yQuotes = JSONObject(yData).optJSONArray("quotes")
                        if (yQuotes != null) {
                            for (i in 0 until yQuotes.length()) {
                                val q = yQuotes.getJSONObject(i)
                                val sym = q.optString("symbol", "")
                                if (sym.endsWith(".NS") || sym.endsWith(".BO")) {
                                    val name = q.optString("longname", q.optString("shortname", ""))
                                    val clean = sym.replace(".NS", "").replace(".BO", "")
                                    parsedList.add(SearchRow(cleanSymbol = clean, rawSymbol = sym, fullName = name, livePrice = 0.0))
                                }
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    searchResults = parsedList
                    isSearching = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { 
                    searchResults = emptyList()
                    isSearching = false 
                }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(), 
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
            
            // --- ASSET TYPE DROPDOWN ---
            Text(text = "Choose Investment Type", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = !typeExpanded }
            ) {
                OutlinedTextField(
                    value = if (assetType.isEmpty()) "Select Asset Type" else assetType,
                    onValueChange = {},
                    readOnly = true,
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
                            text = { Text(selectionOption, color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                assetType = selectionOption
                                assetName = "" 
                                selectedSymbol = ""
                                sheetTicker = ""
                                quantity = ""
                                buyPrice = ""
                                searchResults = emptyList()
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            // ⚡ Conditional Form UI ⚡
            if (assetType.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                // --- SEARCH BAR ---
                OutlinedTextField(
                    value = assetName,
                    onValueChange = { 
                        assetName = it 
                        selectedSymbol = "" 
                        sheetTicker = ""
                    },
                    label = { Text("Search $assetType Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
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

                // ⚡ INLINE SEARCH SUGGESTIONS (Zero Keyboard Overlap Bug) ⚡
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
                                            assetName = row.cleanSymbol 
                                            selectedSymbol = row.cleanSymbol
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
                                        Text(text = row.cleanSymbol, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                        
                                        if (row.fullName.isNotBlank() && !row.fullName.equals(row.cleanSymbol, ignoreCase = true)) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            val cleanBracketName = if (row.fullName.length > 24) "${row.fullName.take(22)}..." else row.fullName
                                            Text(text = "($cleanBracketName)", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    
                                    Text(
                                        text = if (row.livePrice > 0.0) "₹ ${String.format(Locale.US, "%.2f", row.livePrice)}" else "-",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.5.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(0.3f),
                                        textAlign = TextAlign.End
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

                // --- QUANTITY & BUY PRICE INPUTS ---
                val isMutualFund = assetType == "Mutual Fund"
                val qtyLabel = if (isMutualFund) "Total Units" else "Quantity"
                val priceLabel = if (isMutualFund) "Average NAV" else "Buy Price"

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = quantity, 
                        onValueChange = { 
                            if (isMutualFund) {
                                quantity = it // Allows Decimals
                            } else {
                                if (it.all { char -> char.isDigit() }) quantity = it // Integers only
                            }
                        },
                        label = { Text(qtyLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = if (isMutualFund) KeyboardType.Decimal else KeyboardType.Number),
                        modifier = Modifier.weight(1f), singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                    )
                    OutlinedTextField(
                        value = buyPrice, onValueChange = { buyPrice = it },
                        label = { Text(priceLabel) },
                        prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
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
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- SAVE BUTTON ---
                Button(
                    onClick = {
                        val qty = quantity.toDoubleOrNull() ?: 0.0
                        val price = buyPrice.toDoubleOrNull() ?: 0.0
                        
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
                                        
                                        val investMap = userDoc.get("invest") as? Map<String, Any> ?: emptyMap()
                                        val maxKey = investMap.keys.mapNotNull { it.toIntOrNull() }.maxOrNull() ?: 0
                                        val newKey = String.format(Locale.US, "%03d", maxKey + 1)
                                        
                                        val newInvestment = mapOf(
                                            "name" to selectedSymbol, 
                                            "type" to assetType,
                                            "qnt" to qty,
                                            "avg" to price,
                                            "amnt" to (qty * price)
                                        )
                                        
                                        userRef.update("invest.$newKey", newInvestment).await()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                                try {
                                    val client = OkHttpClient()
                                    val jsonPayload = JSONObject().apply {
                                        put("action", "addTicker")
                                        put("ticker", sheetTicker) 
                                        put("name", assetName)
                                        put("type", assetType)
                                    }.toString()
                                    
                                    val requestBody = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                                    val request = Request.Builder()
                                        .url(Constants.GOOGLE_SHEET_API_URL)
                                        .post(requestBody)
                                        .build()
                                        
                                    client.newCall(request).execute()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
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
                    Text("Save Investment", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

data class SearchRow(
    val cleanSymbol: String,
    val rawSymbol: String,
    val fullName: String,
    val livePrice: Double
)
