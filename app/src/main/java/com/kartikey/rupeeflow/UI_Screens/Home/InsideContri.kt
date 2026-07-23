package com.kartikey.rupeeflow.UI_Screens.Home

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ==========================================
// MOCK DATA MODELS (For UI Testing)
// ==========================================
data class ContriExpense(val itemName: String, val amount: Double, val date: String)
data class MemberLedger(val memberName: String, val totalSpent: Double, val expenses: List<ContriExpense>)

@Composable
fun InsideContriScreen(
    room: ContriRoomModel,
    onBackClick: () -> Unit,
    onLeaveClick: () -> Unit,
    onAddClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    val formattedName = if (room.roomName.length > 10) "${room.roomName.take(10)}..." else room.roomName

    // Demo Data matching your sketch
    val mockLedgers = listOf(
        MemberLedger(
            memberName = "XX", 
            totalSpent = 30.0, 
            expenses = listOf(
                ContriExpense("Toffee", 15.0, "17 July"),
                ContriExpense("Pen", 15.0, "19 July")
            )
        ),
        MemberLedger(
            memberName = "YY", 
            totalSpent = 46.0, 
            expenses = listOf(
                ContriExpense("Snacks", 20.0, "16 July"),
                ContriExpense("Tea", 26.0, "14 July")
            )
        ),
        MemberLedger(
            memberName = "ZZ", 
            totalSpent = 35.0, 
            expenses = listOf(
                ContriExpense("Cold Drink", 35.0, "15 July")
            )
        )
    )

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Color.Black)
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = formattedName,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                IconButton(onClick = onLeaveClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ExitToApp, 
                        contentDescription = "Leave Room", 
                        tint = Color.Red
                    )
                }
            }
        },
        floatingActionButton = {
            PremiumFloatingButton(onClick = onAddClick)
        },
        containerColor = Color(0xFFFAFAFA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ==========================================
            // INFO CARD
            // ==========================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("₹", fontSize = 38.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("0", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                clipboardManager.setText(AnnotatedString("Join my RupeeFlow Contri!\nCode: ${room.roomCode}\nPin: ${room.pin}"))
                                Toast.makeText(context, "Code Copied!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy, 
                                contentDescription = "Copy", 
                                tint = Color.Gray, 
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = room.roomCode, 
                                fontSize = 20.sp, 
                                fontWeight = FontWeight.ExtraBold, 
                                color = Color.Black, 
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pin: ${room.pin}", 
                            fontSize = 16.sp, 
                            color = Color.Gray, 
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // SKETCH IMPLEMENTATION (LEDGER TABLE)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // 1. TOP ROW: User Name & Total Spent
                Row {
                    mockLedgers.forEach { ledger ->
                        Column(
                            modifier = Modifier.width(110.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = ledger.memberName, 
                                fontSize = 16.sp, 
                                fontWeight = FontWeight.ExtraBold, 
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "₹${ledger.totalSpent.toInt()}", 
                                fontSize = 18.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = Color(0xFF2E7D32) // Premium Green
                            )
                        }
                    }
                }

                // 2. CONTINUOUS DIVIDER LINE
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp).width((mockLedgers.size * 110).dp),
                    thickness = 1.5.dp,
                    color = Color.LightGray
                )

                // 3. BOTTOM ROW: Expense Items
                Row {
                    mockLedgers.forEach { ledger ->
                        Column(
                            modifier = Modifier.width(110.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ledger.expenses.forEach { expense ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    Text(
                                        text = expense.itemName, 
                                        fontSize = 15.sp, 
                                        fontWeight = FontWeight.SemiBold, 
                                        color = Color.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "₹${expense.amount.toInt()}", 
                                            fontSize = 13.sp, 
                                            fontWeight = FontWeight.Bold, 
                                            color = Color.DarkGray
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = expense.date, 
                                            fontSize = 11.sp, 
                                            color = Color.Gray,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// PREMIUM BOUNCE FLOATING BUTTON (+)
// ==========================================
@Composable
fun PremiumFloatingButton(onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.85f else 1f, label = "fabScale")

    Box(
        modifier = Modifier
            .scale(scale)
            .size(56.dp)
            .background(Color(0xFF2E7D32), shape = CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = "Add Expense",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}
