package com.cs446.expensetracker.nav

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cs446.expensetracker.mockData.Transaction
import com.cs446.expensetracker.mockData.mockTransactions

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
                val transactionId = backStackEntry.arguments?.getString("transactionId")
                TransactionDetailScreen(
                    transactionId = transactionId ?: "Unknown",
                    onBackClick = { transactionNavController.popBackStack() }
                )
            }
        }
    }

    @Composable
    fun TransactionHistoryScreen(onTransactionClick: (String) -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Transaction History",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(mockTransactions) { transaction ->
                    TransactionListItem(transaction, onClick = {
                        onTransactionClick(transaction.date)
                    })
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
            shape = MaterialTheme.shapes.medium
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
                        text = formatTransactionDate(transaction.date), // format date
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Text(
                    text = "$${transaction.amount}",
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.transaction_type == "expense") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            }
        }
    }


    @Composable
    fun TransactionDetailScreen(transactionId: String, onBackClick: () -> Unit) {
        val transaction = mockTransactions.find { it.date == transactionId }

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {
            Text(
                text = "Transaction Details",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            transaction?.let {
                Text(text = "Amount: $${it.amount}", fontWeight = FontWeight.Bold)
                Text(text = "Category ID: ${it.category_id}", fontWeight = FontWeight.Bold)
                Text(text = "Type: ${it.transaction_type}", fontWeight = FontWeight.Bold)
                Text(text = "Note: ${it.note}", fontWeight = FontWeight.Bold)
                Text(text = "Date: ${it.date}", fontWeight = FontWeight.Bold)
            } ?: Text(text = "Transaction not found.")

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onBackClick() }) {
                Text("Back to History")
            }
        }
    }

    // Helper to format date
    private fun formatTransactionDate(isoDate: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(isoDate)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            "Invalid Date"
        }
    }
}
