package com.cs446.expensetracker.api.models

import java.io.Serializable

data class Transaction(
    val id: Int? = null,
    val amount: Double,
    val category_id: Int,
    val transaction_type: String = "expense", // Default to "expense"
    val note: String,
    val date: String, // Must be in ISO 8601 format: "YYYY-MM-DDTHH:mm:ss.SSSZ"
    val vendor: String? = null
) : Serializable
