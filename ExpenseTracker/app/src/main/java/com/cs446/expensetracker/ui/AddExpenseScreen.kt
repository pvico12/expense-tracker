package com.cs446.expensetracker.ui

import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.cs446.expensetracker.api.models.CustomCategoryRequest
import com.cs446.expensetracker.api.models.SuggestionRequest
import com.cs446.expensetracker.api.models.OcrResponse
import com.cs446.expensetracker.api.models.Transaction
import com.cs446.expensetracker.session.UserSession
import com.cs446.expensetracker.ui.ui.theme.Typography
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var expenseAmount by remember { mutableStateOf("") }
    var transactionNote by remember { mutableStateOf("") }
    var vendorName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Category List State
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    // Show Custom Category Dialog
    var showCustomCategoryDialog by remember { mutableStateOf(false) }

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

    // CSV Review State
    var parsedTransactions by remember { mutableStateOf<List<Transaction>?>(null) }
    var showReviewDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            coroutineScope.launch {
                isLoading = true
                val fetchedTransactions = uploadCsv(context, selectedUri, 0) // Set createTransactions to 0 to just parse the CSV
                isLoading = false
                if (fetchedTransactions != null) {
                    parsedTransactions = fetchedTransactions
                    showReviewDialog = true
                    Toast.makeText(context, "CSV Uploaded!", Toast.LENGTH_SHORT).show()
                } else {
                    errorMessage = "Failed to upload CSV."
                }
            }
        }
    }

    val receiptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            coroutineScope.launch {
                isLoading = true
                uploadReceipt(context, selectedUri, categories) { receipt, suggestedCategory ->
                    isLoading = false
                    if (receipt != null) {
                        expenseAmount = receipt.total.toString()
                        transactionNote = "Receipt scanned items added."

                        // Use first item description as vendor name
                        vendorName = receipt.items.firstOrNull()?.descriptor ?: ""

                        // Select AI-suggested category if available
                        selectedCategory = suggestedCategory

                        Toast.makeText(context, "Receipt Scanned!", Toast.LENGTH_SHORT).show()
                    } else {
                        errorMessage = "Failed to scan receipt."
                    }
                }
            }
        }
    }


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
            .verticalScroll(scrollState) // Enable scrolling
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Enter Transaction", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.weight(1.0f))
            TextButton(onClick = { navController.popBackStack() },
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = 0.dp,
                    end = 0.dp,
                    bottom = 10.dp,
                )) {
                Text("X",  style = Typography.titleLarge, color= Color(0xFF4B0C0C))
            }
        }
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
                    val response = RetrofitInstance.apiService.getCategorySuggestion(SuggestionRequest(vendorName))
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
            colors = ButtonDefaults.buttonColors(
                Color(0xFF4B0C0C),
            ),
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

        // Button to Show Custom Category Dialog
        Button(
            onClick = { showCustomCategoryDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                Color(0xFF4B0C0C),
            ),
        ) {
            Text("Add Custom Category")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Show Custom Category Popup
        if (showCustomCategoryDialog) {
            CustomCategoryDialog(
                onDismiss = { showCustomCategoryDialog = false },
                onSave = { newCategory ->
                    // Add the new category to the list
                    categories = categories + newCategory
                    // Optionally, select the new category
                    selectedCategory = newCategory
                }
            )
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
                            date = isoFormat.format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate)!!),
                            vendor = vendorName
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
            colors = ButtonDefaults.buttonColors(
                Color(0xFF4B0C0C),
            ),
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

        Spacer(modifier = Modifier.height(10.dp))

        // Upload CSV Button
        Button(
            onClick = { filePickerLauncher.launch("text/*") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                Color(0xFF4B0C0C),
            )
        ) {
            Text("Upload CSV")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Review Dialog
        if (showReviewDialog && parsedTransactions != null) {
            AlertDialog(
                onDismissRequest = { showReviewDialog = false },
                title = { Text("Review Transactions") },
                text = {
                    LazyColumn {
                        items(parsedTransactions!!) { parsedTransaction ->
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Amount: ${parsedTransaction.amount}")
                                Text("Category: ${categories.find { it.id == parsedTransaction.category_id }?.name ?: "Unknown"}")
                                Text("Note: ${parsedTransaction.note}")
                                Text("Date: ${parsedTransaction.date}")
                                Divider()
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                var successCount = 0
                                var failureCount = 0

                                parsedTransactions?.forEach { parsedTransaction ->
                                    try {
                                        val token = UserSession.access_token ?: ""

                                        val transaction = Transaction(
                                            amount = parsedTransaction.amount,
                                            category_id = parsedTransaction.category_id,
                                            transaction_type = "expense",
                                            note = parsedTransaction.note,
                                            date = isoFormat.format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(parsedTransaction.date)!!),
                                            vendor = parsedTransaction.vendor
                                        )
                                        val response = RetrofitInstance.apiService.addTransaction(transaction)
                                        if (response.isSuccessful) {
                                            successCount++
                                        } else {
                                            failureCount++
                                        }
                                    } catch (e: Exception) {
                                        failureCount++
                                    }
                                }

                                isLoading = false
                                showReviewDialog = false

                                val message = "Saved $successCount transactions, $failureCount failed."
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()

                                if (successCount > 0) {
                                    navController.popBackStack()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            Color(0xFF4B0C0C),
                        )
                    ) {
                        Text("Save Transactions")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showReviewDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            Color(0xFF4B0C0C),
                        )
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Scan Receipt Button
        Button(
            onClick = { receiptPickerLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                Color(0xFF4B0C0C),
            )
        ) {
            Text("Scan Receipt")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Error Message Display
        if (errorMessage != null) {
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(10.dp))
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

suspend fun uploadCsv(context: Context, uri: Uri, createTransactions: Int): List<Transaction>? {
    return try {
        val file = uriToMultipart(context, uri)
        val response = RetrofitInstance.apiService.uploadCsv(file, createTransactions)
        if (response.isSuccessful) response.body() else null
    } catch (e: Exception) {
        null
    }
}

suspend fun uploadReceipt(context: Context, uri: Uri, categories: List<Category>, onResult: (OcrResponse?, Category?) -> Unit) {
    try {
        val file = uriToMultipart(context, uri)
        val response = RetrofitInstance.apiService.scanReceipt(file)

        if (response.isSuccessful) {
            val receipt = response.body()

            // Extract first item's description
            val firstItemDescription = receipt?.items?.firstOrNull()?.descriptor

            if (firstItemDescription != null) {
                // Send item name to AI endpoint for category suggestion
                val aiResponse = RetrofitInstance.apiService.getCategorySuggestion(SuggestionRequest(firstItemDescription))

                if (aiResponse.isSuccessful) {
                    val suggestedCategory = aiResponse.body()
                    val matchedCategory = categories.find { it.id == suggestedCategory?.category_id }
                    onResult(receipt, matchedCategory)
                } else {
                    onResult(receipt, null) // AI failed, return receipt without a category
                }
            } else {
                onResult(receipt, null) // No items in receipt, return without category
            }
        } else {
            onResult(null, null) // Receipt scanning failed
        }
    } catch (e: Exception) {
        onResult(null, null) // Handle errors gracefully
    }
}

fun uriToMultipart(context: Context, uri: Uri): MultipartBody.Part {
    val inputStream = context.contentResolver.openInputStream(uri) ?: return MultipartBody.Part.createFormData("file", "")
    val file = File(context.cacheDir, "upload_temp")
    file.outputStream().use { output -> inputStream.copyTo(output) }

    val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
    return MultipartBody.Part.createFormData("file", file.name, requestFile)
}

@Composable
fun CustomCategoryDialog(
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit, // Callback to notify parent about the new category
    modifier: Modifier = Modifier
) {
    var categoryName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#98Ceed") } // Default color
    var showError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) } // Loading state for API call
    var apiErrorMessage by remember { mutableStateOf<String?>(null) } // Error message from API

    val coroutineScope = rememberCoroutineScope()

    // Predefined color options
    val colorOptions = listOf(
        "#98Ceed", "#FFCC99", "#FF6666", "#99CC99", "#CC99FF", "#FF99CC", "#99CCCC", "#FF9966"
    )

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Create Custom Category", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(modifier = Modifier.padding(8.dp)) {
                // Category Name Input
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = showError && categoryName.isBlank()
                )

                if (showError && categoryName.isBlank()) {
                    Text(
                        text = "Category name cannot be empty",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Color Picker Section
                Text(
                    text = "Select a Color",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Predefined Color Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4), // Fixed 4 columns
                    modifier = Modifier.height(120.dp)
                ) {
                    items(colorOptions) { color ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(40.dp)
                                .background(Color(android.graphics.Color.parseColor(color)), CircleShape)
                                .clickable { selectedColor = color }
                                .border(
                                    width = if (selectedColor == color) 2.dp else 0.dp,
                                    color = Color.Black,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Custom Hex Color Input (Optional)
                OutlinedTextField(
                    value = selectedColor,
                    onValueChange = { selectedColor = it },
                    label = { Text("Custom Color (Hex Code)") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = showError && !isValidHexColor(selectedColor)
                )

                if (showError && !isValidHexColor(selectedColor)) {
                    Text(
                        text = "Invalid hex color code",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // API Error Message
                if (apiErrorMessage != null) {
                    Text(
                        text = apiErrorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (categoryName.isBlank() || !isValidHexColor(selectedColor)) {
                        showError = true
                    } else {
                    // Create the custom category via API
                    coroutineScope.launch {
                        isLoading = true
                        apiErrorMessage = null

                        try {
                            val customCategoryRequest = CustomCategoryRequest(
                                name = categoryName,
                                color = selectedColor
                            )

                            val response = RetrofitInstance.apiService.createCustomCategory(customCategoryRequest)
                            if (response.isSuccessful) {
                                val newCategory = response.body()
                                if (newCategory != null) {
                                    // Notify parent about the new category
                                    onSave(newCategory)
                                    onDismiss()
                                } else {
                                    apiErrorMessage = "Failed to create category."
                                }
                            } else {
                                apiErrorMessage = "Failed to create category: ${response.message()}"
                            }
                        } catch (e: Exception) {
                            apiErrorMessage = "Error: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                }
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF4B0C0C),
            contentColor = Color.White
        ),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text("Save")
        }
    }
        },
        dismissButton = {
            Button(
                onClick = { onDismiss() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.LightGray,
                    contentColor = Color.Black
                )
            ) {
                Text("Cancel")
            }
        }
    )
}

// Helper function to validate hex color codes
fun isValidHexColor(color: String): Boolean {
    return try {
        android.graphics.Color.parseColor(color)
        true
    } catch (e: IllegalArgumentException) {
        false
    }
}