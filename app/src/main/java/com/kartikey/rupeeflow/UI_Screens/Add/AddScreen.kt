package com.kartikey.rupeeflow.UI_Screens.Add

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.UI_Screens.AddExpense.TransactionModel

@Composable
fun AddScreen(
    paddingValues: PaddingValues, 
    username: String, 
    onExpenseAdded: (TransactionModel) -> Unit, 
    onInvestmentAdded: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Add Expense", "Add Investment")

    Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
        Text("Add New Entry", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color(0xFF2E7D32))
        Spacer(modifier = Modifier.height(16.dp))

        TabRow(selectedTabIndex = selectedTabIndex, containerColor = Color.Transparent, indicator = { tabPositions -> TabRowDefaults.Indicator(Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]), color = Color(0xFF2E7D32), height = 3.dp) }) {
            tabs.forEachIndexed { index, title -> Tab(selected = selectedTabIndex == index, onClick = { selectedTabIndex = index }, text = { Text(title, fontWeight = FontWeight.Bold) }, selectedContentColor = Color(0xFF2E7D32), unselectedContentColor = Color.Gray) }
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (selectedTabIndex) {
            0 -> AddExpenseForm(username = username, onExpenseAdded = onExpenseAdded)
            1 -> AddInvestmentForm(username = username, onInvestmentAdded = onInvestmentAdded)
        }
    }
}
