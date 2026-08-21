package com.kartikey.rupeeflow.UI_Screens.Add

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
    var searchExpanded by remember { mutableStateOf(false) }
    
    var searchResults by remember { mutableStateOf<List<SearchRow>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // ⚡ Advanced Dual-API Search Engine ⚡
    LaunchedEffect(assetName, assetType) {
        if (assetName.isBlank() || selectedSymbol.isNotEmpty() || assetType.isEmpty()) {
            searchResults = emptyList()
            searchExpanded = false
            return@LaunchedEffect
        }
        
        delay(600) // Debounce
        isSearching = true
        
        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                
                // --- Target Quote Type Filter ---
                val validQuoteTypes = when (assetType) {
                    "Mutual Fund" -> listOf("MUTUALFUND")
                    "ETF" -> listOf("ETF")
                    "Bond" -> listOf("EQUITY", "ETF") // SGBs trade as equities usually
                    else -> listOf("EQUITY") // Stocks
                }

                // 1. First Call: Search API
                val searchRequest = Request.Builder()
                    .url("https://query2.finance.yahoo.com/v1/finance/search?q=${assetName.replace(" ", "%20")}&quotesCount=25")
                    .get()
                    .build()
                    
                val searchResponse = client.newCall(searchRequest).execute()
                val searchData = searchResponse.body?.string() ?: ""
                
                if (searchResponse.isSuccessful && searchData.isNotEmpty()) {
                    val jsonResponse = JSONObject(searchData)
                    val quotes = jsonResponse.optJSONArray("quotes")
                    
                    val symbolsToFetch = mutableListOf<String>()
                    val fallbackNames = mutableMapOf<String, String>()
                    
                    if (quotes != null) {
                        for (i in 0 until quotes.length()) {
                            val quote = quotes.getJSONObject(i)
                            val sym = quote.optString("symbol", "")
                            val qType = quote.optString("quoteType", "")
                            
                            // ⚡ Filter: Only Indian Assets & Strict Category Match ⚡
                            if (sym.isNotEmpty() && (sym.endsWith(".NS") || sym.endsWith(".BO"))) {
                                if (validQuoteTypes.contains(qType)) {
                                    symbolsToFetch.add(sym)
                                    fallbackNames[sym] = quote.optString("longname", quote.optString("shortname", ""))
                                }
                            }
                        }
                    }
                    
                    // 2. Second Call: Quote API for Live Price & Market Cap Sorting
                    if (symbolsToFetch.isNotEmpty()) {
                        val symbolsParam = symbolsToFetch.take(15).joinToString(",") // Limit to top 15 matches
                        val quoteRequest = Request.Builder()
                            .url("https://query2.finance.yahoo.com/v7/finance/quote?symbols=$symbolsParam")
                            .get()
                            .build()
                            
                        val quoteResponse = client.newCall(quoteRequest).execute()
                        val quoteData = quoteResponse.body?.string() ?: ""
                        
                        if (quoteResponse.isSuccessful && quoteData.isNotEmpty()) {
                            val quoteJson = JSONObject(quoteData)
                            val resultArr = quoteJson.optJSONObject("quoteResponse")?.optJSONArray("result")
                            
                            val parsedResults = mutableListOf<SearchRow>()
                            
                            if (resultArr != null) {
                                for (i in 0 until resultArr.length()) {
                                    val item = resultArr.getJSONObject(i)
                                    val rawSym = item.optString("symbol", "")
                                    val marketCap = item.optLong("marketCap", 0L)
                                    val livePrice = item.optDouble("regularMarketPrice", 0.0)
                                    
                                    val fullName = item.optString("longName", item.optString("shortName", fallbackNames[rawSym] ?: ""))
                                    val cleanSymbol = rawSym.replace(".NS", "").replace(".BO", "")
                                    
                                    if (rawSym.isNotEmpty() && livePrice > 0.0) {
                                        parsedResults.add(
                                            SearchRow(
                                                cleanSymbol = cleanSymbol,
                                                rawSymbol = rawSym,
                                                fullName = fullName,
                                                livePrice = livePrice,
                                                marketCap = marketCap
                                            )
                                        )
                                    }
                                }
                            }
                            
                            // ⚡ Sort: Highest Market Cap to Lowest ⚡
                            parsedResults.sortByDescending { it.marketCap }
                            
                            withContext(Dispatchers.Main) {
                                searchResults = parsedResults
                                isSearching = false
                                if (parsedResults.isNotEmpty()) {
                                    searchExpanded = true
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) { isSearching = false }
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
                withContext(Dispatchers.Main) { isSearching = false }
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
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            // ⚡ Conditional UI (Only visible when Type is selected) ⚡
            if (assetType.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = searchExpanded,
                    onExpandedChange = { 
                        if (searchResults.isNotEmpty() && assetName.isNotEmpty()) {
                            searchExpanded = it 
                        }
                    }
                ) {
                    OutlinedTextField(
                        value = assetName,
                        onValueChange = { 
                            assetName = it 
                            selectedSymbol = "" 
                            sheetTicker = ""
                            searchExpanded = it.isNotEmpty()
                        },
                        label = { Text("Search $assetType Name") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface),
                        trailingIcon = { 
                            if (isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                            } else {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = searchExpanded) 
                            }
                        }
                    )
                    
                    if (assetName.isNotEmpty() && searchResults.isNotEmpty() && selectedSymbol.isEmpty()) {
                        ExposedDropdownMenu(
                            expanded = searchExpanded,
                            onDismissRequest = { searchExpanded = false },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface)
                                .heightIn(max = 260.dp) // ⚡ Strict Height to prevent Keyboard overlap
                        ) {
                            searchResults.forEach { row ->
                                DropdownMenuItem(
                                    text = { 
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // --- Left Side: Symbol + Truncated Name ---
                                            Column(modifier = Modifier.weight(0.65f)) {
                                                Text(text = row.cleanSymbol, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                                
                                                if (row.fullName.isNotBlank() && !row.fullName.equals(row.cleanSymbol, ignoreCase = true)) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    // Truncate logic to max 22 chars
                                                    val cleanBracketName = if (row.fullName.length > 22) "${row.fullName.take(20)}..." else row.fullName
                                                    Text(text = "($cleanBracketName)", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                            
                                            // --- Right Side: Live Price ---
                                            Text(
                                                text = "₹ ${String.format(Locale.US, "%.2f", row.livePrice)}",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.weight(0.35f),
                                                textAlign = TextAlign.End
                                            )
                                        }
                                    },
                                    onClick = {
                                        val prefix = if (row.rawSymbol.endsWith(".NS")) "NSE:" else if (row.rawSymbol.endsWith(".BO")) "BOM:" else ""
                                        
                                        assetName = row.cleanSymbol 
                                        selectedSymbol = row.cleanSymbol
                                        sheetTicker = prefix + row.cleanSymbol
                                        buyPrice = String.format(Locale.US, "%.2f", row.livePrice) // Auto-fill price
                                        
                                        searchExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ⚡ Dynamic Labels & Math Physics depending on Asset Type ⚡
                val isMutualFund = assetType == "Mutual Fund"
                val qtyLabel = if (isMutualFund) "Total Units" else "Quantity"
                val priceLabel = if (isMutualFund) "Average NAV" else "Buy Price"

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = quantity, 
                        onValueChange = { 
                            if (isMutualFund) {
                                // Mutual Funds: Allow Decimals
                                quantity = it
                            } else {
                                // Stocks/ETFs/Bonds: Strictly Whole Numbers (No dot allowed)
                                if (it.all { char -> char.isDigit() }) {
                                    quantity = it
                                }
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

                // Auto-calculate logic preview
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

                Button(
                    onClick = {
                        val qty = quantity.toDoubleOrNull() ?: 0.0
                        val price = buyPrice.toDoubleOrNull() ?: 0.0
                        
                        if (selectedSymbol.isNotBlank() && qty > 0 && price > 0) {
                            
                            onInvestmentAdded()
                            onDismiss() 
                            
                            CoroutineScope(Dispatchers.IO).launch {
                                // Structuring clean data into the 'invest' Map
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
                                            "name" to selectedSymbol, // Stores clean name (e.g. SBIN)
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

                                // Sheet Auto-append ping logic
                                try {
                                    val client = OkHttpClient()
                                    val jsonPayload = JSONObject().apply {
                                        put("action", "addTicker")
                                        put("ticker", sheetTicker) // Sends Full name (e.g. NSE:SBIN)
                                        put("name", assetName)
                                        put("type", assetType) // ⚡ Appended for Google Sheets Parsing
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
    val livePrice: Double,
    val marketCap: Long
)
