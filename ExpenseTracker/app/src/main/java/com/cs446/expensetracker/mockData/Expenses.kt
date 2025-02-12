package com.cs446.expensetracker.mockData
import androidx.compose.ui.graphics.Color

data class MockExpenseJson(
    val total_spending: Double,
    val categories: Array<MockExpense<Any?>>,
)

data class MockExpense<Color>(
    val category: String,
    val percentage: Float,
    val amount: Float,
    val customColor: Color,
)

val dashboard_mock_expense = MockExpenseJson(
    total_spending = 2500.0,
    categories = arrayOf(
        MockExpense(
            category = "Housing",
            percentage = 34f,
            amount = 850f,
            customColor = Color(0xFF9A3B3B)
        ),
        MockExpense(
            category = "Transportation",
            percentage = 10f,
            amount = 250f,
            customColor = Color(0xFFC08261)
        ),
        MockExpense(
            category = "Food",
            percentage = 15f,
            amount = 375f,
            customColor = Color(0xFFDBAD8C)
        ),
        MockExpense(
            category = "Utilities",
            percentage = 7f,
            amount = 175f,
            customColor = Color(0xFFFFEBCF)
        ),
        MockExpense(
            category = "Fun",
            percentage = 12f,
            amount = 300f,
            customColor = Color(0xFFFFCFAC)
        ),
        MockExpense(
            category = "Save",
            percentage = 10f,
            amount = 250f,
            customColor = Color(0xFFFFDADA)
        ),
        MockExpense(
            category = "Misc",
            percentage = 10f,
            amount = 250f,
            customColor = Color(0xFFD6CBAF)
        ),
        MockExpense(
            category = "Custom 1",
            percentage = 2f,
            amount = 50f,
            customColor = Color(0xFF8D5F2E)
        )
    )
)