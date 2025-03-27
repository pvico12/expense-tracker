package com.cs446.expensetracker.mockData

data class Transaction(
    val amount: Double,
    val category_id: Int,
    val transaction_type: String = "expense", // Default to "expense"
    val note: String,
    val date: String // ISO 8601 format: "YYYY-MM-DDTHH:mm:ss.SSSZ"
)

val mockTransactions = listOf(
    Transaction(50.0, 1, "expense", "Groceries", "2024-02-10T10:30:00.000Z"),
    Transaction(15.75, 2, "food", "Coffee", "2024-02-09T08:45:00.000Z"),
    Transaction(120.30, 3, "bill", "Electronics", "2024-02-08T15:20:00.000Z"),
    Transaction(30.0, 4, "expense", "Transportation", "2024-02-07T12:10:00.000Z"),
    Transaction(75.0, 5, "food", "Dinner", "2024-02-06T19:00:00.000Z"),
)

