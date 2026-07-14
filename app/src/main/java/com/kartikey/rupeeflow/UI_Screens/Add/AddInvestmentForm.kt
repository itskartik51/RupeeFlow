package com.kartikey.rupeeflow.UI_Screens.Add

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.Cloud_Database.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
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
    var date by remember { mutableStateOf("") } 
    
    var typeExpanded by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }
    
    var searchResults by remember { mutableStateOf<List<SearchRow>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    var isSubmitting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var isPressed by remember { mutableStateOf(false) }
    val buttonScale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "ButtonScale")

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
        modifier = Modifier.fillMaxWidth(), // FIX: Height restriction removed
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    listOf("Stock", "Mutual Fund", "ETF", "Bond").forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
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
                    trailingIcon = { 
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF2E7D32))
                        } else {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = searchExpanded) 
                        }
                    }
                )
                
                if (assetName.isNotEmpty() && searchResults.isNotEmpty() && selectedSymbol.isEmpty()) {
                    ExposedDropdownMenu(
                        expanded = searchExpanded,
                        onDismissRequest = { searchExpanded = false }
                    ) {
                        searchResults.forEach { row ->
                            DropdownMenuItem(
                                text = { 
                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                        Text(row.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text(row.displaySymbol, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                            if (row.isIndian) {
                                                Text("🇮🇳 India", fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    val cleanSymbol = row.rawSymbol.replace(".NS", "").replace(".BO", "")
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
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = buyPrice, onValueChange = { buyPrice = it },
                    label = { Text("Buy Price") },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = Color.Black) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = date, onValueChange = { date = it },
                label = { Text("Date (Optional)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val qty = quantity.toDoubleOrNull() ?: 0.0
                    val price = buyPrice.toDoubleOrNull() ?: 0.0
                    
                    if (selectedSymbol.isNotBlank() && qty > 0 && price > 0) {
                        isSubmitting = true
                        onInvestmentAdded()
                        
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val jsonBody = JSONObject().apply {
                                    put("action", "add_investment")
                                    put("username", username)
                                    put("inv_date", date)
                                    put("asset_name", selectedSymbol) 
                                    put("asset_type", assetType)
                                    put("quantity", qty)
                                    put("buy_price", price)
                                    put("broker", "")
                                    put("notes", "")
                                }
                                val client = OkHttpClient()
                                val body = jsonBody.toString().toRequestBody("application/json".toMediaType())
                                val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(body).build()
                                client.newCall(request).execute()

                                withContext(Dispatchers.Main) {
                                    isSubmitting = false
                                    Toast.makeText(context, "Investment Saved! Sheet will calculate live returns.", Toast.LENGTH_SHORT).show()
                                    assetName = ""; selectedSymbol = ""; quantity = ""; buyPrice = ""; date = "" 
                                    onDismiss() 
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isSubmitting = false
                                    Toast.makeText(context, "Error saving investment", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(context, "Please select an Asset from the search list, and enter valid Quantity & Price", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .scale(buttonScale)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                            }
                        )
                    },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Save Investment", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}

data class SearchRow(
    val name: String,
    val rawSymbol: String,
    val displaySymbol: String,
    val isIndian: Boolean
)
