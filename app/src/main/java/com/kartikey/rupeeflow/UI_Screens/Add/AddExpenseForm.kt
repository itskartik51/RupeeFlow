package com.kartikey.rupeeflow.UI_Screens.Add

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
fun AddExpenseForm(username: String) {
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    
    // Nayi chizein: Loading state, Coroutine scope aur Context (Toast ke liye)
    var isSubmitting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf("Food", "Transport", "Shopping", "Bills", "Others").forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                category = selectionOption
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.weight(1f), singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = remarks, onValueChange = { remarks = it },
                    label = { Text("Remarks") },
                    modifier = Modifier.weight(1f), singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amount, 
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    // Yahan ₹ ka sign permanently fix kar diya hai
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = Color.Black) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), 
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                
                Button(
                    onClick = {
                        if (amount.isNotBlank() && category.isNotBlank()) {
                            isSubmitting = true
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val json = JSONObject().apply {
                                        put("action", "add_expense")
                                        put("username", username)
                                        put("amount", amount)
                                        put("category", category)
                                        put("detail1", description)
                                        put("detail2", remarks)
                                    }
                                    val client = OkHttpClient()
                                    val body = json.toString().toRequestBody("application/json".toMediaType())
                                    val request = Request.Builder().url(Constants.GOOGLE_SHEET_API_URL).post(body).build()
                                    client.newCall(request).execute()

                                    withContext(Dispatchers.Main) {
                                        isSubmitting = false
                                        Toast.makeText(context, "Expense Added Successfully!", Toast.LENGTH_SHORT).show()
                                        // Form clear karne ke liye:
                                        amount = ""; description = ""; remarks = ""; category = ""
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isSubmitting = false
                                        Toast.makeText(context, "Failed to connect to sheet", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(context, "Please enter Amount and Category", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f).height(56.dp).padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Add", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                }
            }
        }
    }
}
