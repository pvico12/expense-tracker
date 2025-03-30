package com.cs446.expensetracker

import android.util.Log
import android.widget.DatePicker
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
import androidx.navigation.compose.rememberNavController
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.PickerActions
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.auth0.jwt.JWT
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.DealRetrievalRequestWithUser
import com.cs446.expensetracker.api.models.DealRetrievalResponse
import com.cs446.expensetracker.api.models.FcmTokenUploadRequest
import com.cs446.expensetracker.api.models.LoginRequest
import com.cs446.expensetracker.session.UserSession
import com.cs446.expensetracker.ui.deals.AddDealScreen
import com.cs446.expensetracker.ui.ui.theme.Pink40
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.equalTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AddDealUITests {
    private var dealScreenReturned = false
    private val calendar: Calendar = Calendar.getInstance()
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
    private var currentDate = isoFormat.format(calendar.time).substringBeforeLast(("T"))

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun clickAdd() {
        composeTestRule.onNodeWithText("Add").performClick()
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

            val dealRequest = DealRetrievalRequestWithUser(
                user_id = UserSession.userId,
            )
            val dealsResponse: Response<List<DealRetrievalResponse>> =
                RetrofitInstance.apiService.getDeals(dealRequest)
            if (dealsResponse.isSuccessful) {
                val responseBody = dealsResponse.body()
                val listOfDeals = responseBody?.map { x ->
                    DealRetrievalResponse(
                        id = x.id,
                        name = x.name,
                        description = x.description,
                        vendor = x.vendor,
                        price = x.price,
                        date = x.date,
                        address = x.address,
                        longitude = x.longitude,
                        latitude = x.latitude,
                        upvotes = x.upvotes,
                        downvotes = x.downvotes,
                        user_vote = x.user_vote,
                        maps_link = x.maps_link
                    )
                } ?: emptyList()
            }
        }

        composeTestRule.setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Pink40
            ) {
                AddDealScreen(rememberNavController(), -1) { dealScreenReturned = true }
            }
        }
    }

    private fun testDatePickerSelection(year: Int, month: Int, day: Int) {
        composeTestRule.onNodeWithText(currentDate).performClick()

        onView(withClassName(equalTo(DatePicker::class.java.name)))
            .perform(PickerActions.setDate(year, month, day))

        onView(withText("OK")).perform(click())

        val expectedDate = "$year-${String.format("%02d", month)}-${String.format("%02d", day)}"
        composeTestRule.onNodeWithText(expectedDate).assertExists()
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

    private fun performAdd(name: String, description: String, vendor: String, price: String, year: Int, month: Int, day: Int, address: String) {
        composeTestRule.onNodeWithText("Item Name").performTextReplacement(name)
        composeTestRule.onNodeWithText("Vendor").performTextReplacement(vendor)
        composeTestRule.onNodeWithText("Description").performTextReplacement(description)
        composeTestRule.onNodeWithText("Price").performTextReplacement(price)
        testDatePickerSelection(year, month, day)
        testAutoCompleteSelection(address)

        clickAdd()

        val latch = CountDownLatch(1)
        latch.await(2, TimeUnit.SECONDS)
    }

    @Test
    fun testAddDeal_noName() {
        performAdd("",
            "1 box",
            "T&T",
            "1",
            2025,
            3,
            29,
            "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada")

        composeTestRule.onNode(hasText("Please add an item name", substring = true)).assertExists()

        assert(!dealScreenReturned)
    }

    @Test
    fun testAddDeal_noDescription() {
        performAdd("Grapes",
            "",
            "T&T",
            "1",
            2025,
            3,
            29,
            "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada")

        composeTestRule.onNode(hasText("Please add a description", substring = true)).assertExists()

        assert(!dealScreenReturned)
    }

    @Test
    fun testAddDeal_noVendor() {
        performAdd("Grapes",
            "1 box",
            "",
            "1",
            2025,
            3,
            29,
            "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada")

        composeTestRule.onNode(hasText("Please add a vendor name", substring = true)).assertExists()

        assert(!dealScreenReturned)
    }

    @Test
    fun testAddDeal_negPrice() {
        performAdd("Grapes",
            "1 box",
            "T&T",
            "-1",
            2025,
            3,
            29,
            "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada")

        composeTestRule.onNode(hasText("Please have price be above 0", substring = true)).assertExists()

        assert(!dealScreenReturned)
    }

    @Test
    fun testAddDeal_noPrice() {
        performAdd("Grapes",
            "1 box",
            "T&T",
            "",
            2025,
            3,
            29,
            "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada")

        composeTestRule.onNode(hasText("Please set a numerical price", substring = true)).assertExists()

        assert(!dealScreenReturned)
    }

    @Test
    fun testAddDeal_wrongPriceFormat() {
        performAdd("Grapes",
            "1 box",
            "T&T",
            "s10",
            2025,
            3,
            29,
            "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada")

        composeTestRule.onNode(hasText("Please set a numerical price", substring = true)).assertExists()

        assert(!dealScreenReturned)
    }

    @Test
    fun testAddDeal_noAddress() {
        performAdd("Grapes",
            "1 box",
            "T&T",
            "1.99",
            2025,
            3,
            29,
            "")

        composeTestRule.onNode(hasText("Please pick an address from the autocomplete dropdown", substring = true)).assertExists()

        assert(!dealScreenReturned)
    }

    @Test
    fun testAddDeal_invalidAddress() {
        performAdd("Grapes",
            "1 box",
            "T&T",
            "1.99",
            2025,
            3,
            29,
            "afafafafaf")

        composeTestRule.onNode(hasText("Please pick an address from the autocomplete dropdown", substring = true)).assertExists()

        assert(!dealScreenReturned)
    }

    @Test
    fun testAddDeal_valid() {
        performAdd("Grapes",
            "1 box",
            "T&T",
            "1",
            2025,
            3,
            29,
            "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada")

        // wait 2s
        val latch = CountDownLatch(1)
        latch.await(2, TimeUnit.SECONDS)

        assert(dealScreenReturned)
    }
}