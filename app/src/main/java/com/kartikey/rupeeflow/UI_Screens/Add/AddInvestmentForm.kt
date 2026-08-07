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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInvestmentForm(username: String, onInvestmentAdded: () -> Unit, onDismiss: () -> Unit) { 
    var assetType by remember { mutableStateOf("Stock") }
    var assetName by remember { mutableStateOf("") }
    var selectedSymbol by remember { mutableStateOf("") } 
    var quantity by remember { mutableStateOf("") }
    var buyPrice by remember { mutableStateOf("") }
    
    var typeExpanded by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }
    
    var searchResults by remember { mutableStateOf<List<SearchRow>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(assetName) {
        if (assetName.isBlank() || selectedSymbol.isNotEmpty()) {
            searchResults = emptyList()
            searchExpanded = false
            return@LaunchedEffect
        }
        
        delay(500) 
        isSearching = true
        
        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://query2.finance.yahoo.com/v1/finance/search?q=${assetName.replace(" ", "%20")}&quotesCount=30&newsCount=0")
                    .get()
                    .build()
                    
                val response = client.newCall(request).execute()
                val responseData = response.body?.string() ?: ""
                
                if (response.isSuccessful && responseData.isNotEmpty()) {
                    val jsonResponse = JSONObject(responseData)
                    val quotes = jsonResponse.optJSONArray("quotes")
                    
                    val indianList = mutableListOf<SearchRow>()
                    val globalList = mutableListOf<SearchRow>()
                    
                    if (quotes != null) {
                        for (i in 0 until quotes.length()) {
                            val quote = quotes.getJSONObject(i)
                            val sym = quote.optString("symbol", "")
                            if (sym.isEmpty()) continue
                            
                            var cleanName = quote.optString("longname", "").ifBlank {
                                quote.optString("shortname", "").ifBlank { "" }
                            }
                            
                            val isIndian = sym.endsWith(".NS") || sym.endsWith(".BO")
                            
                            if (cleanName.isBlank() || cleanName == sym) {
                                cleanName = if (isIndian) {
                                    sym.replace(".NS", "").replace(".BO", "") + " Asset"
                                } else {
                                    sym
                                }
                            }
                            
                            val displaySymbol = when {
                                sym.endsWith(".NS") -> sym.replace(".NS", "") + " (NSE)"
                                sym.endsWith(".BO") -> sym.replace(".BO", "") + " (BSE)"
                                else -> sym
                            }
                            
                            val row = SearchRow(
                                name = cleanName,
                                rawSymbol = sym,
                                displaySymbol = displaySymbol,
                                isIndian = isIndian
                            )
                            
                            if (isIndian) {
                                indianList.add(row)
                            } else {
                                globalList.add(row)
                            }
                        }
                    }
                    
                    val orderedResults = indianList + globalList
                    
                    withContext(Dispatchers.Main) {
                        searchResults = orderedResults
                        isSearching = false
                        if (orderedResults.isNotEmpty()) {
                            searchExpanded = true
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
            
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = !typeExpanded }
            ) {
                OutlinedTextField(
                    value = assetType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Asset Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
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
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

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
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        searchResults.forEach { row ->
                            DropdownMenuItem(
                                text = { 
                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                        Text(row.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text(row.displaySymbol, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                            if (row.isIndian) {
                                                Text("🇮🇳 India", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    val prefix = if (row.rawSymbol.endsWith(".NS")) "NSE:" else if (row.rawSymbol.endsWith(".BO")) "BOM:" else ""
                                    val cleanSymbol = prefix + row.rawSymbol.replace(".NS", "").replace(".BO", "")
                                    
                                    assetName = row.name 
                                    selectedSymbol = cleanSymbol
                                    searchExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = quantity, onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                )
                OutlinedTextField(
                    value = buyPrice, onValueChange = { buyPrice = it },
                    label = { Text("Buy Price") },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
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
                            // 1. EXACT "invest" map root-level implementation
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

                            // 2. Silent Ping to Google Sheet for Auto-Append
                            try {
                                val client = OkHttpClient()
                                val jsonPayload = JSONObject().apply {
                                    put("action", "addTicker")
                                    put("ticker", selectedSymbol)
                                    put("name", assetName)
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
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

data class SearchRow(
    val name: String,
    val rawSymbol: String,
    val displaySymbol: String,
    val isIndian: Boolean
)
