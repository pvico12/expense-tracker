package com.cs446.expensetracker

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import androidx.navigation.compose.rememberNavController
import com.cs446.expensetracker.ui.AddExpenseScreen
import org.junit.Rule
import org.junit.Test

class AddExpenseUITests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testExpenseAmountInput_showsCorrectValue() {
        composeTestRule.setContent {
            AddExpenseScreen(navController = rememberNavController())
        }

        val inputAmount = "100.00"
        composeTestRule.onNodeWithText("Amount").performTextInput(inputAmount)
        composeTestRule.onNodeWithText(inputAmount).assertExists()
    }

    @Test
    fun testRunAIButton_disabledWhenVendorIsEmpty() {
        composeTestRule.setContent {
            AddExpenseScreen(navController = rememberNavController())
        }

        // Should be disabled initially because vendor is empty
        composeTestRule.onNodeWithText("Run AI").assertIsNotEnabled()
    }

    // Add more tests for categories, save button state, dialogs etc.
}