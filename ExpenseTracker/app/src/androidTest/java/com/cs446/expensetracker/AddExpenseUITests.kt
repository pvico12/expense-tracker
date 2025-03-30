package com.cs446.expensetracker

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import androidx.navigation.compose.rememberNavController
import com.cs446.expensetracker.ui.AddExpenseScreen
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AddExpenseUITests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun Setup() {
        composeTestRule.setContent {
            AddExpenseScreen(navController = rememberNavController())
        }
    }

    @Test
    fun testInitialState() {
        // Verify initial UI elements are present
        composeTestRule.onNodeWithText("Add Transaction").assertExists()
        composeTestRule.onNodeWithText("Amount").assertExists()
        composeTestRule.onNodeWithText("Item / Vendor Name ").assertExists()
        composeTestRule.onNodeWithText("Select a Category").assertExists()
        composeTestRule.onNodeWithText("Save Transaction").assertExists()
    }

    @Test
    fun testExpenseAmountInput_showsCorrectValue() {
        val inputAmount = "100.00"
        composeTestRule.onNodeWithText("Amount").performTextInput(inputAmount)
        composeTestRule.onNodeWithText(inputAmount).assertExists()
    }

    @Test
    fun testRunAIButton_disabledWhenVendorIsEmpty() {
        // Should be disabled initially because vendor is empty
        composeTestRule.onNodeWithText("Run AI").assertIsNotEnabled()
    }

    // Add more tests for categories, save button state, dialogs etc.
}