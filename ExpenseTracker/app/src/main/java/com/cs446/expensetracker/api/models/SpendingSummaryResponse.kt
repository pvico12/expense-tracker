package com.cs446.expensetracker.api.models

data class SpendingSummaryResponse(
    val total_spend: Double,
    val category_breakdown: List<CategoryBreakdown>,
    val transaction_history: List<TransactionResponse>,
)

data class CategoryBreakdown(
    val category_name: String,
    val total_amount: Double,
    val percentage: Double,
    var color: String?
)

