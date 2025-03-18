package com.cs446.expensetracker.api.models

data class RecurringTransactionResponse(
    val id: Int,
    val start_date: String,
    val end_date: String,
    val note: String,
    val period: Int
)
