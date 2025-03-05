package com.cs446.expensetracker.ui

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.Category
import com.cs446.expensetracker.api.models.CategoryRequest
import com.cs446.expensetracker.api.models.Transaction
import com.cs446.expensetracker.session.UserSession
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var expenseAmount by remember { mutableStateOf("") }
    var transactionNote by remember { mutableStateOf("") }
    var vendorName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Category List State
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    // AI Suggestion Loading State
    var isAiLoading by remember { mutableStateOf(false) }

    // Bottom Sheet State
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    // Date Picker State
    val calendar = Calendar.getInstance()
    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
    var selectedDate by remember {
        mutableStateOf(
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            ).format(calendar.time)
        )
    }

    // Date Picker Dialog
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Fetch categories from API when the screen loads
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val token = UserSession.access_token ?: ""
                val response = RetrofitInstance.apiService.getCategories()
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
        Text(text = "NEW TRANSACTION", style = MaterialTheme.typography.headlineMedium)

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

        // Vendor Input
        OutlinedTextField(
            value = vendorName,
            onValueChange = { vendorName = it },
            label = { Text("Item / Vendor Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // AI Category Suggestion Button
        Button(
            onClick = {
                coroutineScope.launch {
                    isAiLoading = true
                    val response = RetrofitInstance.apiService.getCategorySuggestion(CategoryRequest(vendorName))
                    isAiLoading = false
                    if (response.isSuccessful) {
                        val aiCategory = response.body()
                        selectedCategory = categories.find { it.id == aiCategory?.category_id }
                    } else {
                        errorMessage = "AI could not predict the category."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = vendorName.isNotBlank()
        ) {
            if (isAiLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("Run with AI")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Category Box (Tap to Open Bottom Sheet)
        Text(text = "Category", style = MaterialTheme.typography.bodyLarge)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clickable { showBottomSheet = true }
                .padding(10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(text = selectedCategory?.name ?: "Select a Category")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Error Message Display
        if (errorMessage != null) {
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Date Picker
        Text(text = "Date", style = MaterialTheme.typography.bodyLarge)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clickable { datePickerDialog.show() }
                .padding(10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(text = selectedDate)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Transaction Note
        OutlinedTextField(
            value = transactionNote,
            onValueChange = { transactionNote = it },
            label = { Text("Note (optional)") },
            modifier = Modifier.fillMaxWidth()
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
                        if (amount == null || selectedCategory == null) {
                            errorMessage = "Please fill in all fields correctly."
                            return@launch
                        }

                        val transaction = Transaction(
                            amount = amount,
                            category_id = selectedCategory!!.id,
                            transaction_type = "expense",
                            note = transactionNote,
                            date = isoFormat.format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate)!!) // Convert here
                        )

                        val token = UserSession.access_token ?: ""
                        val response =
                            RetrofitInstance.apiService.addTransaction(transaction)
                        if (response.isSuccessful) {
                            Toast.makeText(
                                context,
                                "Expense added successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
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


        // Bottom Sheet Implementation
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                Text(
                    text = "Select a Category",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp) // Limits height for scrolling
                ) {
                    items(categories) { category ->
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCategory = category
                                    showBottomSheet = false
                                }
                                .padding(8.dp),
                            headlineContent = {
                                Text(
                                    text = category.name,
                                    color = if (selectedCategory?.id == category.id) Color.Blue else Color.Black
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
