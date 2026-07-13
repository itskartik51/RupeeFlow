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
    var quantity by remember { mutableStateOf("") }
    var buyPrice by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") } 
    
    var typeExpanded by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }

    var isSubmitting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var isPressed by remember { mutableStateOf(false) }
    val buttonScale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "ButtonScale")

    // Note: Jab hum Live API lagayenge to ye dummy list hat jayegi aur data network se aayega
    val assetDatabase = mapOf(
        "Stock" to listOf("SBIN", "RELIANCE", "TCS", "HDFCBANK", "INFY", "ITC", "TATAMOTORS", "ZOMATO", "WIPRO", "HINDUNILVR"),
        "ETF" to listOf("NIFTYBEES", "BANKBEES", "GOLDBEES", "ITBEES", "LIQUIDBEES", "MON100"),
        "Mutual Fund" to listOf("Parag Parikh Flexi Cap", "Quant Small Cap", "SBI Contra", "HDFC Mid-Cap", "Nippon India Small Cap"),
        "Bond" to listOf("SGB", "NHAI Bond", "RBI Floating Rate Bond", "REC Bond")
    )
    val currentSuggestions = assetDatabase[assetType] ?: emptyList()

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
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. ASSET NAME SEARCH (Hide until typed logic)
            ExposedDropdownMenuBox(
                expanded = searchExpanded,
                onExpandedChange = { 
                    // UPDATE: Sirf tabhi khulega jab textbox me kuch type kiya ho
                    if (assetName.isNotEmpty()) {
                        searchExpanded = it 
                    }
                }
            ) {
                OutlinedTextField(
                    value = assetName,
                    onValueChange = { 
                        assetName = it 
                        // UPDATE: Type karte hi list khulegi, mita dete hi band ho jayegi
                        searchExpanded = it.isNotEmpty() 
                    },
                    label = { Text("Search $assetType Name") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = searchExpanded) }
                )
                
                val filteredOptions = currentSuggestions.filter { it.contains(assetName, ignoreCase = true) }
                
                // UPDATE: Jab filter hoke options aayein aur textbox khali na ho, tabhi menu dikhana hai
                if (assetName.isNotEmpty() && filteredOptions.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = searchExpanded,
                        onDismissRequest = { searchExpanded = false }
                    ) {
                        filteredOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    assetName = option
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
                    
                    if (assetName.isNotBlank() && qty > 0 && price > 0) {
                        isSubmitting = true
                        onInvestmentAdded()
                        
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val jsonBody = JSONObject().apply {
                                    put("action", "add_investment")
                                    put("username", username)
                                    put("inv_date", date)
                                    put("asset_name", assetName) 
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
                                    assetName = ""; quantity = ""; buyPrice = ""; date = "" 
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isSubmitting = false
                                    Toast.makeText(context, "Error saving investment", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(context, "Please enter Asset Name, Quantity & Price correctly", Toast.LENGTH_LONG).show()
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
