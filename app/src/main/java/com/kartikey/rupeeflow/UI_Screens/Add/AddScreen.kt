package com.kartikey.rupeeflow.UI_Screens.Add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AddScreen(
    paddingValues: PaddingValues, 
    username: String,
    onExpenseAdded: (TransactionModel) -> Unit,
    onInvestmentAdded: () -> Unit,
    onFinanceAdded: () -> Unit // Naya parameter
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    // Naya "Add Finance" Tab
    val tabs = listOf("Add Expense", "Add Investment", "Add Finance")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack, 
                contentDescription = "Back",
                tint = Color(0xFF2E7D32),
                modifier = Modifier
                    .size(26.dp)
                    .clickable { }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add", 
                fontWeight = FontWeight.Bold, 
                fontSize = 24.sp, 
                color = Color(0xFF2E7D32)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        ScrollableTabRow( // Changed to ScrollableTabRow for better fit
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = Color(0xFF2E7D32),
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = Color(0xFF2E7D32),
                    height = 3.dp
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    selectedContentColor = Color(0xFF2E7D32),
                    unselectedContentColor = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (selectedTabIndex) {
            0 -> AddExpenseForm(username = username, onExpenseAdded = onExpenseAdded)
            1 -> AddInvestmentForm(username = username, onInvestmentAdded = onInvestmentAdded)
            2 -> AddFinanceForm(username = username, onFinanceAdded = onFinanceAdded) // Nayi call
        }
    }
}
