package com.kartikey.rupeeflow.UI_Screens.Add

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
fun AddInvestmentForm(username: String, onInvestmentAdded: () -> Unit) {
    var assetType by remember { mutableStateOf("Stock") }
    var assetName by remember { mutableStateOf("") }
    var selectedSymbol by remember { mutableStateOf("") } // API validation ke liye
    var quantity by remember { mutableStateOf("") }
    var buyPrice by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") } 
    
    var typeExpanded by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }
    
    // API Search States
    var searchResults by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    var isSubmitting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var isPressed by remember { mutableStateOf(false) }
    val buttonScale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "ButtonScale")

    // LIVE YAHOO FINANCE API SEARCH LOGIC
    LaunchedEffect(assetName) {
        if (assetName.isBlank() || assetName == selectedSymbol) {
            searchResults = emptyList()
            searchExpanded = false
            return@LaunchedEffect
        }
        
        // Debounce: Typing ke 500ms baad hi API call hit hogi taki network overload na ho
        delay(500) 
        isSearching = true
        
        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://query2.finance.yahoo.com/v1/finance/search?q=${assetName.replace(" ", "%20")}&quotesCount=10&newsCount=0")
                    .get()
                    .build()
                    
                val response = client.newCall(request).execute()
                val responseData = response.body?.string() ?: ""
                
                if (response.isSuccessful && responseData.isNotEmpty()) {
                    val jsonResponse = JSONObject(responseData)
                    val quotes = jsonResponse.optJSONArray("quotes")
                    val results = mutableListOf<Pair<String, String>>()
                    
                    if (quotes != null) {
                        for (i in 0 until quotes.length()) {
                            val quote = quotes.getJSONObject(i)
                            val sym = quote.optString("symbol", "")
                            val name = quote.optString("shortname", sym)
                            if (sym.isNotEmpty()) {
                                results.add(Pair(name, sym)) // Save Name & Symbol mapping
                            }
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        searchResults = results
                        isSearching = false
                        if (results.isNotEmpty()) {
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            
            // 1. ASSET TYPE SELECTOR
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

            // 2. LIVE ASSET NAME SEARCH (Hide until typed)
            ExposedDropdownMenuBox(
                expanded = searchExpanded,
                onExpandedChange = { 
                    if (searchResults.isNotEmpty()) {
                        searchExpanded = it 
                    }
                }
            ) {
                OutlinedTextField(
                    value = assetName,
                    onValueChange = { 
                        assetName = it 
                        selectedSymbol = "" // Naya type karte hi purana selection reset ho jayega
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
                
                if (searchResults.isNotEmpty() && selectedSymbol.isEmpty()) {
                    ExposedDropdownMenu(
                        expanded = searchExpanded,
                        onDismissRequest = { searchExpanded = false }
                    ) {
                        searchResults.forEach { (name, sym) ->
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(sym, fontSize = 12.sp, color = Color.Gray)
                                    }
                                },
                                onClick = {
                                    // GOOGLE FINANCE COMPATIBILITY FIX: Strip .NS and .BO
                                    val cleanSymbol = sym.replace(".NS", "").replace(".BO", "")
                                    assetName = cleanSymbol 
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
                    
                    // STRICT VALIDATION: User must select from the API list
                    if (selectedSymbol.isNotBlank() && qty > 0 && price > 0) {
                        isSubmitting = true
                        onInvestmentAdded()
                        
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val jsonBody = JSONObject().apply {
                                    put("action", "add_investment")
                                    put("username", username)
                                    put("inv_date", date)
                                    put("asset_name", selectedSymbol) // Cleaned symbol goes to sheet
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
                    Text("Add Investment", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}
