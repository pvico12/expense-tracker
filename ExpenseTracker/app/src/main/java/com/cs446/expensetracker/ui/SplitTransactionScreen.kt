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
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.Transaction
import com.cs446.expensetracker.ui.ui.theme.mainBackgroundColor
import kotlinx.coroutines.launch

@Composable
fun SplitTransactionScreen(transaction: Transaction? = null) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var amountInput by remember { mutableStateOf(transaction?.amount?.toString() ?: "") }
    var numberOfPeople by remember { mutableStateOf("") }
    var emailList by remember { mutableStateOf(listOf<String>()) }

    val count = numberOfPeople.toIntOrNull() ?: 0
    val isExceeding = count > 10
    val numberLabel = if (isExceeding) "Maximum number of people is 10" else "Enter number of people to split with"

    if (!isExceeding && emailList.size != count) {
        emailList = List(count) { index -> emailList.getOrNull(index) ?: "" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(mainBackgroundColor)
            .padding(16.dp)
    ) {
        Text(
            text = "Split Bill",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (transaction != null) {
            Text(text = "Transaction: ${transaction.note}")
            Text(text = "Amount: $${transaction.amount}")
        } else {
            OutlinedTextField(
                value = amountInput,
                onValueChange = { amountInput = it },
                label = { Text("Enter Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = numberOfPeople,
            onValueChange = { numberOfPeople = it },
            label = { Text(numberLabel) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = isExceeding,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (!isExceeding) {
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
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val parsedAmount = amountInput.toDoubleOrNull() ?: transaction?.amount ?: 0.0
                if (count > 0 && !isExceeding) {
                    val splitAmount = parsedAmount / (count + 1)

                    coroutineScope.launch {
                        transaction?.let { newTransaction ->
                            val id = newTransaction.id
                            if (id != null) {
                                val updatedTransaction = newTransaction.copy(amount = splitAmount)
                                try {
                                    val updateResponse = RetrofitInstance.apiService.updateTransaction(id, updatedTransaction)
                                    if (!updateResponse.isSuccessful) {
                                        // TODO
                                    }
                                } catch (e: Exception) {
                                    // TODO
                                }
                            }
                        }
                    }

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
