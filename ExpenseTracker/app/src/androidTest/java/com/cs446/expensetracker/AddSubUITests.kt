package com.cs446.expensetracker

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.auth0.jwt.JWT
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.FcmTokenUploadRequest
import com.cs446.expensetracker.api.models.LoginRequest
import com.cs446.expensetracker.session.UserSession
import com.cs446.expensetracker.ui.deals.AddSubScreen
import com.cs446.expensetracker.ui.ui.theme.Pink40
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AddSubUITests {
    private var dealScreenReturned = false

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun clickAdd() {
        composeTestRule.onNodeWithText("Subscribe").performClick()
    }

    @Before
    fun setUp() = runBlocking {
        val response = RetrofitInstance.apiService.login(LoginRequest("jeni", "cs"))
        val loginResponse = response.body()
        if (loginResponse != null) {
            UserSession.isLoggedIn = true
            UserSession.access_token = loginResponse.access_token
            UserSession.refresh_token = loginResponse.refresh_token
            UserSession.role = loginResponse.role

            val jwt = JWT.decode(loginResponse.access_token)
            UserSession.userId = jwt.getClaim("user_id").asInt()

            if (UserSession.fcmToken != "") {
                RetrofitInstance.apiService.uploadFcmToken(FcmTokenUploadRequest(UserSession.fcmToken))
            }
        }

        composeTestRule.setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Pink40
            ) {
                AddSubScreen(-1) { dealScreenReturned = true }
            }
        }
    }

    private fun testAutoCompleteSelection(address: String) {
        composeTestRule.onNodeWithTag("AutoCompleteField").performTextReplacement(address)

        try {
            composeTestRule.waitUntil(
                condition = {
                    composeTestRule.onAllNodes(hasTestTag("AutoCompleteItem"))
                        .fetchSemanticsNodes().isNotEmpty()
                },
                timeoutMillis = 5000
            )

            composeTestRule.onAllNodes(hasTestTag("AutoCompleteItem"))
                .onFirst()
                .performClick()
        } catch (e: ComposeTimeoutException) {
            Log.d("Test", "No AutoCompleteItem nodes found within timeout.")
        }
    }

    private fun performAdd(address: String) {
        testAutoCompleteSelection(address)
        clickAdd()

        val latch = CountDownLatch(1)
        latch.await(2, TimeUnit.SECONDS)
    }

    @Test
    fun testAddSub_noAddress() {
        performAdd("")

        composeTestRule.onNode(hasText("Please pick an address from the autocomplete dropdown", substring = true)).assertExists()

        assert(!dealScreenReturned)
    }

    @Test
    fun testAddSub_invalidAddress() {
        performAdd("afafafafaf")

        composeTestRule.onNode(hasText("Please pick an address from the autocomplete dropdown", substring = true)).assertExists()

        assert(!dealScreenReturned)
    }

    @Test
    fun testAddDeal_valid() {
        performAdd("T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada")

        assert(dealScreenReturned)
    }
}