package com.cs446.expensetracker.nav

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cs446.expensetracker.mockData.Transaction
import com.cs446.expensetracker.mockData.mockTransactions
import com.cs446.expensetracker.ui.ui.theme.Typography
import com.cs446.expensetracker.ui.ui.theme.mainBackgroundColor
import com.cs446.expensetracker.ui.ui.theme.mainTextColor
import java.text.SimpleDateFormat
import java.util.*

class DealsContainer {

    @Composable
    fun DealsNavHost() {
        val dealsNavController = rememberNavController()
        val scrollState = rememberScrollState()
        NavHost(dealsNavController, startDestination = "deals") {
            composable("deals") {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(mainBackgroundColor)
                        .verticalScroll(scrollState)
                    ,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Community Deals",
                        color = mainTextColor,
                        style = Typography.titleLarge,
                        modifier = Modifier
                            .padding(start = 16.dp, top = 16.dp).fillMaxWidth()
                    )
                    Text(
                        text = "Your Region is set to \n${"location"}",
                        textAlign = TextAlign.Center,
                        color = mainTextColor,
                        style = Typography.titleMedium,
                        modifier = Modifier
                            .padding(start = 16.dp, top = 16.dp)
                    )
//                    TransactionHistoryScreen(
//                        onTransactionClick = { transactionId ->
//                            dealsNavController.navigate("history/detail/$transactionId")
//                        }
//                    )
                }
            }

            composable("history/detail/{transactionId}") { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getString("transactionId")
                TransactionDetailScreen(
                    transactionId = transactionId ?: "Unknown",
                    onBackClick = { dealsNavController.popBackStack() }
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
            Spacer(modifier = Modifier.height(16.dp))

//            Column {
//                TransactionListItem(transaction, onClick = {
//                    onTransactionClick(transaction.date)
//                })
//            }
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
