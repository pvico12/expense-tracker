package com.cs446.expensetracker.api.models

data class RecurringTransactionRequest(
    val start_date: String, // ISO 8601 format
    val end_date: String,   // ISO 8601 format
    val note: String,
    val period: Int,        // Period in days
    val amount: Double,
    val category_id: Int,
    val transaction_type: String, // "expense" or "income"
    val vendor: String
)
