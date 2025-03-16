package com.cs446.expensetracker.api.models

data class OcrResponse(
    val items: List<OcrItem>,
    val approx_subtotal: Double?,
    val approx_fees: Double?,
    val total: Double?
)
