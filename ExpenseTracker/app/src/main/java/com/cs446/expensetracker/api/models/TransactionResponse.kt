package com.cs446.expensetracker.api.models

import com.google.gson.annotations.SerializedName

data class TransactionResponse(
    val id: Int,
    val userId: Int,
    val amount: Double,
    @SerializedName("category_id")
    val categoryId: Int,
    val transactionType: String?,
    val note: String?,
    val date: String?,
    val vendor: String?
)
