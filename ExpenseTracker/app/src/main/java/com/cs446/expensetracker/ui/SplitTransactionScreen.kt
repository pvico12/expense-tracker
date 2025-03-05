package com.cs446.expensetracker.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cs446.expensetracker.models.Transaction
import com.cs446.expensetracker.ui.ui.theme.mainBackgroundColor

@Composable
fun SplitTransactionScreen(transaction: Transaction? = null) {
    val context = LocalContext.current

    // If a transaction exists, use its amount; otherwise, allow user input.
    var amountInput by remember { mutableStateOf(transaction?.amount?.toString() ?: "") }
    var numberOfPeople by remember { mutableStateOf("") }
    var emailList by remember { mutableStateOf(listOf<String>()) }
    val count = numberOfPeople.toIntOrNull() ?: 0

    // Adjust the email list size whenever the count changes.
    if (emailList.size != count) {
        emailList = List(count) { index -> emailList.getOrNull(index) ?: "" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(mainBackgroundColor)
            .padding(16.dp)
    ) {
        // If a transaction exists, show its note and amount.
        Text(
            text = "Split Bill",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (transaction != null) {
            Text(text = "Transaction: ${transaction.note}")
            Text(text = "Amount: $${transaction.amount}")
        } else {
            // Allow user to input an amount if no transaction is provided.
            OutlinedTextField(
                value = amountInput,
                onValueChange = { amountInput = it },
                label = { Text("Enter Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Input for the number of people to split with.
        OutlinedTextField(
            value = numberOfPeople,
            onValueChange = { numberOfPeople = it },
            label = { Text("Enter number of people to split with") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Dynamically create a text field for each email address.
        repeat(count) { index ->
            OutlinedTextField(
                value = emailList.getOrNull(index) ?: "",
                onValueChange = { newEmail ->
                    emailList = emailList.toMutableList().also { it[index] = newEmail }
                },
                label = { Text("Email ${index + 1}") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Send Email button that constructs a mailto URI and opens the email app.
        Button(
            onClick = {
                // Use the amount from the transaction if available, otherwise the user input.
                val parsedAmount = amountInput.toDoubleOrNull() ?: transaction?.amount ?: 0.0
                if (count > 0) {
                    val splitAmount = parsedAmount / count
                    val recipients = emailList.joinToString(separator = ",")
                    val subject = "E-Transfer request for ${transaction?.note ?: "Transaction"}"
                    val body = "Hello! Please E-Transfer this amount to the sender: $${"%.2f".format(splitAmount)}. Thank you!"
                    val mailtoUri = "mailto:$recipients" +
                            "?subject=${Uri.encode(subject)}" +
                            "&body=${Uri.encode(body)}"
                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse(mailtoUri)
                    }
                    context.startActivity(emailIntent)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF228B22))
        ) {
            Text("Send Email")
        }
    }
}
