package com.cs446.expensetracker

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import androidx.navigation.compose.rememberNavController
import com.cs446.expensetracker.ui.AddExpenseScreen
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import androidx.activity.ComponentActivity
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


class AddExpenseUITests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

//    @Before
//    fun Setup() {
//        composeTestRule.setContent {
//            AddExpenseScreen(navController = rememberNavController())
//        }
//    }

    @Test
    fun testInitialState() {
        composeTestRule.setContent {
            AddExpenseScreen(navController = rememberNavController())
        }

        // Verify initial UI elements are present
        composeTestRule.onNodeWithText("Add Transaction").assertExists()
        composeTestRule.onNodeWithText("Amount").assertExists()
        composeTestRule.onNodeWithText("Item / Vendor Name ").assertExists()
        composeTestRule.onNodeWithText("Category").assertExists()
        composeTestRule.onNodeWithText("Add Custom Category").assertExists()
        composeTestRule.onNodeWithText("Date & Recurrence").assertExists()
        composeTestRule.onNodeWithText("Note (optional)").assertExists()
        composeTestRule.onNodeWithText("Scan Receipt").assertExists()
        composeTestRule.onNodeWithText("Upload CSV").assertExists()
        composeTestRule.onNodeWithText("Preview CSV Template").assertExists()
        composeTestRule.onNodeWithText("Save Transaction").assertExists()

        composeTestRule.onNodeWithText("Pick End Date").assertDoesNotExist()
        composeTestRule.onNodeWithText("Repeats: Weekly").assertDoesNotExist()
        composeTestRule.onNodeWithText("Save Recurring Transaction").assertDoesNotExist()
    }

    // Amount Input

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
    fun testExpenseAmountInput_rejectsInvalidInput() {
        composeTestRule.setContent {
            AddExpenseScreen(navController = rememberNavController())
        }

        composeTestRule.onNodeWithText("Please enter a valid non-negative amount").assertDoesNotExist()
        composeTestRule.onNodeWithText("Amount").performTextInput("abc")
        composeTestRule.onNodeWithText("Please enter a valid non-negative amount").assertExists()
    }

    // Vendor Input

    @Test
    fun testVendorInput() {
        composeTestRule.setContent {
            AddExpenseScreen(navController = rememberNavController())
        }

        composeTestRule.onNodeWithText("Item / Vendor Name ").performTextInput("Test Vendor")
        composeTestRule.onNodeWithText("Test Vendor").assertExists()
    }

    // Run AI Button

    @Test
    fun testRunAIButton_disabledWhenVendorIsEmpty() {
        composeTestRule.setContent {
            AddExpenseScreen(navController = rememberNavController())
        }

        // Should be disabled initially because vendor is empty
        composeTestRule.onNodeWithText("Run AI").assertIsNotEnabled()
    }

    @Test
    fun testRunAI_showsErrorMessageWhenFails() {
        composeTestRule.setContent {
            AddExpenseScreen(navController = rememberNavController())
        }

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
        composeTestRule.setContent {
            AddExpenseScreen(navController = rememberNavController())
        }

        // Tap on the category box to open bottom sheet
        composeTestRule.onNodeWithText("Tap to Open Bottom Sheet").performClick()

        // Wait for sheet to appear
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithText("Select a Category").fetchSemanticsNodes().isNotEmpty()
        }
    }

    // Add Custom Category

    @Test
    fun testAddCustomCategory_successfullyAddsCategory() {
        composeTestRule.setContent {
            AddExpenseScreen(navController = rememberNavController())
        }

        // Open custom category dialog
        composeTestRule.onNodeWithText("Add Custom Category").performClick()

        // Type category name
        composeTestRule.onNodeWithText("Category Name").performTextInput("Test Custom")
        composeTestRule.onNodeWithText("Test Custom").assertExists()

        // Input a valid color
        composeTestRule.onNodeWithText("Custom Color (Hex Code)").performTextClearance()
        composeTestRule.onNodeWithText("Custom Color (Hex Code)").performTextInput("#123ABC")
        composeTestRule.onNodeWithText("#123ABC").assertExists()

//        // Click Save
//        composeTestRule.onNodeWithText("Save").performClick()
//
//        // Check that the new category is selected and shown
//        composeTestRule.onNodeWithText("Test Custom").assertExists()
    }

    // Date & Recurrence
    @Test
    fun testInitialDate_isToday() {
        composeTestRule.setContent {
            AddExpenseScreen(navController = rememberNavController())
        }

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        composeTestRule.onNodeWithText(today).assertExists()
    }

    @Test
    fun testRecurringSwitch_showsEndDateAndRepeatOptions() {
        composeTestRule.setContent {
            AddExpenseScreen(navController = rememberNavController())
        }

        // Find the switch (only one toggleable node should exist)
        composeTestRule
            .onAllNodes(isToggleable())
            .onFirst()
            .performClick()

        // Wait for recomposition
        composeTestRule.waitForIdle()

        // End date button should appear
        composeTestRule.onNodeWithText("Pick End Date").assertExists()

        // Repeat period button should appear
        composeTestRule.onNodeWithText("Repeats: Weekly").assertExists()
    }

    @Test
    fun testRepeatDropdown_selectsCorrectOption() {
        composeTestRule.setContent {
            AddExpenseScreen(navController = rememberNavController())
        }

        // Enable the recurring switch
        composeTestRule
            .onAllNodes(isToggleable())
            .onFirst()
            .performClick()

        composeTestRule.waitForIdle()

        // Click the "Repeats:" dropdown button
        composeTestRule.onNodeWithText("Repeats: Weekly").performClick()

        // Click the "Monthly" option from the dropdown
        composeTestRule.onNodeWithText("Monthly", useUnmergedTree = true).performClick()

        // Assert that the dropdown now shows "Repeats: Monthly"
        composeTestRule.onNodeWithText("Repeats: Monthly", useUnmergedTree = true).assertExists()
    }


    @Test
    fun testSaveRecurringButton_disabledWhenEndDateIsMissing() {
        composeTestRule.setContent {
            AddExpenseScreen(navController = rememberNavController())
        }

        // Fill in amount and vendor to simulate user input
        composeTestRule.onNodeWithText("Amount").performTextInput("100")
        composeTestRule.onNodeWithText("Item / Vendor Name ").performTextInput("Coffee")

        // Find the switch (only one toggleable node should exist)
        composeTestRule
            .onAllNodes(isToggleable())
            .onFirst()
            .performClick()

        // Wait for recomposition
        composeTestRule.waitForIdle()

        // "Save Recurring Transaction" button should be disabled initially
        composeTestRule.onNodeWithText("Save Recurring Transaction").assertIsNotEnabled()
    }

    // Note

    @Test
    fun testTransactionNoteInput_updatesCorrectly() {
        composeTestRule.setContent {
            AddExpenseScreen(navController = rememberNavController())
        }

        val note = "This was for lunch 🍔"

        // Input the note
        composeTestRule.onNodeWithText("Note (optional)").performTextInput(note)

        // Assert the note is now shown
        composeTestRule.onNodeWithText(note).assertExists()
    }

    // Scan Receipt & Upload CSV

    @Test
    fun testScanReceiptButton_isDisplayedAndClickable() {
        composeTestRule.setContent {
            AddExpenseScreen(navController = rememberNavController())
        }

        // Find and click the "Scan Receipt" button
        composeTestRule
            .onNodeWithText("Scan Receipt")
            .assertExists()
            .assertHasClickAction()
    }

    @Test
    fun testUploadCsvButton_isDisplayedAndClickable() {
        composeTestRule.setContent {
            AddExpenseScreen(navController = rememberNavController())
        }

        // Find and click the "Upload CSV" button
        composeTestRule
            .onNodeWithText("Upload CSV")
            .assertExists()
            .assertHasClickAction()
    }



    @Test
    fun testPreviewCsvTemplateButton_showsDialog() {
        composeTestRule.setContent {
            AddExpenseScreen(navController = rememberNavController())
        }

        // Simulate clicking the preview template button
        composeTestRule
            .onNodeWithText("Preview CSV Template")
            .performClick()

        // Wait if async call populates the template
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("CSV Format Preview").fetchSemanticsNodes().isNotEmpty()
        }

        // Assert dialog title exists
        composeTestRule
            .onNodeWithText("CSV Format Preview")
            .assertExists()

        // Assert close button is visible
        composeTestRule
            .onNodeWithText("Close")
            .assertExists()
            .assertHasClickAction()

        // Assert the help button is present
        composeTestRule
            .onNodeWithText("What does each column mean?")
            .assertExists()

        // Click the "What does each column mean?" button
        composeTestRule.onNodeWithText("What does each column mean?")
            .assertExists()
            .performClick()

        // Verify the CSV explanation dialog appears
        composeTestRule.onNodeWithText("CSV Column Format Guide")
            .assertExists()

        // Verify one of the descriptions
        composeTestRule.onNodeWithText("amount 💵")
            .assertExists()


    }


}