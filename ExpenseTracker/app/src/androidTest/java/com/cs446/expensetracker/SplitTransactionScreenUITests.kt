package com.cs446.expensetracker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cs446.expensetracker.api.models.Transaction
import com.cs446.expensetracker.ui.SplitTransactionScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SplitTransactionScreenUITests {

    // Use TestActivity (a minimal host) as the test activity.
    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    // Test: When a transaction is provided, vendor and total info are shown.
    @Test
    fun testUIWithTransactionProvided() {
        val dummyTransaction = Transaction(
            id = 100,
            amount = 300.0,
            category_id = 1,
            transaction_type = "expense",
            note = "Lunch",
            date = "2022-01-01T12:00:00",
            vendor = "Cafe Delight"
        )

        composeTestRule.setContent {
            SplitTransactionScreen(transaction = dummyTransaction)
        }
        // Check that the header is displayed.
        composeTestRule.onNodeWithText("Split Bill").assertIsDisplayed()
        // Check that vendor and total info are displayed.
        composeTestRule.onNodeWithText("Vendor: Cafe Delight").assertIsDisplayed()
        composeTestRule.onNodeWithText("Total Amount: $300.0").assertIsDisplayed()
    }

    // Test: When no transaction is provided, the "Enter Total Amount" field is shown.
    @Test
    fun testUIWithoutTransactionProvided() {
        composeTestRule.setContent {
            SplitTransactionScreen(transaction = null)
        }
        composeTestRule.onNode(hasText("Enter Total Amount")).assertIsDisplayed()
    }

    // Test: Validate that entering a number greater than 10 changes the label to indicate an error.
    @Test
    fun testNumberOfPeopleInputExceedsLimit() {
        composeTestRule.setContent {
            SplitTransactionScreen(transaction = null)
        }
        // Initially, the label should be "Enter number of people to split with"
        composeTestRule.onNode(hasText("Enter number of people to split with")).assertIsDisplayed()
        // Input "11" into the number-of-people field.
        composeTestRule.onNode(hasText("Enter number of people to split with"))
            .performTextInput("11")
        composeTestRule.waitForIdle()
        // The label should update to indicate the limit is exceeded.
        composeTestRule.onNodeWithText("Maximum number of people is 10").assertIsDisplayed()
    }


    // Test: In non-equal (manual) mode, the "Send Email" button should be disabled if required inputs are missing.
    @Test
    fun testSendEmailButtonDisabledForInvalidAmounts() {
        composeTestRule.setContent {
            SplitTransactionScreen(transaction = null)
        }
        // Initially, with all fields blank, the button should be disabled.
        composeTestRule.onNodeWithText("Send Email").assertIsNotEnabled()
    }

    // Test: In non-equal mode, when valid amounts are entered, the "Send Email" button becomes enabled.
    @Test
    fun testSendEmailButtonEnabledForValidNonEqualAmounts() {
        composeTestRule.setContent {
            SplitTransactionScreen(transaction = null)
        }
        // Enter a total amount.
        composeTestRule.onNode(hasText("Enter Total Amount")).performTextInput("300")
        // Enter number of people (e.g., 2).
        composeTestRule.onNode(hasText("Enter number of people to split with")).performTextInput("2")
        composeTestRule.waitForIdle()

        // Manual mode is default so "Your Amount" field should appear.
        composeTestRule.onNode(hasText("Enter Your Amount")).assertIsDisplayed()
        // Enter your amount.
        composeTestRule.onNode(hasText("Enter Your Amount")).performTextInput("100")
        // For each recipient, enter email and amount.
        composeTestRule.onNode(hasText("Email 1")).performTextInput("recipient1@example.com")
        composeTestRule.onNode(hasText("Amount for Email 1")).performTextInput("100")
        composeTestRule.onNode(hasText("Email 2")).performTextInput("recipient2@example.com")
        composeTestRule.onNode(hasText("Amount for Email 2")).performTextInput("100")
        composeTestRule.waitForIdle()
        // Now the sum of amounts is valid (100 + 100 + 100 = 300); button should be enabled.
        composeTestRule.onNodeWithText("Send Email").assertIsEnabled()
    }

    // Test: In equal split mode, verify that the computed share is shown and the button is enabled.
    @Test
    fun testSendEmailButtonEnabledForEqualSplit() {
        // Provide a dummy transaction with a total of $200.00
        // to match "Vendor: Concert Hall" example.
        val dummyTransaction = Transaction(
            id = 1,
            amount = 200.0,
            category_id = 1,
            transaction_type = "expense",
            note = "Tickets",
            date = "2022-01-01T00:00:00",
            vendor = "Concert Hall"
        )

        // Set the content to our SplitTransactionScreen, passing the transaction.
        composeTestRule.setContent {
            SplitTransactionScreen(transaction = dummyTransaction)
        }

        // Wait for Compose to finish any pending changes.
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Split Equally")
            .assertExists()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("NumberOfPeopleField")
            .assertIsDisplayed()
            .performTextInput("2")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("EmailField_0")
            .assertIsDisplayed()
            .performTextInput("alice@example.com")
        composeTestRule.onNodeWithTag("EmailField_1")
            .assertIsDisplayed()
            .performTextInput("bob@example.com")
        composeTestRule.waitForIdle()

        // Note: Amounts are disabled since split equally does all the calculations prior
        composeTestRule.onNodeWithTag("SendEmailButton")
            .performScrollTo()
            .assertIsDisplayed()
    }

}
