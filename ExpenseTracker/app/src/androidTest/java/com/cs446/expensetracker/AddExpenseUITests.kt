package com.cs446.expensetracker

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import androidx.navigation.compose.rememberNavController
import com.cs446.expensetracker.ui.AddExpenseScreen
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule


class AddExpenseUITests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

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
        composeTestRule.onNodeWithText("Category").assertExists()
        composeTestRule.onNodeWithText("Save Transaction").assertExists()
    }

    // Amount Input

    @Test
    fun testExpenseAmountInput_showsCorrectValue() {
        val inputAmount = "100.00"
        composeTestRule.onNodeWithText("Amount").performTextInput(inputAmount)
        composeTestRule.onNodeWithText(inputAmount).assertExists()
    }

    @Test
    fun testExpenseAmountInput_rejectsInvalidInput() {
        composeTestRule.onNodeWithText("Please enter a valid non-negative amount").assertDoesNotExist()
        composeTestRule.onNodeWithText("Amount").performTextInput("abc")
        composeTestRule.onNodeWithText("Please enter a valid non-negative amount").assertExists()
    }

    // Vendor Input

    @Test
    fun testVendorInput() {
        composeTestRule.onNodeWithText("Item / Vendor Name ").performTextInput("Test Vendor")
        composeTestRule.onNodeWithText("Test Vendor").assertExists()
    }

    // Run AI Button

    @Test
    fun testRunAIButton_disabledWhenVendorIsEmpty() {
        // Should be disabled initially because vendor is empty
        composeTestRule.onNodeWithText("Run AI").assertIsNotEnabled()
    }

    @Test
    fun testRunAI_showsErrorMessageWhenFails() {
        // Input a vendor name to enable the AI button
        composeTestRule.onNodeWithText("Item / Vendor Name ").performTextInput("UnknownVendor")

        // Click Run AI
        composeTestRule.onNodeWithText("Run AI").performClick()

        // Wait for error message to appear
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("❌ AI flopped. Time for manual mode! ").fetchSemanticsNodes().isNotEmpty()
        }

        // Assert the error is shown
        composeTestRule.onNodeWithText("❌ AI flopped. Time for manual mode! ").assertExists()
    }

    // Category
    @Test
    fun testCategorySelection_bottomSheetOpens() {
        // Tap on the category box to open bottom sheet
        composeTestRule.onNodeWithText("Tap to Open Bottom Sheet").performClick()

        // Wait for sheet to appear
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithText("Select a Category").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun testAddCustomCategory_successfullyAddsCategory() {
        // Open custom category dialog
        composeTestRule.onNodeWithText("Add Custom Category").performClick()

        // Type category name
        composeTestRule.onNodeWithText("Category Name").performTextInput("Test Custom")

        // Input a valid color
        composeTestRule.onNodeWithText("Custom Color (Hex Code)").performTextClearance()
        composeTestRule.onNodeWithText("Custom Color (Hex Code)").performTextInput("#123ABC")

        // Click Save
        composeTestRule.onNodeWithText("Save").performClick()

        // Check that the new category is selected and shown
        composeTestRule.onNodeWithText("Test Custom").assertExists()
    }



    // Add more tests for categories, save button state, dialogs etc.
}