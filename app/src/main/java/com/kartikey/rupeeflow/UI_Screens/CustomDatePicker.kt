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
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePicker(
    label: String,
    selectedDateMillis: Long?,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    restrictToCurrentMonth: Boolean = false
) {
    var showDialog by remember { mutableStateOf(false) }

    // Smart logic to restrict dates only to the CURRENT and PREVIOUS month
    val selectableDates = remember(restrictToCurrentMonth) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                if (!restrictToCurrentMonth) return true
                val currentCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                val targetCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcTimeMillis }

                val yearDiff = currentCal.get(Calendar.YEAR) - targetCal.get(Calendar.YEAR)
                val monthDiff = yearDiff * 12 + currentCal.get(Calendar.MONTH) - targetCal.get(Calendar.MONTH)

                // 0 means current month, 1 means previous month. Block everything else.
                return monthDiff == 0 || monthDiff == 1
            }

            override fun isSelectableYear(year: Int): Boolean {
                if (!restrictToCurrentMonth) return true
                val currentCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                val currentYear = currentCal.get(Calendar.YEAR)
                val currentMonth = currentCal.get(Calendar.MONTH)

                // If current month is January, allow the previous year (for December)
                return if (currentMonth == Calendar.JANUARY) {
                    year == currentYear || year == currentYear - 1
                } else {
                    year == currentYear
                }
            }
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateMillis,
        selectableDates = selectableDates
    )

    val displayDate = if (selectedDateMillis != null) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
    } else {
        "DD/MM/YYYY"
    }

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
