package com.cs446.expensetracker.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
    val scrollState = rememberScrollState()

    // Use the transaction amount if provided; otherwise, use user-entered amount.
    var amountInput by remember { mutableStateOf(transaction?.amount?.toString() ?: "") }
    // User's own amount (shown only in custom mode)
    var userAmountInput by remember { mutableStateOf("") }
    var numberOfPeople by remember { mutableStateOf("") }
    var emailList by remember { mutableStateOf(listOf<String>()) }
    // List holding each recipient's amount (only used in custom mode)
    var recipientAmounts by remember { mutableStateOf(listOf<String>()) }
    // Toggle switch: if true, split equally (computed amounts); if false, amounts are entered manually.
    var isSplitEqually by remember { mutableStateOf(false) }

    val count = numberOfPeople.toIntOrNull() ?: 0
    val isExceeding = count > 10
    val numberLabel = if (isExceeding) "Maximum number of people is 10" else "Enter number of people to split with"

    // Adjust email and recipient amounts list sizes when the number of people changes.
    if (!isExceeding) {
        if (emailList.size != count) {
            emailList = List(count) { index -> emailList.getOrNull(index) ?: "" }
        }
        if (recipientAmounts.size != count) {
            recipientAmounts = List(count) { index -> recipientAmounts.getOrNull(index) ?: "" }
        }
    }

    // Compute the total amount from the transaction or input.
    val totalAmount = (transaction?.amount ?: amountInput.toDoubleOrNull()) ?: 0.0

    // Determine if all amounts have been entered.
    // In equal mode, the amounts are computed automatically so consider them as entered.
    val allAmountsEntered = if (isSplitEqually) {
        true
    } else {
        userAmountInput.isNotBlank() && count > 0 && (recipientAmounts.size == count) && recipientAmounts.all { it.isNotBlank() }
    }

    // Compute validity only if all amounts are entered.
    val isAmountsValid: Boolean = if (!allAmountsEntered) {
        false
    } else {
        if (isSplitEqually) {
            // Compute the equal share among (count+1) people and ensure it's > 0.
            val computedShare = if (count > 0) totalAmount / (count + 1) else 0.0
            val calculatedTotal = computedShare * (count + 1)
            kotlin.math.abs(calculatedTotal - totalAmount) <= 0.01 && computedShare > 0.0
        } else {
            val userAmt = userAmountInput.toDoubleOrNull() ?: 0.0
            val recipientsValues = recipientAmounts.mapNotNull { it.toDoubleOrNull() }
            // If any amount (user or recipient) is 0 or less, mark as invalid.
            if (userAmt <= 0.0 || recipientsValues.any { it <= 0.0 }) {
                false
            } else {
                kotlin.math.abs(userAmt + recipientsValues.sum() - totalAmount) <= 0.01
            }
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(mainBackgroundColor)
            .padding(16.dp)
    ) {
        Text(
            text = "Split Bill",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Row with vendor/total info on the left and toggle on the right.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (transaction != null) {
                    Text(text = "Vendor: ${transaction.vendor ?: ""}")
                    Text(text = "Total Amount: $${transaction.amount}")
                } else {
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Enter Total Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Split Equally")
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isSplitEqually,
                        onCheckedChange = { isSplitEqually = it }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = numberOfPeople,
            onValueChange = { numberOfPeople = it },
            label = { Text(numberLabel) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = isExceeding,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("NumberOfPeopleField")
        )
        Spacer(modifier = Modifier.height(16.dp))

        // User's amount field is shown only in custom (non-equal) mode.
        if (!isSplitEqually) {
            Text(
                text = "Your Amount",
                style = MaterialTheme.typography.headlineMedium
            )
            OutlinedTextField(
                value = userAmountInput,
                onValueChange = { userAmountInput = it },
                label = { Text("Enter Your Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                isError = userAmountInput.isNotBlank() && (userAmountInput.toDoubleOrNull() == 0.0)
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        // For each recipient, display email and amount fields.
        if (!isExceeding) {
            repeat(count) { index ->
                OutlinedTextField(
                    value = emailList.getOrNull(index) ?: "",
                    onValueChange = { newEmail ->
                        emailList = emailList.toMutableList().also { it[index] = newEmail }
                    },
                    label = { Text("Email ${index + 1}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("EmailField_$index")
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (isSplitEqually) {
                    // Compute equal share automatically.
                    val computedShare = if (count > 0) totalAmount / (count + 1) else 0.0
                    OutlinedTextField(
                        value = "%.2f".format(computedShare),
                        onValueChange = {},
                        label = { Text("Amount for Email ${index + 1}") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        // In equal mode, error outline appears only if all amounts are entered and the computed total is off.
                        isError = allAmountsEntered && !isAmountsValid
                    )
                } else {
                    OutlinedTextField(
                        value = recipientAmounts.getOrNull(index) ?: "",
                        onValueChange = { newAmt ->
                            recipientAmounts = recipientAmounts.toMutableList().also { it[index] = newAmt }
                        },
                        label = { Text("Amount for Email ${index + 1}") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        isError = (recipientAmounts.getOrNull(index)?.toDoubleOrNull() == 0.0)
                                || (allAmountsEntered && !isAmountsValid)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // The button is wrapped in a Box that intercepts clicks if amounts aren't valid.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (!allAmountsEntered || !isAmountsValid)
                        Modifier.clickable {
                            Toast
                                .makeText(context, "Amounts don't equal transaction amount", Toast.LENGTH_LONG)
                                .show()
                        } else Modifier
                )
        ) {
            Button(
                onClick = {
                    // Recheck validations on click.
                    if (!allAmountsEntered || !isAmountsValid) {
                        Toast.makeText(context, "Amounts don't equal transaction amount", Toast.LENGTH_LONG)
                            .show()
                        return@Button
                    }
                    if (count <= 0 || isExceeding) {
                        Toast.makeText(context, "Please enter a valid number of people", Toast.LENGTH_LONG)
                            .show()
                        return@Button
                    }

                    if (isSplitEqually) {
                        val computedShare = if (count > 0) totalAmount / (count + 1) else 0.0
                        val calculatedTotal = computedShare * (count + 1)
                        if (kotlin.math.abs(calculatedTotal - totalAmount) > 0.01) {
                            Toast.makeText(context, "Amounts don't equal transaction amount", Toast.LENGTH_LONG)
                                .show()
                            return@Button
                        }
                        coroutineScope.launch {
                            transaction?.let { existingTransaction ->
                                val id = existingTransaction.id
                                if (id != null) {
                                    val updatedTransaction = existingTransaction.copy(amount = computedShare)
                                    try {
                                        val updateResponse = RetrofitInstance.apiService.updateTransaction(id, updatedTransaction)
                                        if (!updateResponse.isSuccessful) {
                                            // TODO: Handle unsuccessful update
                                        }
                                    } catch (e: Exception) {
                                        // TODO: Handle exception
                                    }
                                }
                            }
                        }
                        val emailDetails = emailList.mapIndexed { index, email ->
                            "$email: $${"%.2f".format(computedShare)}"
                        }.joinToString(separator = "\n")
                        val subject = "E-Transfer request for ${transaction?.vendor ?: "Transaction"}"
                        var body = ""
                        if(isSplitEqually) {
                            body = "Hello!\nPlease E-Transfer the following amount to the sender:\n$emailDetails\nThank you!"
                        } else {
                            body = emailList.mapIndexed { index, email ->
                                val amt = recipientAmounts.getOrNull(index)?.toDoubleOrNull() ?: 0.0
                                "$email please e-transfer $${"%.2f".format(amt)} to sender"
                            }.joinToString(separator = "\n")
                        }
                        val recipients = emailList.joinToString(separator = ",")
                        val mailtoUri = "mailto:$recipients" +
                                "?subject=${Uri.encode(subject)}" +
                                "&body=${Uri.encode(body)}"
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse(mailtoUri)
                        }
                        context.startActivity(emailIntent)
                    } else {
                        val userAmt = userAmountInput.toDoubleOrNull() ?: 0.0
                        val recipientsTotal = recipientAmounts.mapNotNull { it.toDoubleOrNull() }.sum()
                        if (kotlin.math.abs(userAmt + recipientsTotal - totalAmount) > 0.01) {
                            Toast.makeText(context, "Amounts don't equal transaction amount", Toast.LENGTH_LONG)
                                .show()
                            return@Button
                        }
                        coroutineScope.launch {
                            transaction?.let { existingTransaction ->
                                val id = existingTransaction.id
                                if (id != null) {
                                    val updatedTransaction = existingTransaction.copy(amount = userAmt)
                                    try {
                                        val updateResponse = RetrofitInstance.apiService.updateTransaction(id, updatedTransaction)
                                        if (!updateResponse.isSuccessful) {
                                            Log.d("UpdateTransaction", "Unsuccessful")
                                        }
                                    } catch (e: Exception) {
                                        Log.d("UpdateTransaction", e.toString())
                                    }
                                }
                            }
                        }
                        val emailDetails = emailList.mapIndexed { index, email ->
                            val amt = recipientAmounts.getOrNull(index)?.toDoubleOrNull() ?: 0.0
                            "$email: $${"%.2f".format(amt)}"
                        }.joinToString(separator = "\n")
                        val subject = "E-Transfer request for ${transaction?.note ?: "Transaction"}"
                        val body = "Hello!\nPlease E-Transfer the following amount to the sender:\n$emailDetails\nThank you!"
                        val recipients = emailList.joinToString(separator = ",")
                        val mailtoUri = "mailto:$recipients" +
                                "?subject=${Uri.encode(subject)}" +
                                "&body=${Uri.encode(body)}"
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse(mailtoUri)
                        }
                        context.startActivity(emailIntent)
                    }
                },
                enabled = allAmountsEntered && isAmountsValid && (count > 0) && !isExceeding,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("SendEmailButton"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF228B22))
            ) {
                Text("Send Email")
            }
        }
    }
}
