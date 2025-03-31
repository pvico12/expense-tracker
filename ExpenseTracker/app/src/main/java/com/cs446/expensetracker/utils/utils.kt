package com.cs446.expensetracker.utils

import android.icu.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

fun formatCurrency(amount: Double): String {
    if (amount == 0.0) { return "0.00"}
    val locale = DecimalFormatSymbols(Locale.US)
    val format = java.text.DecimalFormat("#,###.00", locale)
    return format.format(amount)
}