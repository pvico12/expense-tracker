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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.Category
import com.cs446.expensetracker.api.models.CustomCategoryRequest
import com.cs446.expensetracker.api.models.SuggestionRequest
import com.cs446.expensetracker.api.models.OcrResponse
import com.cs446.expensetracker.api.models.RecurringTransactionRequest
import com.cs446.expensetracker.api.models.Transaction
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
    var aiFeedbackMessage by remember { mutableStateOf<String?>(null) }

    // Bottom Sheet State
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    // Recurrence Period
    var isRecurring by remember { mutableStateOf(false) }
    var recurrencePeriod by remember { mutableStateOf(7) } // Default to weekly (7 days)
    var expanded by remember { mutableStateOf(false) }

    // End Date
    var endDate by remember { mutableStateOf("") }
    var isEndDateValid by remember { mutableStateOf(true) } // Track if end date is valid

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
            // Reset end date when start date changes
            endDate = ""
            isEndDateValid = true
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // End Date Picker Dialog

    val endDatePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedEndDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)

            // Ensure end date is later than start date
            if (selectedEndDate > selectedDate) {
                endDate = selectedEndDate
                isEndDateValid = true
            } else {
                isEndDateValid = false
            }
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).apply {
        // Prevent selecting an end date before the start date
        datePicker.minDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate)?.time ?: 0L
    }

    // CSV Review State
    var parsedTransactions by remember { mutableStateOf<List<Transaction>?>(null) }
    var showReviewDialog by remember { mutableStateOf(false) }


    // CSV Template
    var csvTemplate by remember { mutableStateOf<List<List<String>>?>(null) }
    var isTemplateLoading by remember { mutableStateOf(false) }

    var showTemplatePreviewDialog by remember { mutableStateOf(false) }
    var showCsvHelpDialog by remember { mutableStateOf(false) }



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
            Text(text = "Add Transaction", style = MaterialTheme.typography.headlineMedium)
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

        val isAmountValid = isValidAmount(expenseAmount)

        OutlinedTextField(
            value = expenseAmount,
            onValueChange = { expenseAmount = it },
            label = { Text("Amount") },
            isError = !isAmountValid && expenseAmount.isNotBlank(),
            supportingText = {
                if (!isAmountValid && expenseAmount.isNotBlank()) {
                    Text("Please enter a valid non-negative amount")

                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

//        Spacer(modifier = Modifier.height(8.dp))

        // Vendor Input
        OutlinedTextField(
            value = vendorName,
            onValueChange = {
                vendorName = it
                aiFeedbackMessage = null
                            },
            label = { Text("Item / Vendor Name ") },
            trailingIcon = {
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
                                aiFeedbackMessage = "✅ AI picked ${selectedCategory?.name}"
                            } else {
                                aiFeedbackMessage = "❌ AI flopped. Time for manual mode! "
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        Color(0xFF4B0C0C),
                    ),
                    enabled = vendorName.isNotBlank()
                ) {
                    if (isAiLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = " Run AI")
                        Text("Run AI")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        aiFeedbackMessage?.let {
            Text(
                text = it,
                color = if (it.contains("✅")) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
//                style = MaterialTheme.typography.bodySmall,
//                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Category Box (Tap to Open Bottom Sheet)
        Text(text = "Category", style = MaterialTheme.typography.bodyLarge)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clickable { showBottomSheet = true }
                .padding(8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(text = selectedCategory?.name ?: "Tap to Open Bottom Sheet")
        }

//        Spacer(modifier = Modifier.height(8.dp))

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

        Spacer(modifier = Modifier.height(8.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text("Date & Recurrence", style = MaterialTheme.typography.titleMedium)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { datePickerDialog.show() }
                    ) {
                        Text(text = selectedDate)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text("Recurring? ")
                    Switch(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF4B0C0C),)
                    )
                }

                if (isRecurring) {

                    // End Date Picker
                    TextButton(onClick = { endDatePickerDialog.show() }) {
                        Text(if (endDate.isNotEmpty()) "Until $endDate" else "Pick End Date")
                    }

                    // Show error message if end date is invalid
                    if (!isEndDateValid) {
                        Text(
                            text = "End date must be later than the start date",
                            color = Color.Red,
                        )
                    }

                    // Recurrence Period
                    TextButton(
                        onClick = { expanded = true },
                    ) {
                        Text(
                            text = "Repeats: ${when (recurrencePeriod) {
                                1 -> "Daily"
                                7 -> "Weekly"
                                30 -> "Monthly"
                                365 -> "Yearly"
                                else -> "Custom"
                            }}"
                        )
                    }

                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf(
                            "Daily" to 1,
                            "Weekly" to 7,
                            "Monthly" to 30,
                            "Yearly" to 365
                        ).forEach { (label, days) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    recurrencePeriod = days
                                    expanded = false
                                }
                            )
                        }
                    }
                }

            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Transaction Note
        OutlinedTextField(
            value = transactionNote,
            onValueChange = { transactionNote = it },
            label = { Text("Note (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Scan Receipt Button
            Button(
                onClick = { receiptPickerLauncher.launch("image/*") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    Color(0xFF4B0C0C),
                )
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Scan Receipt")
            }

            // Upload CSV Button
            Button(
                onClick = { filePickerLauncher.launch("text/*") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    Color(0xFF4B0C0C),
                )
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Upload CSV")
            }



        }

        // Download CSV Template
        Button(
            onClick = {
                coroutineScope.launch {
                    isTemplateLoading = true
                    try {
                        val response = RetrofitInstance.apiService.getCsvTemplate()
                        if (response.isSuccessful) {
                            val csvText = response.body()?.string()
                            csvTemplate = csvText?.let { parseCsvTemplate(it) }
                            showTemplatePreviewDialog = true
                        } else {
                            Toast.makeText(context, "Failed to download CSV template.", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isTemplateLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(Color(0xFF4B0C0C))
        ) {
            Icon(Icons.Default.Download, contentDescription = "Download Template")
            Spacer(Modifier.width(8.dp))
            Text("Preview CSV Template")
        }

        if (showCsvHelpDialog) {
            AlertDialog(
                onDismissRequest = { showCsvHelpDialog = false },
                confirmButton = {
                    TextButton(onClick = { showCsvHelpDialog = false }) {
                        Text("Got it ✔️")
                    }
                },
                title = {
                    Text("CSV Column Format Guide", style = MaterialTheme.typography.titleLarge)
                },
                text = {
                    Column() {
//                        Text("🧾 Here's what each column means:")
//                        Spacer(Modifier.height(8.dp))
                        val descriptions = listOf(
                            "amount 💵" to "The numeric value of the expense (e.g., 100.50).",
                            "category 📂" to "The name of the expense category (must match an existing one).",
                            "transaction_type 🔁" to "Usually 'EXPENSE' or 'INCOME'.",
                            "note 📝" to "Optional description of the transaction.",
                            "date 📅" to "The date of the transaction in yyyy-MM-dd format.",
                            "vendor 🏪" to "Where or whom you spent money on."
                        )

                        descriptions.forEach { (field, explanation) ->
                            Column(modifier = Modifier.padding(bottom = 6.dp)) {
                                Text(
                                    text = field,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = explanation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            )
        }


        if (showTemplatePreviewDialog && csvTemplate != null) {
            AlertDialog(
                onDismissRequest = { showTemplatePreviewDialog = false },
                confirmButton = {
                    TextButton(onClick = { showTemplatePreviewDialog = false }) {
                        Text("Close")
                    }
                },
                title = {
                    Text(
                        "CSV Format Preview",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                },
                text = {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showCsvHelpDialog = true }) {
                                Text("What does each column mean?")
                            }
                        }

                        csvTemplate!!.forEachIndexed { index, row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(if (index == 0) Color(0xFFEFEFEF) else Color.Transparent)
                            ) {
                                row.forEach { cell ->
                                    Text(
                                        text = cell,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(4.dp),
                                        fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }


        Spacer(Modifier.height(10.dp))

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
            enabled = !isLoading && selectedCategory != null && isAmountValid
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

        if (isRecurring) {

            Spacer(modifier = Modifier.height(8.dp))

            // Save Recurring Transaction Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            val amount = expenseAmount.toDoubleOrNull()
                            if (amount == null || selectedCategory == null || endDate.isEmpty() || !isEndDateValid) {
                                errorMessage = "Please fill in all fields correctly."
                                return@launch
                            }

                            val recurringTransaction = RecurringTransactionRequest(
                                start_date = isoFormat.format(
                                    SimpleDateFormat(
                                        "yyyy-MM-dd",
                                        Locale.getDefault()
                                    ).parse(selectedDate)!!
                                ),
                                end_date = isoFormat.format(
                                    SimpleDateFormat(
                                        "yyyy-MM-dd",
                                        Locale.getDefault()
                                    ).parse(endDate)!!
                                ),
                                note = transactionNote,
                                period = recurrencePeriod,
                                amount = amount,
                                category_id = selectedCategory!!.id,
                                transaction_type = "expense",
                                vendor = vendorName
                            )

                            val response = RetrofitInstance.apiService.createRecurringTransaction(
                                recurringTransaction
                            )
                            if (response.isSuccessful) {
                                Toast.makeText(
                                    context,
                                    "Recurring transaction added successfully!",
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
                colors = ButtonDefaults.buttonColors(Color(0xFF4B0C0C)),
                enabled = !isLoading && endDate.isNotEmpty() && isEndDateValid
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(text = "Save Recurring Transaction")
                }
            }
        }

        // Error Message Display
        if (errorMessage != null) {
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Review Dialog
        if (showReviewDialog && parsedTransactions != null) {
            AlertDialog(
                onDismissRequest = { showReviewDialog = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.extraLarge,
                title = {
                    Text(
                        text = "Review Transactions",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                },
                text = {
                    Column {
                        // Summary chip
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = "${parsedTransactions!!.size} transactions found",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }

                        // Transactions list
                        LazyColumn(
                            modifier = Modifier
                                .heightIn(max = 400.dp)
                                .padding(vertical = 4.dp)
                        ) {
                            items(parsedTransactions!!) { transaction ->
                                val categoryName = categories.find { it.id == transaction.category_id }?.name ?: "Unknown"

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    elevation = CardDefaults.cardElevation(1.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFF8F8F8),
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Amount row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "💰 Amount",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                            Text(
                                                text = "$${"%.2f".format(transaction.amount)}",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // Category row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "📂 Category",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                            Text(
                                                text = categoryName,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }

                                        // Date row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "📅 Date",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                            Text(
                                                text = transaction.date,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }

                                        // Note (if available)
                                        if (!transaction.note.isNullOrEmpty()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "📃 Note",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                                Text(
                                                    text = transaction.note,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                            }
                                        }
                                    }
                                }
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
                                        val transaction = Transaction(
                                            amount = parsedTransaction.amount,
                                            category_id = parsedTransaction.category_id,
                                            transaction_type = "expense",
                                            note = parsedTransaction.note,
                                            date = isoFormat.format(
                                                SimpleDateFormat(
                                                    "yyyy-MM-dd",
                                                    Locale.getDefault()
                                                ).parse(parsedTransaction.date)!!
                                            ),
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

                                val message = "Saved $successCount transactions${if (failureCount > 0) ", $failureCount failed" else ""}"
                                Toast.makeText(
                                    context,
                                    message,
                                    Toast.LENGTH_LONG
                                ).show()

                                if (successCount > 0) {
                                    navController.popBackStack()
                                }
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("✅Save All", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showReviewDialog = false },
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("Cancel", style = MaterialTheme.typography.labelLarge)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurface
            )
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
    return Regex("^#[0-9a-fA-F]{6}\$").matches(color)
//    return try {
//        android.graphics.Color.parseColor(color)
//        true
//    } catch (e: IllegalArgumentException) {
//        false
//    }
}

// Helper function to validate amount
fun isValidAmount(input: String): Boolean {
    return input.toDoubleOrNull()?.let { it >= 0.0 } ?: false
}

// CSV Parsing Helper
fun parseCsvTemplate(csv: String): List<List<String>> {
    return csv.trim()
        .lines()
        .filter { it.isNotBlank() }
        .map { it.split(",") }
}
