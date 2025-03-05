package com.cs446.expensetracker.models

data class Transaction(
    val amount: Double,
    val category_id: Int,
    val transaction_type: String = "expense", // Default to "expense"
    val note: String,
    val date: String
)
