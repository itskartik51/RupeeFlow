package com.kartikey.rupeeflow.UI_Screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePicker(
    label: String,
    selectedDateMillis: Long?,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)

    val displayDate = if (selectedDateMillis != null) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
    } else {
        "DD/MM/YYYY"
    }

    // Box transparent layer to make the entire TextField clickable seamlessly
    Box(modifier = modifier.clickable { showDialog = true }) {
        OutlinedTextField(
            value = displayDate,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            enabled = false, // Prevents keyboard popup
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = Color.Black,
                disabledBorderColor = Color.Gray,
                disabledLabelColor = Color.DarkGray,
                disabledTrailingIconColor = Color(0xFF2E7D32)
            ),
            trailingIcon = {
                Icon(Icons.Outlined.DateRange, contentDescription = "Select Date")
            }
        )
    }

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                    showDialog = false
                }) {
                    Text("OK", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Color.White)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = Color(0xFF2E7D32),
                    todayDateBorderColor = Color(0xFF2E7D32),
                    todayContentColor = Color(0xFF2E7D32)
                )
            )
        }
    }
}

