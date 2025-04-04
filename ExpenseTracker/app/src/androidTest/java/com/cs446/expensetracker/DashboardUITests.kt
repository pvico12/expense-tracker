package com.cs446.expensetracker

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.navigation.compose.rememberNavController
import com.auth0.jwt.JWT
import com.cs446.expensetracker.api.BaseAPIService
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.Category
import com.cs446.expensetracker.api.models.FcmTokenUploadRequest
import com.cs446.expensetracker.api.models.GoalRetrievalResponse
import com.cs446.expensetracker.api.models.LevelRequest
import com.cs446.expensetracker.api.models.LoginRequest
import com.cs446.expensetracker.api.models.SpendingSummaryResponse
import com.cs446.expensetracker.session.UserSession
import com.cs446.expensetracker.ui.dashboard.Dashboard
import com.cs446.expensetracker.utils.formatCurrency
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Response
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.round

class DashboardUITests {

    private var useMockApi: String = "default"
    private var mockApiResponseCode: Int = 200

    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var SavedSpendingSummaryResponse: Response<SpendingSummaryResponse>
    lateinit var SavedLevelResponse: Response<LevelRequest>
    lateinit var SavedCategoriesResponse: Response<List<Category>>
    lateinit var SavedGoalsResponse: Response<GoalRetrievalResponse>

    fun setUp() = runTest {
        val response = RetrofitInstance.apiService.login(LoginRequest("admin", "admin"))
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

            // runs synchronously, ensures completion
            val responses: Array<Response<out Any>>? = if (useMockApi != "false") runBlocking { if (useMockApi != "empty API") createDashboardMockAPIRequests() else createEmptyDashboardMockAPIRequests() } else null
            val dashboard = Dashboard()

            composeTestRule.setContent {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                    if(useMockApi != "false") {
                        if (responses != null) {
                            dashboard.DashboardScreen(drawerState, rememberNavController(), useMockApi, responses)
                        }
                    } else {
                        dashboard.DashboardScreen(drawerState, rememberNavController())
                    }
                }
            }
        }
    }

    // runblocking halts main thread, don't do that in dashboard
    private suspend fun createEmptyDashboardMockAPIRequests(): Array<Response<out Any>> {
            val mockWebServer = MockWebServer()
            mockWebServer.start()

            val apiService = Retrofit.Builder()
                .baseUrl(mockWebServer.url("/"))  // Use the mock server's URL
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(BaseAPIService::class.java)
            // mock for spending summary

            val mockSpendingSummaryResponse = MockResponse()
                .setResponseCode(if (useMockApi == "fail one API Request") 403 else mockApiResponseCode)
                .setBody(
                    """
            {
              "total_spend": 0.0,
              "category_breakdown": [
               
            ],
            "transaction_history": [
                ]
            }
            """.trimIndent()
                )
            mockWebServer.enqueue(mockSpendingSummaryResponse)

            val ReturnedSpendingSummaryResponse = withContext(Dispatchers.IO) {
                apiService.getSpendingSummary("2025-03-01T00:00:00", "2025-03-31T23:59:59")
            }
            SavedSpendingSummaryResponse = ReturnedSpendingSummaryResponse

            val mockLevelResponse = MockResponse()
                .setResponseCode(mockApiResponseCode)
                .setBody(
                    """
            {
                "level": 1,
                "current_xp": 0,
                "remaining_xp_until_next_level": 5,
                "total_xp_for_next_level": 5
            }
            """.trimIndent()
                )
            mockWebServer.enqueue(mockLevelResponse)

            val ReturnedLevelResponse = withContext(Dispatchers.IO) {
                apiService.getLevel()
            }
            SavedLevelResponse = ReturnedLevelResponse

            val mockCategoriesResponse = MockResponse()
                .setResponseCode(mockApiResponseCode)
                .setBody(
                    """
            [
            ]
            """.trimIndent()
                )
            mockWebServer.enqueue(mockCategoriesResponse)

            val ReturnedCategoriesResponse = withContext(Dispatchers.IO) {
                apiService.getCategories()
            }
            SavedCategoriesResponse = ReturnedCategoriesResponse

            val mockGoalsResponse = MockResponse()
                .setResponseCode(mockApiResponseCode)
                .setBody(
                    """
            {
                "goals": [
                ],
                "stats": {
                    "completed": 0,
                    "incompleted": 0,
                    "failed": 0
                }
            }
            """.trimIndent()
                )
            mockWebServer.enqueue(mockGoalsResponse)

            val ReturnedGoalsResponse = withContext(Dispatchers.IO) {
                apiService.getGoals("2025-03-01T00:00:00", "2025-03-31T23:59:59")
            }
            SavedGoalsResponse = ReturnedGoalsResponse

            mockWebServer.shutdown()

            return arrayOf(
                ReturnedSpendingSummaryResponse,
                ReturnedLevelResponse,
                ReturnedCategoriesResponse,
                ReturnedGoalsResponse
            )

    }

    // runblocking halts main thread, don't do that in dashboard
    private suspend fun createDashboardMockAPIRequests(): Array<Response<out Any>> {
        val mockWebServer = MockWebServer()
        mockWebServer.start()

        val apiService = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))  // Use the mock server's URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BaseAPIService::class.java)
        // mock for spending summary

        val mockSpendingSummaryResponse = MockResponse()
            .setResponseCode(if (useMockApi == "fail one API Request") 403 else mockApiResponseCode)
            .setBody(
                """
            {
              "total_spend": 2500.00,
              "category_breakdown": [
            ],
            "transaction_history": [
                    {
                      "id": 3,
                      "user_id": 1,
                      "amount": 50.75,
                      "category_id": 2,
                      "transaction_type": "expense",
                      "note": "Dinner at Italian Restaurant",
                      "date": "2025-03-15T00:00:00",
                      "vendor": "Italian Bistro"
                    }
                ]
            }
            """.trimIndent()
            )
        mockWebServer.enqueue(mockSpendingSummaryResponse)

        val ReturnedSpendingSummaryResponse = withContext(Dispatchers.IO) {
            apiService.getSpendingSummary("2025-03-01T00:00:00", "2025-03-31T23:59:59")
        }
        SavedSpendingSummaryResponse = ReturnedSpendingSummaryResponse

        val mockLevelResponse = MockResponse()
            .setResponseCode(mockApiResponseCode)
            .setBody(
                """
            {
                "level": 1,
                "current_xp": 1,
                "remaining_xp_until_next_level": 4,
                "total_xp_for_next_level": 5
            }
            """.trimIndent()
            )
        mockWebServer.enqueue(mockLevelResponse)

        val ReturnedLevelResponse = withContext(Dispatchers.IO) {
            apiService.getLevel()
        }
        SavedLevelResponse = ReturnedLevelResponse

        val mockCategoriesResponse = MockResponse()
            .setResponseCode(mockApiResponseCode)
            .setBody(
                """
            [
              {
                "id": 1,
                "name": "Test Housing",
                "color": "#FFDBAD8C"
              },
              {
                "id": 2,
                "name": "Test Savings",
                "color": "#FFFFDADA"
              },
              {
                "id": 3,
                "name": "Test Entertainment",
                "color": "#FF9A3B3B"
              },
              {
                "id": 4,
                "name": "Test Transportation",
                "color": "#FFD6CBAF"
              }
            ]
            """.trimIndent()
            )
        mockWebServer.enqueue(mockCategoriesResponse)

        val ReturnedCategoriesResponse = withContext(Dispatchers.IO) {
            apiService.getCategories()
        }
        SavedCategoriesResponse = ReturnedCategoriesResponse

        val mockGoalsResponse = MockResponse()
            .setResponseCode(mockApiResponseCode)
            .setBody(
                """
            {
                "goals": [
                    {
                      "id": 1,
                      "category_id": 1,
                      "goal_type": "percentage",
                      "limit": 10,
                      "start_date": "2025-03-01T00:00:00",
                      "end_date": "2025-03-31T23:59:59.999999",
                      "period": 31,
                      "on_track": false,
                      "time_left": 1,
                      "amount_spent": 0
                    },
                    {
                      "id": 16,
                      "category_id": 1,
                      "goal_type": "amount",
                      "limit": 30,
                      "start_date": "2025-03-30T02:57:18",
                      "end_date": "2025-04-28T02:57:18",
                      "period": 30,
                      "on_track": true,
                      "time_left": 28,
                      "amount_spent": 280
                    },
                    {
                      "id": 17,
                      "category_id": 1,
                      "goal_type": "amount",
                      "limit": 20,
                      "start_date": "2025-03-10T12:12:12",
                      "end_date": "2025-03-16T12:12:12",
                      "period": 31,
                      "on_track": true,
                      "time_left": 0,
                      "amount_spent": 18
                    },
                    {
                      "id": 18,
                      "category_id": 1,
                      "goal_type": "amount",
                      "limit": 30,
                      "start_date": "2025-03-10T12:12:12",
                      "end_date": "2025-03-16T12:12:12",
                      "period": 31,
                      "on_track": false,
                      "time_left": 0,
                      "amount_spent": 280
                    }
                ],
                "stats": {
                    "completed": 1,
                    "incompleted": 2,
                    "failed": 1
                }
            }
            """.trimIndent()
            )
        mockWebServer.enqueue(mockGoalsResponse)

        val ReturnedGoalsResponse = withContext(Dispatchers.IO) {
            apiService.getGoals("2025-03-01T00:00:00", "2025-03-31T23:59:59")
        }
        SavedGoalsResponse = ReturnedGoalsResponse

        mockWebServer.shutdown()

        return arrayOf(
            ReturnedSpendingSummaryResponse,
            ReturnedLevelResponse,
            ReturnedCategoriesResponse,
            ReturnedGoalsResponse
        )

    }

    @Test
    fun testDashboardUponFirstLoading(): Unit = runTest {
        useMockApi = "default"
        setUp()
//         this gives the api time to respond
//        val latch = CountDownLatch(1)
//        latch.await(5, TimeUnit.SECONDS)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("ErrorMessage").assertDoesNotExist()
        composeTestRule.onNodeWithTag("LoadingSpinner").assertDoesNotExist()

        composeTestRule.onNodeWithTag("Scaffold").assertExists()
        composeTestRule.onNodeWithTag("TotalSpending").assertExists()
        composeTestRule.onNodeWithTag("CatGif").assertExists()
        composeTestRule.onNodeWithTag("Piechart").assertExists()
        composeTestRule.onNodeWithTag("CatGifButton").assertExists()
        composeTestRule.onNodeWithTag("ToggleSpendingGoalsButton").assertExists()
        composeTestRule.onNodeWithTag("LevelBar").assertExists()

        composeTestRule.onNodeWithTag("CatGifButton").assertHasClickAction()
        composeTestRule.onNodeWithTag("ToggleSpendingGoalsButton").assertHasClickAction()

        assert(SavedSpendingSummaryResponse.isSuccessful)
        assert(SavedLevelResponse.isSuccessful)
        assert(SavedCategoriesResponse.isSuccessful)
        assert(SavedLevelResponse.isSuccessful)

        //goals
        composeTestRule.onNodeWithTag("AlertDialog").assertDoesNotExist()
        composeTestRule.onNodeWithTag("GoalHeader").assertDoesNotExist()
        composeTestRule.onNodeWithTag("GoalSearchBar").assertDoesNotExist()
        composeTestRule.onNodeWithTag("GoalCard").assertDoesNotExist()
        composeTestRule.onNodeWithTag("DeleteButton").assertDoesNotExist()
        composeTestRule.onNodeWithTag("EditButton").assertDoesNotExist()

        composeTestRule.onNode(hasScrollAction()).assertExists()
        composeTestRule.onNodeWithTag("BottomDashboard").assertExists()
        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("BottomDashboard"))
    }

    @Test
    fun testExpenseCards() {
        useMockApi = "default"
        setUp()
        composeTestRule.waitForIdle()

        val allExpenseCards = composeTestRule.onAllNodes(hasTestTag("ExpenseCategoryCard")).fetchSemanticsNodes()

        assert(allExpenseCards.size == SavedSpendingSummaryResponse.body()?.category_breakdown?.size)

        val cardNameNodes = composeTestRule.onAllNodes(
            hasTestTag("ExpenseCategoryCardName") and hasParent(hasTestTag("ExpenseCategoryCard"))
        ).fetchSemanticsNodes()

        val cardPercentageNodes = composeTestRule.onAllNodes(
            hasTestTag("ExpenseCategoryCardPercentage") and hasParent(hasTestTag("ExpenseCategoryCard"))
        ).fetchSemanticsNodes()

        val cardTotalAmountNodes = composeTestRule.onAllNodes(
            hasTestTag("ExpenseCategoryCardTotalAmount") and hasParent(hasTestTag("ExpenseCategoryCard"))
        ).fetchSemanticsNodes()

        assert(SavedSpendingSummaryResponse.body() != null)

        for (i in allExpenseCards.indices) {
            val SpendingCategory = SavedSpendingSummaryResponse.body()?.category_breakdown?.get(i)
            assertEquals(cardNameNodes[i].config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text, "${SpendingCategory?.category_name}: ")
            assertEquals(cardPercentageNodes[i].config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text, "${round(SpendingCategory!!.percentage)}%")
            assertEquals(cardTotalAmountNodes[i].config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text, "$${formatCurrency(SpendingCategory.total_amount)}")
        }

    }

    @Test
    fun testTotalSpending() {
        useMockApi = "default"
        setUp()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("TotalSpending").assertExists()
        val children = composeTestRule.onNode(hasTestTag("TotalSpending")).onChildren().fetchSemanticsNodes()

        assert(children.size == 3)


        assert(SavedSpendingSummaryResponse.body() != null)

        val totalSpending = SavedSpendingSummaryResponse.body()?.total_spend
        assertEquals(children[0].config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text, "$")
        if (totalSpending != null) {
            assertEquals(children[1].config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text, "${totalSpending.toInt()}")
            assertEquals(children[2].config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text, ".${((totalSpending - totalSpending.toInt()) * 100).toInt()}")
        }

    }

    @Test
    fun testCatGif() {
        useMockApi = "default"
        setUp()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("CatGif").assertExists()
        composeTestRule.onNodeWithTag("CatGifButton").assertExists()

        assertEquals(composeTestRule.onNode(hasTestTag("CatGifImage")).fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull().toString(), "neutral")

        composeTestRule.onNodeWithTag("CatGifButton").performClick()
        composeTestRule.waitForIdle()

        assertEquals(composeTestRule.onNode(hasTestTag("CatGifImage")).fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull().toString(), "on pet")

        // check it goes back after being pet
        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onNode(hasTestTag("CatGifImage"))
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.ContentDescription)
                ?.firstOrNull()
                .toString() == "neutral"
        }


    }

    @Test
    fun testLevelBar() {
        useMockApi = "default"
        setUp()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("LevelBar").assertExists()

        composeTestRule.onNodeWithTag("LevelIcon").assertExists()

        assert(SavedLevelResponse.body() != null)

        val actualBarNode = composeTestRule.onNode(
            hasProgressBarRangeInfo(rangeInfo = ProgressBarRangeInfo(SavedLevelResponse.body()!!.current_xp.toFloat() / SavedLevelResponse.body()!!.total_xp_for_next_level.toFloat(), 0f..1f)) and hasParent(hasTestTag("LevelBar"))
        )

        actualBarNode.assertExists()

        val actualIconNode = composeTestRule.onNode(hasTestTag("LevelIcon"))

        actualIconNode.assertExists()

//         make sure there's no awkward gap here
        assert(abs(actualBarNode.fetchSemanticsNode().positionOnScreen.x - actualIconNode.fetchSemanticsNode().positionOnScreen.y) <= 1000)

    }

    @Test
    fun testViewingGoals() {
        useMockApi = "default"
        setUp()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("ToggleSpendingGoalsButton").assertExists()
        composeTestRule.onNodeWithTag("ToggleSpendingGoalsButton").assertHasClickAction()

        val toggleButton = composeTestRule.onNodeWithTag("ToggleSpendingGoalsButton").fetchSemanticsNode()

        assertEquals(toggleButton.config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text, "View Goals")

        composeTestRule.onNodeWithTag("ToggleSpendingGoalsButton").performClick()
        composeTestRule.waitForIdle()

        assertEquals(toggleButton.config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text, "View Spending")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("ErrorMessage").assertDoesNotExist()
        composeTestRule.onNodeWithTag("LoadingSpinner").assertDoesNotExist()

        composeTestRule.onNodeWithTag("Scaffold").assertExists()
        composeTestRule.onNodeWithTag("TotalSpending").assertExists()
        composeTestRule.onNodeWithTag("CatGif").assertExists()
        composeTestRule.onNodeWithTag("Piechart").assertExists()
        composeTestRule.onNodeWithTag("CatGifButton").assertExists()
        composeTestRule.onNodeWithTag("ToggleSpendingGoalsButton").assertExists()
        composeTestRule.onNodeWithTag("LevelBar").assertExists()

        //goals
        composeTestRule.onNodeWithTag("AlertDialog").assertDoesNotExist()
        composeTestRule.onNodeWithTag("GoalHeader").assertExists()
        composeTestRule.onNodeWithTag("GoalSearchBar").assertExists()

        composeTestRule.onNodeWithTag("ExpenseCategoryCard").assertDoesNotExist()

        composeTestRule.onNode(hasScrollAction()).assertExists()
        composeTestRule.onNodeWithTag("BottomDashboard").assertExists()
        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("BottomDashboard"))
    }

    @Test
    fun testGoalCards() {
        useMockApi = "default"
        setUp()
        composeTestRule.waitForIdle()


        assert(SavedGoalsResponse.body() != null)

        composeTestRule.onNodeWithTag("ToggleSpendingGoalsButton").assertExists()
        composeTestRule.onNodeWithTag("ToggleSpendingGoalsButton").assertHasClickAction()

        val toggleButton = composeTestRule.onNodeWithTag("ToggleSpendingGoalsButton").fetchSemanticsNode()

        assertEquals(toggleButton.config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text, "View Goals")

        composeTestRule.onNodeWithTag("ToggleSpendingGoalsButton").performClick()
        composeTestRule.waitForIdle()

        val allGoalCards = composeTestRule.onAllNodes(hasTestTag("GoalCard")).fetchSemanticsNodes()

        assert(allGoalCards.size == SavedGoalsResponse.body()?.goals?.size)

        composeTestRule.onNode(hasScrollAction()).assertExists()
        composeTestRule.onNodeWithTag("BottomDashboard").assertExists()
        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("BottomDashboard"))

        val cardDeleteNodes = composeTestRule.onAllNodes(
            hasTestTag("DeleteButton") and hasClickAction()
        ).fetchSemanticsNodes()

        val cardEditNodes = composeTestRule.onAllNodes(
            hasTestTag("EditButton") and hasClickAction()
        ).fetchSemanticsNodes()

        assert(cardDeleteNodes.size == allGoalCards.size)
        assert(cardEditNodes.size == allGoalCards.size)

        val cardMainText = composeTestRule.onAllNodes(
            hasTestTag("MainGoalCardText")
        ).fetchSemanticsNodes()

        val cardSecondaryText = composeTestRule.onAllNodes(
            hasTestTag("SecondaryGoalCardText")
        ).fetchSemanticsNodes()

        // hardcoded because I think this is most likely to change
        assertEquals(cardMainText[3].config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text, "Spend less than $30.00 on Test Housing")
        assertEquals(cardSecondaryText[3].config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text, "$280.00 amount spent in set month")

    }


    // API failure tests
    @Test
    fun testAPIRequestFailsMultipleTimes() {
        useMockApi = "fail one API Request"
        setUp()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("ErrorMessage").assertExists()
        composeTestRule.onNodeWithTag("LoadingSpinner").assertDoesNotExist()

        val errorScreen = composeTestRule.onNodeWithTag("ErrorMessage").onChildren().fetchSemanticsNodes()
        assert(errorScreen.size == 2)
        assertEquals(errorScreen[0].config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text, "Failed to load expense data.\n")
        assertEquals(errorScreen[1].config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text, "Retry")

        composeTestRule.onNodeWithTag("ErrorMessage").onChildren()[1].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ErrorMessage").assertExists()
        composeTestRule.onNodeWithTag("LoadingSpinner").assertDoesNotExist()
    }

    @Test
    fun testAPIRequestLoading() {
        useMockApi = "slowdown"
        setUp()
        composeTestRule.onNodeWithTag("LoadingSpinner").assertExists()
        composeTestRule.onNodeWithTag("ErrorMessage").assertDoesNotExist()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onAllNodes(hasTestTag("LoadingSpinner")).fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onNodeWithTag("LoadingSpinner").assertDoesNotExist()
        composeTestRule.onNodeWithTag("ErrorMessage").assertDoesNotExist()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("TotalSpending").assertExists()
    }

    @Test
    fun testMultipleTypesOfFailedAPIRequests() {
        useMockApi = "default"
        mockApiResponseCode = 403
        setUp()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("ErrorMessage").assertExists()
        composeTestRule.onNodeWithTag("LoadingSpinner").assertDoesNotExist()

        val errorScreen = composeTestRule.onNodeWithTag("ErrorMessage").onChildren().fetchSemanticsNodes()
        assert(errorScreen.size == 2)
        assertEquals(errorScreen[0].config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text, "Failed to load expense data.\nFailed to load levels.\nFailed to load categories.\nFailed to load goals.\n")
        assertEquals(errorScreen[1].config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text, "Retry")

        composeTestRule.onNodeWithTag("TotalSpending").assertDoesNotExist()
    }

    @Test
    fun incorrectAPIResponses() {
        useMockApi = "incorrect API"
        try {
            setUp()
            assertEquals(0, 1)
        } catch (e: Exception) {
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("LoadingSpinner").assertDoesNotExist()
        }

    }

    @Test
    fun emptyAPIResponses() {
        useMockApi = "empty API"
        setUp()
        composeTestRule.waitForIdle()
    }

}
