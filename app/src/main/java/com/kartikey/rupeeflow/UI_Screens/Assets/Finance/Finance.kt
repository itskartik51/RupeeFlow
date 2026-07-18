package com.kartikey.rupeeflow.UI_Screens.Assets.Finance

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Money
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikey.rupeeflow.UI_Screens.Assets.BankAccountItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    onBackClick: () -> Unit,
    username: String,
    bankList: List<BankAccountItem>,
    isLoading: Boolean,
    onRefreshClick: () -> Unit,
    onEditBankClick: (BankAccountItem) -> Unit 
) {
    var currentFinanceView by remember { mutableStateOf("Main") }

    if (currentFinanceView == "Main") {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("My Finance Vault", fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8F9FA))
                )
            },
            containerColor = Color(0xFFF8F9FA)
        ) { paddingValues ->
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BigFinanceCard("Cash", "₹0.00", Icons.Outlined.Money, Color(0xFF388E3C), isClickable = false) {}
                
                val totalBank = bankList.sumOf { it.currentBalance }
                BigFinanceCard("Bank Accounts", formatRupeeAmount(totalBank), Icons.Outlined.AccountBalance, Color(0xFF1976D2), isClickable = true) { currentFinanceView = "BankAccounts" }
                
                BigFinanceCard("Credit Card", "₹0.00", Icons.Outlined.CreditCard, Color(0xFFD32F2F), isClickable = false) {}
                BigFinanceCard("FD : Fixed Deposit", "₹0.00", Icons.Outlined.Savings, Color(0xFFF57C00), isClickable = false) {}
            }
        }
    } else if (currentFinanceView == "BankAccounts") {
        BankAccountsScreen(
            onBackClick = { currentFinanceView = "Main" },
            username = username, 
            bankList = bankList,
            isLoading = isLoading,
            onRefreshClick = onRefreshClick,
            onEditBankClick = onEditBankClick 
        )
    }
}

@Composable
fun BigFinanceCard(title: String, amount: String, icon: ImageVector, iconColor: Color, isClickable: Boolean, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (isPressed && isClickable) 0.97f else 1f, label = "ScaleAnim")
    
    Card(
        modifier = Modifier.fillMaxWidth().height(100.dp).scale(cardScale).pointerInput(Unit) {
            if (isClickable) { 
                detectTapGestures(onPress = { 
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                    onClick() 
                }) 
            }
        },
        colors = CardDefaults.cardColors(containerColor = Color.White), 
        shape = RoundedCornerShape(16.dp), 
        elevation = CardDefaults.cardElevation(if (isClickable) 4.dp else 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(50.dp).background(iconColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(text = title, color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = amount, color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
