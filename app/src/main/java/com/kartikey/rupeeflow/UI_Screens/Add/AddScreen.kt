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

// Ye imports ab apne aap classes ko dhundh lenge
import com.kartikey.rupeeflow.UI_Screens.*
import com.kartikey.rupeeflow.UI_Screens.Home.*

@Composable
fun AddScreen(
    paddingValues: PaddingValues, 
    username: String,
    onExpenseAdded: (TransactionModel) -> Unit,
    onInvestmentAdded: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Add Expense", "Add Investment")

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
                    .clickable { /* Future navigation back hook */ }
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

        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = Color(0xFF2E7D32),
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
                    text = { Text(title, fontWeight = FontWeight.Bold) },
                    selectedContentColor = Color(0xFF2E7D32),
                    unselectedContentColor = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (selectedTabIndex) {
            0 -> AddExpenseForm(username = username, onExpenseAdded = onExpenseAdded)
            1 -> AddInvestmentForm(username = username, onInvestmentAdded = onInvestmentAdded)
        }
    }
}
