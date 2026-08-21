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

    // ⚡ UNBREAKABLE YAHOO SPARK ENGINE ⚡
    LaunchedEffect(assetName, assetType) {
        if (assetName.isBlank() || selectedSymbol.isNotEmpty() || assetType.isEmpty()) {
            searchResults = emptyList()
            return@LaunchedEffect
        }
        
        delay(500) // Smart Debounce
        isSearching = true
        
        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                
                // 1. Strict Category Filters based on Selection
                val validQuoteTypes = when (assetType) {
                    "Mutual Fund" -> listOf("MUTUALFUND")
                    "ETF" -> listOf("ETF")
                    "Bond" -> listOf("EQUITY", "ETF")
                    else -> listOf("EQUITY") // Strictly Stocks
                }

                // Call 1: Yahoo Search API
                val searchUrl = "https://query2.finance.yahoo.com/v1/finance/search?q=${assetName.replace(" ", "%20")}&quotesCount=25"
                val searchReq = Request.Builder().url(searchUrl).header("User-Agent", userAgent).get().build()
                val searchResp = client.newCall(searchReq).execute()
                val searchData = searchResp.body?.string() ?: ""

                if (searchResp.isSuccessful && searchData.isNotEmpty()) {
                    val jsonResponse = JSONObject(searchData)
                    val quotes = jsonResponse.optJSONArray("quotes")
                    
                    val orderedSymbols = mutableListOf<String>()
                    val fallbackNames = mutableMapOf<String, String>()

                    if (quotes != null) {
                        for (i in 0 until quotes.length()) {
                            val quote = quotes.getJSONObject(i)
                            val sym = quote.optString("symbol", "")
                            val qType = quote.optString("quoteType", "")
                            
                            // ⚡ IRON-CLAD FILTERS: Only .NS/.BO, Must Match Type, NO '0P' Garbage ⚡
                            if (sym.isNotEmpty() && (sym.endsWith(".NS") || sym.endsWith(".BO"))) {
                                if (validQuoteTypes.contains(qType)) {
                                    
                                    // Block BSE Mutual Fund Garbage from appearing in Stocks
                                    if (assetType == "Stock" && sym.startsWith("0P")) continue
                                    
                                    if (!orderedSymbols.contains(sym)) {
                                        orderedSymbols.add(sym)
                                        fallbackNames[sym] = quote.optString("longname", quote.optString("shortname", ""))
                                    }
                                }
                            }
                        }
                    }

                    // Call 2: Fetch Live Price via Secret Spark API
                    if (orderedSymbols.isNotEmpty()) {
                        val symbolsParam = orderedSymbols.take(15).joinToString(",")
                        val sparkUrl = "https://query1.finance.yahoo.com/v7/finance/spark?symbols=$symbolsParam"
                        val sparkReq = Request.Builder().url(sparkUrl).header("User-Agent", userAgent).get().build()
                        val sparkResp = client.newCall(sparkReq).execute()
                        val sparkData = sparkResp.body?.string() ?: ""
                        
                        val priceMap = mutableMapOf<String, Double>()
                        
                        if (sparkResp.isSuccessful && sparkData.isNotEmpty()) {
                            val sparkJson = JSONObject(sparkData)
                            val resultArr = sparkJson.optJSONObject("spark")?.optJSONArray("result")
                            
                            if (resultArr != null) {
                                for (i in 0 until resultArr.length()) {
                                    val item = resultArr.getJSONObject(i)
                                    val sym = item.optString("symbol", "")
                                    val meta = item.optJSONArray("response")?.optJSONObject(0)?.optJSONObject("meta")
                                    val price = meta?.optDouble("regularMarketPrice", 0.0) ?: 0.0
                                    priceMap[sym] = price
                                }
                            }
                        }

                        // Assemble Final Clean List (Preserving Yahoo's Relevance/Market Cap Sort Order)
                        val parsedList = mutableListOf<SearchRow>()
                        for (sym in orderedSymbols.take(15)) {
                            val price = priceMap[sym] ?: 0.0
                            val name = fallbackNames[sym] ?: ""
                            val cleanSym = sym.replace(".NS", "").replace(".BO", "")
                            
                            parsedList.add(SearchRow(cleanSymbol = cleanSym, rawSymbol = sym, fullName = name, livePrice = price))
                        }

                        withContext(Dispatchers.Main) {
                            searchResults = parsedList
                            isSearching = false
                        }
                    } else {
                        withContext(Dispatchers.Main) { 
                            searchResults = emptyList()
                            isSearching = false 
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) { isSearching = false }
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
                                        
                                        // ⚡ Dynamic Name Truncation Logic ⚡
                                        if (row.fullName.isNotBlank() && !row.fullName.equals(row.cleanSymbol, ignoreCase = true)) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            val cleanBracketName = if (row.fullName.length > 22) "${row.fullName.take(20)}..." else row.fullName
                                            Text(text = "($cleanBracketName)", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    
                                    // ⚡ Live Price formatting ⚡
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
                                if (it.all { char -> char.isDigit() }) quantity = it // Strictly Integers
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
