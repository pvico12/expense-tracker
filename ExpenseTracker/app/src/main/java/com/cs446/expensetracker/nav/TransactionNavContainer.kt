package com.cs446.expensetracker.nav

import android.app.DatePickerDialog
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.Category
import com.cs446.expensetracker.api.models.TransactionResponse
import com.cs446.expensetracker.api.models.Transaction
import com.cs446.expensetracker.session.UserSession
import com.cs446.expensetracker.ui.SplitTransactionScreen
import com.cs446.expensetracker.ui.ui.theme.mainBackgroundColor
import com.cs446.expensetracker.ui.ui.theme.tileColor
import kotlinx.coroutines.launch
import retrofit2.Response
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.Locale

class TransactionNavContainer {

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun TransactionNavHost() {
        val navController = rememberNavController()
        NavHost(navController, startDestination = "history") {

            composable("history") {
                TransactionHistoryScreen(
                    navController = navController,
                    onTransactionClick = { transaction ->
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("selectedTransaction", transaction)
                        navController.navigate("history/detail")
                    }
                )
            }

            composable("history/detail") {
                val transaction = navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<Transaction>("selectedTransaction")
                TransactionDetailScreen(
                    transaction = transaction,
                    onBackClick = { navController.popBackStack() },
                    onSplitClick = { txn ->
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("transactionToSplit", txn)
                        navController.navigate("split")
                    },
                    onTransactionUpdated = { updatedTxn ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("updatedTransaction", updatedTxn)
                    }
                )
            }

            composable("split") {
                val transaction = navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<Transaction>("transactionToSplit")
                SplitTransactionScreen(transaction = transaction)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun TransactionHistoryScreen(
        navController: NavController,
        onTransactionClick: (Transaction) -> Unit
    ) {
        var searchQuery by rememberSaveable { mutableStateOf("") }
        var selectedStartDate by rememberSaveable {
            mutableStateOf(
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                    Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, getActualMinimum(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time
                )
            )
        }
        var selectedEndDate by rememberSaveable {
            mutableStateOf(
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                    Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 0)
                    }.time
                )
            )
        }
        var showAdvancedFilter by rememberSaveable { mutableStateOf(false) }

        // These Date? values are updated via the advanced filter pickers.
        var startDate by remember { mutableStateOf<Date?>(null) }
        var endDate by remember { mutableStateOf<Date?>(null) }

        // List of transactions (assumed Parcelable for saving state if needed).
        var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var advancedFilterChanged by remember { mutableStateOf(false) }

        val calendar = Calendar.getInstance()
        val context = LocalContext.current
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val startDatePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedStartDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                startDate = inputDateFormat.parse(selectedStartDate)
                selectedEndDate = ""
                endDate = null
                advancedFilterChanged = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        val endDatePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newEndDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                if (newEndDate > selectedStartDate) {
                    selectedEndDate = newEndDate
                    endDate = inputDateFormat.parse(newEndDate)
                    advancedFilterChanged = true
                } else {
                    // Show error if needed.
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = inputDateFormat.parse(selectedStartDate)?.time ?: 0L
        }

        LaunchedEffect(showAdvancedFilter) {
            if (!showAdvancedFilter) {
                val apiStartDateTime = isoFormat.format(inputDateFormat.parse(selectedStartDate)!!)
                val apiEndDateTime = isoFormat.format(inputDateFormat.parse(selectedEndDate)!!)
                isLoading = true
                errorMessage = null
                try {
                    val response: Response<List<TransactionResponse>> =
                        RetrofitInstance.apiService.getTransactions(
                            skip = 0,
                            limit = 100,
                            startDate = apiStartDateTime,
                            endDate = apiEndDateTime
                        )
                    if (response.isSuccessful) {
                        val transactionResponses = response.body() ?: emptyList()
                        transactions = transactionResponses.map { tr ->
                            Transaction(
                                id = tr.id,
                                amount = tr.amount.toDouble(),
                                category_id = tr.categoryId,
                                transaction_type = tr.transactionType ?: "expense",
                                note = if (tr.note.toString().isEmpty()) "Transaction" else tr.note.toString(),
                                date = tr.date ?: "",
                                vendor = tr.vendor ?: "N/A"
                            )
                        }
                    } else {
                        errorMessage = "Failed to load transactions."
                    }
                } catch (e: Exception) {
                    errorMessage = "Error: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        }

        LaunchedEffect(Unit) {
            isLoading = true
            Log.d("Token", "Token: $UserSession.access_token")
                val apiStartDateTime = isoFormat.format(inputDateFormat.parse(selectedStartDate)!!)
                val apiEndDateTime = isoFormat.format(inputDateFormat.parse(selectedEndDate)!!)
                isLoading = true
                errorMessage = null
                try {
                    val response: Response<List<TransactionResponse>> =
                        RetrofitInstance.apiService.getTransactions(
                            skip = 0,
                            limit = 100,
                            startDate = apiStartDateTime,
                            endDate = apiEndDateTime
                        )
                    if (response.isSuccessful) {
                        val transactionResponses = response.body() ?: emptyList()
                        transactions = transactionResponses.map { tr ->
                            Transaction(
                                id = tr.id,
                                amount = tr.amount.toDouble(),
                                category_id = tr.categoryId,
                                transaction_type = tr.transactionType ?: "expense",
                                note = if (tr.note.toString().isEmpty()) "Transaction" else tr.note.toString(),
                                date = tr.date ?: "",
                                vendor = tr.vendor ?: "N/A"
                            )
                        }
                    } else {
                        errorMessage = "Failed to load transactions."
                    }
                } catch (e: Exception) {
                    errorMessage = "Error: ${e.message}"
                } finally {
                    isLoading = false
                }

        }

        LaunchedEffect(advancedFilterChanged, selectedStartDate, selectedEndDate) {
            if (showAdvancedFilter && advancedFilterChanged &&
                selectedStartDate.isNotEmpty() && selectedEndDate.isNotEmpty()) {

                val apiStartDateTime = isoFormat.format(inputDateFormat.parse(selectedStartDate)!!)
                val apiEndDateTime = isoFormat.format(inputDateFormat.parse(selectedEndDate)!!)
                isLoading = true
                errorMessage = null
                try {
                    val response: Response<List<TransactionResponse>> =
                        RetrofitInstance.apiService.getTransactions(
                            skip = 0,
                            limit = 100,
                            startDate = apiStartDateTime,
                            endDate = apiEndDateTime
                        )
                    if (response.isSuccessful) {
                        val transactionResponses = response.body() ?: emptyList()
                        transactions = transactionResponses.map { tr ->
                            Transaction(
                                id = tr.id,
                                amount = tr.amount.toDouble(),
                                category_id = tr.categoryId,
                                transaction_type = tr.transactionType ?: "expense",
                                note = if (tr.note.toString().isEmpty()) "Transaction" else tr.note.toString(),
                                date = tr.date ?: "",
                                vendor = tr.vendor ?: "N/A"
                            )
                        }
                    } else {
                        errorMessage = "Failed to load transactions."
                    }
                } catch (e: Exception) {
                    errorMessage = "Error: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        }

        LaunchedEffect(navController.currentBackStackEntry?.savedStateHandle) {
            val updatedTxn = navController.currentBackStackEntry
                ?.savedStateHandle
                ?.getLiveData<Transaction>("updatedTransaction")
                ?.value
            if (updatedTxn != null) {
                transactions = transactions.map { txn ->
                    if (txn.id == updatedTxn.id) updatedTxn else txn
                }
                navController.currentBackStackEntry?.savedStateHandle?.remove<Transaction>("updatedTransaction")
            }
        }

        val filteredTransactions = transactions.filter { txn ->
            if (searchQuery.isNotBlank())
                txn.note.contains(searchQuery, ignoreCase = true) || txn.vendor?.contains(searchQuery, ignoreCase = true) ?: false
            else true
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

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search...") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Animated arrow toggle for advanced filter
            val rotationAngle by animateFloatAsState(targetValue = if (showAdvancedFilter) 90f else 0f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAdvancedFilter = !showAdvancedFilter },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Advanced Filter (Date Range)",
                    style = MaterialTheme.typography.bodyLarge,
//                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Toggle Advanced Filter",
                    modifier = Modifier.rotate(rotationAngle)
                )
            }

            if (showAdvancedFilter) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Start Date", style = MaterialTheme.typography.bodyLarge)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .border(BorderStroke(1.dp, Color.Gray), shape = RoundedCornerShape(4.dp))
                                .clickable { startDatePickerDialog.show() }
                                .padding(10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(text = selectedStartDate)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "End Date", style = MaterialTheme.typography.bodyLarge)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .border(BorderStroke(1.dp, Color.Gray), shape = RoundedCornerShape(4.dp))
                                .clickable { endDatePickerDialog.show() }
                                .padding(10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(text = if (selectedEndDate.isNotEmpty()) selectedEndDate else "Select End Date")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }


        when {
                isLoading -> CircularProgressIndicator()
                errorMessage != null -> Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                else -> {
                    LazyColumn {
                        items(filteredTransactions) { txn ->
                            TransactionListItem(txn, onClick = { onTransactionClick(txn) })
                        }
                    }
                }
            }
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
                    Text(text = transaction.vendor ?: transaction.note, fontWeight = FontWeight.Bold)
                    Text(
                        text = formatDate(transaction.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Text(
                    text = "$${"%.2f".format(transaction.amount)}",
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
        transaction: Transaction?,
        onBackClick: () -> Unit,
        onSplitClick: (Transaction) -> Unit,
        onTransactionUpdated: (Transaction) -> Unit
    ) {
        // Maintain a mutable copy of the transaction.
        var currentTransaction by remember { mutableStateOf(transaction) }
        var isEditMode by remember { mutableStateOf(false) }
        var editedAmount by remember { mutableStateOf("") }
        var editedVendor by remember { mutableStateOf("") }
        val coroutineScope = rememberCoroutineScope()

        var categoriesList by remember { mutableStateOf<List<Category>>(emptyList()) }
        var selectedCategory: Category? by remember { mutableStateOf(null) }
        var expanded by remember { mutableStateOf(false) }

        // Date picker state.
        val calendar = Calendar.getInstance()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        var selectedDate by remember { mutableStateOf("") }
        val context = LocalContext.current
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Initialize fields when the transaction is first passed in.
        LaunchedEffect(currentTransaction) {
            currentTransaction?.let {
                editedAmount = it.amount.toString()
                editedVendor = it.vendor ?: ""
                selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(it.date)
                        ?: calendar.time
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(mainBackgroundColor)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaction Details",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { isEditMode = true }) {
                    Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit Transaction")
                }
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            currentTransaction?.let { txn ->
                                if (txn.id == null) {
                                    onBackClick()
                                } else {
                                    try {
                                        val response = RetrofitInstance.apiService.deleteTransaction(txn.id)
                                        if (response.isSuccessful) {
                                            onBackClick()
                                        } else {
                                            Log.e("TransactionDetail", "Deletion failed: ${response.errorBody()?.string()}")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("TransactionDetail", "Exception during deletion: ${e.message}")
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete Transaction")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            if (currentTransaction == null) {
                Text(text = "Transaction not found.", color = MaterialTheme.colorScheme.error)
            } else {
                if (isEditMode) {
                    OutlinedTextField(
                        value = editedAmount,
                        onValueChange = { editedAmount = it },
                        label = { Text("Amount") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editedVendor,
                        onValueChange = { editedVendor = it },
                        label = { Text("Vendor") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                    Text(text = "Category", style = MaterialTheme.typography.bodyLarge)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = true }
                            .border(1.dp, Color.Gray, shape = RoundedCornerShape(4.dp))
                            .padding(12.dp)
                    ) {
                        Text(text = selectedCategory?.name ?: "Select Category")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categoriesList.forEach { category ->
                            DropdownMenuItem(
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                },
                                text = { Text(text = category.name) }
                            )
                        }
                    }
                } else {
                    Text(text = "Amount: $${"%.2f".format(currentTransaction!!.amount)}", fontWeight = FontWeight.Bold)
                    Text(text = "Vendor: ${currentTransaction!!.vendor}", fontWeight = FontWeight.Bold)
                    Text(text = "Category: ${selectedCategory?.name ?: "N/A"}", fontWeight = FontWeight.Bold)
                    Text(text = "Date: ${formatDate(currentTransaction!!.date)}", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (isEditMode) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            currentTransaction?.let { txn ->
                                if (txn.id == null) {
                                    Log.e("TransactionDetail", "Transaction id is null; update skipped.")
                                } else {
                                    try {
                                        val updatedTransaction = Transaction(
                                            id = txn.id,
                                            amount = editedAmount.toDoubleOrNull() ?: txn.amount,
                                            category_id = selectedCategory?.id ?: txn.category_id,
                                            transaction_type = txn.transaction_type,
                                            vendor = editedVendor,
                                            date = isoFormat.format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate)!!),
                                            note = txn.note
                                        )
                                        val response = RetrofitInstance.apiService.updateTransaction(txn.id, updatedTransaction)
                                        if (response.isSuccessful) {
                                            // Update local state and send update to history screen.
                                            currentTransaction = updatedTransaction
                                            onTransactionUpdated(updatedTransaction)
                                            isEditMode = false
                                        } else {
                                            Log.e("TransactionDetail", "Update failed: ${response.errorBody()?.string()}")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("TransactionDetail", "Exception during update: ${e.message}")
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes")
                }
            } else {
                Button(
                    onClick = { currentTransaction?.let { onSplitClick(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF228B22))
                ) {
                    Text("Split Bill")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { onBackClick() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Back to History")
                }
            }
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            if (date != null) outputFormat.format(date) else dateString
        } catch (e: Exception) {
            dateString
        }
    }
}
