package com.cs446.expensetracker.api.models

data class TransactionResponse(
    val id: Int,
    val userId: Int,
    val amount: Int,
    val categoryId: Int,
    val transactionType: String?,
    val note: String?,
    val date: String?
)
