package com.cs446.expensetracker

import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cs446.expensetracker.api.models.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionHistoryNavControllerTests {

    @Test
    fun testTransactionNavigationToDetail() {
        // Create a TestNavHostController with the target context.
        val navController = TestNavHostController(
            InstrumentationRegistry.getInstrumentation().targetContext
        )
        // Add the ComposeNavigator so composable destinations can be handled.
        navController.navigatorProvider.addNavigator(ComposeNavigator())

        // Create a minimal navigation graph with "history" and "history/detail".
        val navGraph = navController.createGraph(startDestination = "history") {
            composable("history") { }
            composable("history/detail") { }
        }

        // Set the navigation graph on the main thread.
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            navController.graph = navGraph
        }

        // Variable to simulate saving the clicked transaction.
        var selectedTransaction: Transaction? = null

        // Define the onTransactionClick callback.
        val onTransactionClick: (Transaction) -> Unit = { txn ->
            selectedTransaction = txn
            navController.navigate("history/detail")
        }

        // Create a dummy transaction.
        val dummyTransaction = Transaction(
            id = 1,
            amount = 100.0,
            category_id = 1,
            transaction_type = "expense",
            note = "Dummy Transaction",
            date = "2022-01-01T00:00:00",
            vendor = "Dummy Vendor"
        )

        // Invoke the callback on the main thread.
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            onTransactionClick(dummyTransaction)
        }

        // Assert that the transaction was saved.
        assertNotNull(selectedTransaction)
        assertEquals(dummyTransaction, selectedTransaction)

        // Verify that the nav controller navigated to "history/detail" on the main thread.
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            assertEquals("history/detail", navController.currentBackStackEntry?.destination?.route)
        }
    }
}
