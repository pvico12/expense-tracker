package com.cs446.expensetracker.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cs446.expensetracker.api.Category
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.Transaction
import com.cs446.expensetracker.session.UserSession
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AddExpenseScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var expenseAmount by remember { mutableStateOf("") }

    var transactionNote by remember { mutableStateOf("") }
    var expenseDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // Fetch categories from API
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val token = UserSession.access_token ?: ""
                val response = RetrofitInstance.apiService.getCategories("Bearer $token")
                if (response.isSuccessful) {
                    categories = response.body() ?: emptyList()
                } else {
                    errorMessage = "Failed to load categories."
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Add New Expense", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(20.dp))

        // Amount Input
        OutlinedTextField(
            value = expenseAmount,
            onValueChange = { expenseAmount = it },
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category Dropdown // needed to be fixed
        Box {
            OutlinedTextField(
                value = selectedCategory?.name ?: "Select Category",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isDropdownExpanded = true },
                label = { Text("Category") }
            )

            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        onClick = {
                            selectedCategory = category
                            isDropdownExpanded = false // Close dropdown after selection
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Transaction Note
        OutlinedTextField(
            value = transactionNote,
            onValueChange = { transactionNote = it },
            label = { Text("Note (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Date Input (Pre-filled in ISO format)
        OutlinedTextField(
            value = expenseDate,
            onValueChange = { expenseDate = it },
            label = { Text("Date (ISO 8601 Format)") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Error Message Display
        if (errorMessage != null) {
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Save Expense Button
        Button(
            onClick = {
                coroutineScope.launch {
                    isLoading = true
                    errorMessage = null
                    try {
                        val amount = expenseAmount.toDoubleOrNull()
                        if (amount == null) { // need to be fixed
                            errorMessage = "Please fill in all fields correctly."
                            return@launch
                        }

                        val transaction = Transaction(
                            amount = amount,
                            category_id = 1, // need to be fixed
                            transaction_type = "expense",
                            note = transactionNote,
                            date = expenseDate
                        )

                        val token = UserSession.access_token ?: ""
                        val response = RetrofitInstance.apiService.addTransaction("Bearer $token", transaction)
                        if (response.isSuccessful) {
                            Toast.makeText(context, "Expense added successfully!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        } else {
                            errorMessage = "Failed to add transaction. Please try again."
                        }
                    } catch (e: Exception) {
                        errorMessage = "Error: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(text = "Save Transaction")
            }
        }
    }
}
