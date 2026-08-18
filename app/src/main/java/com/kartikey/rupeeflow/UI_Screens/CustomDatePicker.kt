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

    // 🚀 FIX: Convert Local Time to UTC Time so DatePicker doesn't jump 1 day back
    val adjustedInitialMillis = remember(selectedDateMillis) {
        selectedDateMillis?.let {
            val tz = TimeZone.getDefault()
            it + tz.getOffset(it) // Adding timezone offset (e.g., +5:30 for IST)
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = adjustedInitialMillis,
        selectableDates = selectableDates
    )

    // Ensures state updates correctly if date is changed from outside
    LaunchedEffect(adjustedInitialMillis) {
        datePickerState.selectedDateMillis = adjustedInitialMillis
    }

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
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.primary
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
                    datePickerState.selectedDateMillis?.let { utcMillis ->
                        // 🚀 FIX: Convert UTC Time back to Local Time before returning it to App
                        val tz = TimeZone.getDefault()
                        val localMillis = utcMillis - tz.getOffset(utcMillis)
                        onDateSelected(localMillis)
                    }
                    showDialog = false
                }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    headlineContentColor = MaterialTheme.colorScheme.onSurface,
                    weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    subheadContentColor = MaterialTheme.colorScheme.onSurface,
                    yearContentColor = MaterialTheme.colorScheme.onSurface,
                    currentYearContentColor = MaterialTheme.colorScheme.primary,
                    selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                    selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
                    dayContentColor = MaterialTheme.colorScheme.onSurface,
                    disabledDayContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                    todayDateBorderColor = MaterialTheme.colorScheme.primary,
                    todayContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
