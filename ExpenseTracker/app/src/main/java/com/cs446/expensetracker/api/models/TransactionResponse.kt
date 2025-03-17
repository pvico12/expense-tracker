package com.cs446.expensetracker.api.models

data class TransactionResponse(
    val id: Int,
    val userId: Int,
    val amount: Double,
    val categoryId: Int,
    val transactionType: String?,
    val note: String?,
    val date: String?,
    val vendor: String?
)
