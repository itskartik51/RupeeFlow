// Isme maine Amount field ko "Number Only" bana diya hai
OutlinedTextField(
    value = amount, 
    onValueChange = { 
        // Sirf numbers lene ka logic
        if (it.all { char -> char.isDigit() }) {
            amount = it
        }
    }, 
    label = { Text("Amount (₹)") },
    modifier = Modifier.fillMaxWidth(), 
    singleLine = true,
    // Yeh keyboard ko sirf number mode par kholega
    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
    ),
    colors = TextFieldDefaults.outlinedTextFieldColors(
        focusedBorderColor = MaterialTheme.colorScheme.primary
    )
)
