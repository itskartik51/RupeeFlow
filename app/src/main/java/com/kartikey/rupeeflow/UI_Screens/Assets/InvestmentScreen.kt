package com.kartikey.rupeeflow.UI_Screens.Assets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Ye Dummy Data class hai (Google Sheet ke I2 se U2 columns jaisi)
data class InvestmentItem(
    val assetName: String,
    val assetType: String,
    val quantity: Double,
    val avgBuyPrice: Double,
    val broker: String,
    val currentPrice: Double // Abhi dummy live price ke liye
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentScreen(onBackClick: () -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }

    // Dummy data (Jab API banegi to ye data Google sheet se aayega)
    val investmentList = remember {
        mutableStateListOf(
            InvestmentItem("SBIN", "Stock", 36.0, 900.0, "Zerodha", 1080.0),
            InvestmentItem("Jio BlackRock Flexi Cap", "Mutual Fund", 150.0, 100.0, "Groww", 112.5)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Investments", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF5F5F5))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF2E7D32),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Investment")
            }
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        
        // Investment List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            items(investmentList) { item ->
                InvestmentCard(item)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Add Investment Form (Dialog)
        if (showAddDialog) {
            AddInvestmentDialog(
                onDismiss = { showAddDialog = false },
                onSave = { newItem ->
                    investmentList.add(newItem)
                    showAddDialog = false
                    // TODO: Yahan API call lagayenge Google Sheet me save karne ke liye
                }
            )
        }
    }
}

@Composable
fun InvestmentCard(item: InvestmentItem) {
    val investedValue = item.quantity * item.avgBuyPrice
    val currentValue = item.quantity * item.currentPrice
    val totalReturn = currentValue - investedValue
    val returnColor = if (totalReturn >= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.assetName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(item.assetType, color = Color.Gray, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Invested (₹)", color = Color.Gray, fontSize = 12.sp)
                    Text("₹${investedValue.toInt()}", fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Current (₹)", color = Color.Gray, fontSize = 12.sp)
                    Text("₹${currentValue.toInt()}", fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Qty: ${item.quantity} | Avg: ₹${item.avgBuyPrice}", color = Color.Gray, fontSize = 12.sp)
                Text(
                    text = "${if (totalReturn >= 0) "+" else ""}₹${totalReturn.toInt()}",
                    color = returnColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun AddInvestmentDialog(onDismiss: () -> Unit, onSave: (InvestmentItem) -> Unit) {
    var assetName by remember { mutableStateOf("") }
    var assetType by remember { mutableStateOf("Stock") }
    var quantity by remember { mutableStateOf("") }
    var buyPrice by remember { mutableStateOf("") }
    var broker by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Investment", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(value = assetName, onValueChange = { assetName = it }, label = { Text("Asset Name (e.g. SBIN)") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = assetType, onValueChange = { assetType = it }, label = { Text("Type (Stock/MF/ETF)") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Qty") }, modifier = Modifier.weight(1f), singleLine = true)
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(value = buyPrice, onValueChange = { buyPrice = it }, label = { Text("Avg Price") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = broker, onValueChange = { broker = it }, label = { Text("Broker (e.g. Zerodha)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantity.toDoubleOrNull() ?: 0.0
                    val price = buyPrice.toDoubleOrNull() ?: 0.0
                    if (assetName.isNotBlank() && qty > 0 && price > 0) {
                        onSave(InvestmentItem(assetName, assetType, qty, price, broker, price)) // Default current price = buy price
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
}
