package com.cs446.expensetracker.mockData

import android.icu.text.DecimalFormat

fun formatCurrency(amount: Double): String {
    if (amount == 0.0) { return "0.00"}
    val format = DecimalFormat("#,###.00")
    return format.format(amount)
}