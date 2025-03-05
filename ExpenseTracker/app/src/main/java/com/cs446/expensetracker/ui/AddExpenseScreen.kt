package com.cs446.expensetracker.ui

import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.cs446.expensetracker.api.models.CategoryRequest
import com.cs446.expensetracker.api.models.OcrResponse
import com.cs446.expensetracker.api.models.Transaction
import com.cs446.expensetracker.session.UserSession
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

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            coroutineScope.launch {
                isLoading = true
                val transactions = uploadCsv(context, selectedUri, 0)
                isLoading = false
                if (transactions != null) {
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
                val receipt = uploadReceipt(context, selectedUri)
                isLoading = false
                if (receipt != null) {
                    expenseAmount = receipt.total.toString()
                    transactionNote = "Receipt scanned items added."
                    Toast.makeText(context, "Receipt Scanned!", Toast.LENGTH_SHORT).show()
                } else {
                    errorMessage = "Failed to scan receipt."
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

        Button(
            onClick = { filePickerLauncher.launch("text/csv") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Upload CSV")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = { receiptPickerLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scan Receipt")
        }

        Spacer(modifier = Modifier.height(10.dp))

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

suspend fun uploadCsv(context: Context, uri: Uri, createTransactions: Int): List<Transaction>? {
    return try {
        val file = uriToMultipart(context, uri)
        val response = RetrofitInstance.apiService.uploadCsv(file, createTransactions)
        if (response.isSuccessful) response.body() else null
    } catch (e: Exception) {
        null
    }
}

suspend fun uploadReceipt(context: Context, uri: Uri): OcrResponse? {
    return try {
        val file = uriToMultipart(context, uri)
        val response = RetrofitInstance.apiService.scanReceipt(file)
        if (response.isSuccessful) response.body() else null
    } catch (e: Exception) {
        null
    }
}

fun uriToMultipart(context: Context, uri: Uri): MultipartBody.Part {
    val inputStream = context.contentResolver.openInputStream(uri) ?: return MultipartBody.Part.createFormData("file", "")
    val file = File(context.cacheDir, "upload_temp")
    file.outputStream().use { output -> inputStream.copyTo(output) }

    val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
    return MultipartBody.Part.createFormData("file", file.name, requestFile)
}
