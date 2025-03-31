package com.cs446.expensetracker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cs446.expensetracker.api.models.Transaction
import com.cs446.expensetracker.nav.TransactionNavContainer
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionNavContainerUITests {

    // Use TestActivity as the host for Compose tests.
    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    @Test
    fun testTransactionHistoryScreenDisplaysComponents() {
        composeTestRule.setContent {
            TransactionNavContainer().TransactionHistoryScreen(
                navController = rememberNavController(),
                onTransactionClick = {}
            )
        }
        // Check that header, search field and advanced filter toggle are visible.
        composeTestRule.onNodeWithText("Transaction History").assertIsDisplayed()
        composeTestRule.onNodeWithText("Search...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Advanced Filter (Date Range)").assertIsDisplayed()
    }

    @Test
    fun testAdvancedFilterToggleShowsDates() {
        composeTestRule.setContent {
            TransactionNavContainer().TransactionHistoryScreen(
                navController = rememberNavController(),
                onTransactionClick = {}
            )
        }
        // Initially, the date fields should not be visible.
        composeTestRule.onNodeWithText("Start Date").assertDoesNotExist()
        composeTestRule.onNodeWithText("End Date").assertDoesNotExist()

        // Tap the Advanced Filter toggle.
        composeTestRule.onNodeWithText("Advanced Filter (Date Range)").performClick()
        composeTestRule.waitForIdle()

        // Verify that the date fields appear.
        composeTestRule.onNodeWithText("Start Date").assertIsDisplayed()
        composeTestRule.onNodeWithText("End Date").assertIsDisplayed()
    }

    @Test
    fun testTransactionListItemClick() {
        var wasClicked = false
        val dummyTransaction = Transaction(
            id = 1,
            amount = 123.45,
            category_id = 2,
            transaction_type = "expense",
            note = "Grocery Shopping",
            date = "2022-01-01T12:00:00",
            vendor = "Supermarket"  // non-null, so "Supermarket" is displayed
        )

        composeTestRule.setContent {
            TransactionNavContainer().TransactionListItem(
                transaction = dummyTransaction,
                onClick = { wasClicked = true }
            )
        }

        // Since vendor is non-null, the item displays "Supermarket"
        composeTestRule.onNodeWithText("Supermarket").performClick()
        assertTrue("Transaction list item click callback was not invoked", wasClicked)
    }


    @Test
    fun testTransactionDetailScreenDisplaysTransaction() {
        val dummyTransaction = Transaction(
            id = 2,
            amount = 250.0,
            category_id = 3,
            transaction_type = "income",
            note = "Salary",
            date = "2022-01-15T00:00:00",
            vendor = "Company Inc."
        )

        composeTestRule.setContent {
            TransactionNavContainer().TransactionDetailScreen(
                transaction = dummyTransaction,
                onBackClick = {},
                onSplitClick = {},
                onTransactionUpdated = {}
            )
        }
        composeTestRule.waitForIdle()

        // Dump the semantics tree for debugging (optional)
        composeTestRule.onRoot().printToLog("TransactionDetailScreen")

        // Update the assertions to match the text as rendered
        composeTestRule.onNodeWithText("Transaction Details").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vendor: Company Inc.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Amount: $250.00").assertIsDisplayed()
    }



    @Test
    fun testTransactionNavHostInitialScreen() {
        composeTestRule.setContent {
            TransactionNavContainer().TransactionNavHost()
        }
        // Verify that when the NavHost is displayed, the initial "history" screen is shown.
        composeTestRule.onNodeWithText("Transaction History").assertIsDisplayed()
        // Also, check that the advanced filter toggle is present.
        composeTestRule.onNodeWithText("Advanced Filter (Date Range)").assertIsDisplayed()
    }
}
