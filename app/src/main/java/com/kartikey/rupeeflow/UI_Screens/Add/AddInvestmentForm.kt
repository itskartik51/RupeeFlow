package com.kartikey.rupeeflow.UI_Screens.Add

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
    var expanded by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f)

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(value = assetType, onValueChange = {}, readOnly = true, label = { Text("Asset Type") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp))
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("Stock", "Mutual Fund", "ETF", "Bond").forEach { selectionOption -> DropdownMenuItem(text = { Text(selectionOption) }, onClick = { assetType = selectionOption; expanded = false }) }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = assetName, onValueChange = { assetName = it }, label = { Text("Asset Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Qty") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = buyPrice, onValueChange = { buyPrice = it }, label = { Text("Buy Price") }, prefix = { Text("₹ ") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (assetName.isNotBlank()) {
                        isSubmitting = true
                        onInvestmentAdded() // PHASE 3: OPTIMISTIC UPDATE
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val json = JSONObject().apply { put("action", "add_investment"); put("username", username); put("asset_name", assetName); put("quantity", quantity); put("buy_price", buyPrice) }
                                val client = OkHttpClient(); val body = json.toString().toRequestBody("application/json".toMediaType()); val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(body).build()
                                client.newCall(request).execute()
                                withContext(Dispatchers.Main) { isSubmitting = false; assetName = ""; quantity = ""; buyPrice = "" }
                            } catch (e: Exception) { withContext(Dispatchers.Main) { isSubmitting = false } }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp).scale(scale).pointerInput(Unit) { detectTapGestures(onPress = { pressed = true; tryAwaitRelease(); pressed = false }) },
                shape = RoundedCornerShape(12.dp), enabled = !isSubmitting
            ) { Text("Add Investment", fontWeight = FontWeight.Bold) }
        }
    }
}
