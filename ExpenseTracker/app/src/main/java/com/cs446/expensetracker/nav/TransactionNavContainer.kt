package com.cs446.expensetracker.nav

import android.app.DatePickerDialog
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.TransactionResponse
import com.cs446.expensetracker.api.models.Transaction
import com.cs446.expensetracker.session.UserSession
import com.cs446.expensetracker.ui.SplitTransactionScreen  // Import the bill splitting screen from ui package
import com.cs446.expensetracker.ui.ui.theme.mainBackgroundColor
import com.cs446.expensetracker.ui.ui.theme.tileColor
import kotlinx.coroutines.launch
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import retrofit2.HttpException

class TransactionNavContainer {

    @Composable
    fun TransactionNavHost() {
        val transactionNavController = rememberNavController()
        NavHost(transactionNavController, startDestination = "history") {

            composable("history") {
                TransactionHistoryScreen(
                    onTransactionClick = { transactionId ->
                        transactionNavController.navigate("history/detail/$transactionId")
                    }
                )
            }

            composable("history/detail/{transactionId}") { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
                TransactionDetailScreen(
                    transactionId = transactionId,
                    onBackClick = { transactionNavController.popBackStack() },
                    onSplitClick = { transaction ->
                        transactionNavController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("transactionToSplit", transaction)

                        transactionNavController.navigate("split")
                    }
                )
            }

            composable("split") {
                val transaction = transactionNavController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<Transaction>("transactionToSplit")

                SplitTransactionScreen(transaction = transaction)
            }
        }
    }


    @Composable
    fun TransactionHistoryScreen(onTransactionClick: (String) -> Unit) {
        var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        var searchQuery by remember { mutableStateOf("") }
        var startDate by remember { mutableStateOf<Date?>(null) }
        var endDate by remember { mutableStateOf<Date?>(null) }

        val filteredTransactions = transactions.filter { transaction ->
            val matchesQuery = if (searchQuery.isNotBlank())
                transaction.note.contains(searchQuery, ignoreCase = true)
            else true

            val transactionDate: Date? = parseIsoDate(transaction.date)

            val inDateRange = if (transactionDate != null) {
                (startDate?.let { transactionDate >= it } ?: true) &&
                        (endDate?.let { transactionDate <= it } ?: true)
            } else {
                true
            }

            matchesQuery && inDateRange
        }

        LaunchedEffect(Unit) {
            isLoading = true
            errorMessage = null
            try {
                val token = UserSession.access_token ?: ""
                val response: Response<List<TransactionResponse>> =
                    RetrofitInstance.apiService.getTransactions(skip = 0, limit = 100)
                if (response.isSuccessful) {
                    val transactionResponses = response.body() ?: emptyList()
                    Log.d("TransactionHistory", "TransactionResponse list: $transactionResponses")
                    transactions = transactionResponses.map { tr ->
                        Transaction(
                            id = tr.id,
                            amount = tr.amount.toDouble(),
                            category_id = tr.categoryId,
                            transaction_type = tr.transactionType ?: "expense",
                            note = if (tr.note.toString().isEmpty()) "Transaction" else tr.note.toString(),
                            date = tr.date ?: ""
                        )
                    }
                    Log.d("Transactions", "Transactions list: $transactions")
                } else {
                    errorMessage = "Failed to load transactions."
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(mainBackgroundColor)
                .padding(16.dp)
        ) {
            Text(
                text = "Transaction History",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Search bar: note text and date range pickers.
            TransactionSearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                startDate = startDate,
                onStartDateChange = { startDate = it },
                endDate = endDate,
                onEndDateChange = { endDate = it }
            )
            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading -> {
                    CircularProgressIndicator()
                }
                errorMessage != null -> {
                    Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                }
                else -> {
                    LazyColumn {
                        items(filteredTransactions) { transaction ->
                            TransactionListItem(transaction, onClick = {
                                onTransactionClick(transaction.date)
                            })
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun TransactionSearchBar(
        searchQuery: String,
        onSearchQueryChange: (String) -> Unit,
        startDate: Date?,
        onStartDateChange: (Date?) -> Unit,
        endDate: Date?,
        onEndDateChange: (Date?) -> Unit
    ) {
        val context = LocalContext.current

        // ----- Start Date Picker Implementation -----
        val startCalendar = Calendar.getInstance()
        var selectedStartDate by remember {
            mutableStateOf(
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startCalendar.time)
            )
        }
        val startDatePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedStartDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                onStartDateChange(sdf.parse(selectedStartDate))
            },
            startCalendar.get(Calendar.YEAR),
            startCalendar.get(Calendar.MONTH),
            startCalendar.get(Calendar.DAY_OF_MONTH)
        )

        // ----- End Date Picker Implementation -----
        val endCalendar = Calendar.getInstance()
        var selectedEndDate by remember {
            mutableStateOf(
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(endCalendar.time)
            )
        }
        val endDatePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedEndDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                onEndDateChange(sdf.parse(selectedEndDate))
            },
            endCalendar.get(Calendar.YEAR),
            endCalendar.get(Calendar.MONTH),
            endCalendar.get(Calendar.DAY_OF_MONTH)
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            // Text field for note search.
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Search...") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Start and End Date Pickers (UI commented out)
        }
    }

    @Composable
    fun TransactionListItem(transaction: Transaction, onClick: () -> Unit) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp)
                .clickable { onClick() },
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = tileColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = transaction.note, fontWeight = FontWeight.Bold)
                    Text(
                        text = formatTransactionDate(transaction.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Text(
                    text = "$${transaction.amount}",
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.transaction_type == "expense")
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondary
                )
            }
        }
    }


    @Composable
    fun TransactionDetailScreen(
        transactionId: String,
        onBackClick: () -> Unit,
        onSplitClick: (Transaction) -> Unit // Parameter for handling the split bill navigation
    ) {
        var transaction: Transaction? by remember { mutableStateOf(null) }
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        // New state variables for edit mode.
        var isEditMode by remember { mutableStateOf(false) }
        var editedAmount by remember { mutableStateOf("") }
        var editedNote by remember { mutableStateOf("") }

        // Create a coroutine scope for API calls.
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(transactionId) {
            try {
                val token = UserSession.access_token ?: ""
                val response = RetrofitInstance.apiService.getTransactions(skip = 0, limit = 100)
                if (response.isSuccessful) {
                    val transactionResponses = response.body() ?: emptyList()
                    val transactions = transactionResponses.map { tr ->
                        Transaction(
                            id = tr.id,
                            amount = tr.amount.toDouble(),
                            category_id = tr.categoryId,
                            transaction_type = tr.transactionType ?: "expense",
                            note = if (tr.note.toString().isEmpty()) "Transaction" else tr.note.toString(),
                            date = tr.date ?: ""
                        )
                    }
                    transaction = transactions.find { it.date == transactionId }
                    if (transaction == null) {
                        errorMessage = "Transaction not found."
                    } else {
                        Log.d("Transaction", "T: ${transaction}")
                    }
                } else {
                    errorMessage = "Failed to load transaction details."
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }

        LaunchedEffect(transaction) {
            transaction?.let {
                editedAmount = it.amount.toString()
                editedNote = it.note
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(mainBackgroundColor)
                .padding(16.dp)
        ) {
            Text(
                text = "Transaction Details",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            when {
                isLoading -> {
                    CircularProgressIndicator()
                }
                errorMessage != null -> {
                    Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                }
                else -> {
                    transaction?.let { tr ->
                        if (isEditMode) {
                            OutlinedTextField(
                                value = editedAmount,
                                onValueChange = { editedAmount = it },
                                label = { Text("Amount") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editedNote,
                                onValueChange = { editedNote = it },
                                label = { Text("Note") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(text = "Amount: $${tr.amount}", fontWeight = FontWeight.Bold)
                            Text(text = "Note: ${tr.note}", fontWeight = FontWeight.Bold)
                        }
                        Text(text = "Category ID: ${tr.category_id}", fontWeight = FontWeight.Bold)
                        Text(text = "Type: ${tr.transaction_type}", fontWeight = FontWeight.Bold)
                        Text(text = "Date: ${tr.date}", fontWeight = FontWeight.Bold)
                    } ?: Text(text = "Transaction not found.")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (isEditMode) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            // Convert transactionId to Int (assumes it's convertible)
                            val id = transaction?.id
                            if (id == null) {
                                Log.e("TransactionDetail", "Invalid transaction id: ${transactionId}")
                                return@launch
                            }
                            try {
                                val updatedTransaction = Transaction(
                                    id = id,
                                    amount = editedAmount.toDoubleOrNull() ?: transaction?.amount ?: 0.0,
                                    category_id = transaction?.category_id ?: 0,
                                    transaction_type = transaction?.transaction_type ?: "expense",
                                    note = editedNote,
                                    date = transaction?.date ?: "",
                                    vendor = ""
                                )
                                val response = RetrofitInstance.apiService.updateTransaction(id, updatedTransaction)
                                if (response.isSuccessful) {
                                    isEditMode = false
                                    transaction = updatedTransaction
                                } else {
                                    Log.e("TransactionDetail", "Update failed: ${response.errorBody()?.string()}, ${transaction}")
                                }
                            } catch (e: Exception) {
                                Log.e("TransactionDetail", "Exception during update: ${e.message}")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes")
                }
            } else {
                Button(
                    onClick = {
                        transaction?.let { onSplitClick(it) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF228B22))
                ) {
                    Text("Split Bill")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { onBackClick() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Back to History")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        isEditMode = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val id = transaction?.id
                            if (id == null) {
                                Log.e("TransactionDetail", "Invalid transaction id: ${transactionId}")
                                return@launch
                            }
                            try {
                                val response = RetrofitInstance.apiService.deleteTransaction(id)
                                if (response.isSuccessful) {
                                    // Deletion successful, navigate back to history.
                                    onBackClick()
                                } else {
                                    Log.e("TransactionDetail", "Deletion failed: ${response.errorBody()?.string()}")
                                }
                            } catch (e: Exception) {
                                Log.e("TransactionDetail", "Exception during deletion: ${e.message}")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete")
                }
            }
        }
    }


    // Helper to format date (assumes ISO format "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private fun formatTransactionDate(isoDate: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(isoDate)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            ""
        }
    }

    // Helper to parse an ISO date string to a Date object.
    private fun parseIsoDate(isoDate: String): Date? {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.parse(isoDate)
        } catch (e: Exception) {
            null
        }
    }

    // Dummy helper to get a Transaction by its ID.
    // Replace this with your own implementation or repository lookup.
    private fun getTransactionById(transactionId: String): Transaction? {
        // For now, simply return null so that the SplitTransactionScreen
        // shows an empty amount text field for user input.
        return null
    }
}
