package com.cs446.expensetracker.ui.dashboard

import com.cs446.expensetracker.api.models.CategoryBreakdown

class CategoryBreakdownDecorator(private val categoryBreakdown: CategoryBreakdown) {

    private val defaultColors = listOf("#FF9A3B3B", "#FFC08261", "#FFDBAD8C", "#FFFFEBCF")

    val category_name: String get() = categoryBreakdown.category_name
    val total_amount: Double get() = categoryBreakdown.total_amount
    val percentage: Double get() = categoryBreakdown.percentage
    val color: String
        get() = categoryBreakdown.color ?: assignDynamicColor()

    private fun assignDynamicColor(): String {
        val hash = category_name.hashCode()
        return defaultColors[hash % defaultColors.size]
    }
}