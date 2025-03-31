package com.cs446.expensetracker

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.espresso.Espresso
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
import com.cs446.expensetracker.ui.dashboard.AddGoalScreen
import com.cs446.expensetracker.ui.dashboard.Dashboard
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

class AddGoalScreenUITests {

    private var useMockApi: String = "default"
    private var mockApiResponseCode: Int = 200

    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var SavedSpendingSummaryResponse: Response<SpendingSummaryResponse>
    lateinit var SavedLevelResponse: Response<LevelRequest>
    lateinit var SavedCategoriesResponse: Response<List<Category>>
    lateinit var SavedGoalsResponse: Response<GoalRetrievalResponse>
    lateinit var rootNavController: NavHostController

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
            val responses: Array<Response<out Any>>? = if (useMockApi != "false") runBlocking { createDashboardMockAPIRequests() } else null
            val dashboard = Dashboard()

            val failedEditResponses: Array<Response<out Any>>? = if (useMockApi == "api fail in edit") runBlocking { createDashboardMockAPIRequests(failGoalsAPI = true) } else null

            composeTestRule.setContent {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    rootNavController = rememberNavController()
                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                    NavHost(rootNavController, startDestination = "home") {
                        composable("home") {
                            if(useMockApi != "false") {
                                if (responses != null) {
                                    dashboard.DashboardScreen(drawerState, rootNavController, useMockApi, responses)
                                }
                            } else {
                                dashboard.DashboardScreen(drawerState, rootNavController)
                            }
                        }
                        composable("addGoalScreen/{editVersion}") { backStackEntry ->
                            val failedEditResponse: Array<Response<out Any>>?
                            if(useMockApi == "api fail in edit") {
                                AddGoalScreen(navController = rootNavController, useMockApi = useMockApi, createMockAddGoalApiRequests = failedEditResponses, editVersion=backStackEntry.arguments?.getString("editVersion")?.toInt() ?: -1)
                            } else {
                                AddGoalScreen(navController = rootNavController, useMockApi = useMockApi, createMockAddGoalApiRequests = responses, editVersion=backStackEntry.arguments?.getString("editVersion")?.toInt() ?: -1)
                            }
                        }
                    }
                }
            }
            composeTestRule.waitForIdle()
        }
    }

    // runblocking halts main thread, don't do that in dashboard
    private suspend fun createDashboardMockAPIRequests(failGoalsAPI: Boolean = false): Array<Response<out Any>> {
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
                {
                  "category_name": "Test Housing",
                  "total_amount": 1200,
                  "percentage": ${if (useMockApi == "incorrect API") "wrong string" else 46.85466377440347},
                  "color": "#FFDBAD8C"
                },
                {
                  "category_name": "Test Savings",
                  "total_amount": 800,
                  "percentage": 27.765726681127983,
                  "color": "#FFFFDADA"
                },
                {
                  "category_name": "Test Entertainment",
                  "total_amount": 280,
                  "percentage": 9.718004338394794,
                  "color": "#FF9A3B3B"
                },
                {
                  "category_name": "Test Transportation",
                  "total_amount": 220,
                  "percentage": 7.809110629067245,
                  "color": "#FFD6CBAF"
                }
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
                .setResponseCode(if (failGoalsAPI) 403 else mockApiResponseCode)
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

    // ui tests
    @Test
    fun testAddGoalScreenUponFirstLoading() = runTest {
        useMockApi = "default"
        setUp()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 8000) {
        composeTestRule.onAllNodes(hasTestTag("LoadingSpinner")).fetchSemanticsNodes().isEmpty()
    }

        val toggleButton = composeTestRule.onAllNodesWithTag("ToggleSpendingGoalsButton")

        toggleButton.assertAll(hasClickAction())

        toggleButton[0].performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("AddGoalButton"))

        composeTestRule.onNodeWithTag("AddGoalButton").performClick()
        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onAllNodes(hasTestTag("CategoryPicker")).fetchSemanticsNodes().isNotEmpty()
        }

        assert(SavedGoalsResponse.body() != null)
        assert(SavedCategoriesResponse.body() != null)

        composeTestRule.onNodeWithTag("GoalDropdownMenu").assertExists()
        composeTestRule.onNodeWithTag("GoalDropdownMenu").assertHasClickAction()
        composeTestRule.onNodeWithTag("PeriodDropdownMenu").assertExists()
        composeTestRule.onNodeWithTag("PeriodDropdownMenu").assertHasClickAction()
        composeTestRule.onNodeWithTag("CategoryPicker").assertExists()
        composeTestRule.onNodeWithTag("CategoryPicker").assertHasClickAction()
        composeTestRule.onNodeWithTag("LimitField").assertExists()
        composeTestRule.onNodeWithTag("LimitField").assertHasClickAction()
        composeTestRule.onNodeWithTag("DatePicker").assertExists()
        composeTestRule.onNodeWithTag("DatePicker").assertHasClickAction()
        composeTestRule.onNodeWithTag("TitleText").assertExists()
        assertEquals(composeTestRule.onNodeWithTag("TitleText").fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text, "Submit a Goal")
        composeTestRule.onNodeWithTag("ClosePage").assertExists()
        composeTestRule.onNodeWithTag("ClosePage").assertHasClickAction()
        composeTestRule.onNodeWithTag("SaveButton").assertExists()
        composeTestRule.onNodeWithTag("SaveButton").assertHasClickAction()

    }

    // ui tests
    @Test
    fun testEditGoalScreenUponFirstLoading() = runTest {
        useMockApi = "default"
        setUp()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onAllNodes(hasTestTag("LoadingSpinner")).fetchSemanticsNodes().isEmpty()
        }

        val toggleButton = composeTestRule.onAllNodesWithTag("ToggleSpendingGoalsButton")

        toggleButton.assertAll(hasClickAction())

        toggleButton[0].performClick()
        composeTestRule.waitForIdle()

        val cardEditNodes = composeTestRule.onAllNodes(
            hasTestTag("EditButton") and hasClickAction()
        )

        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("EditButton"))
        cardEditNodes[0].performClick()

        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onAllNodes(hasTestTag("CategoryPicker")).fetchSemanticsNodes().isNotEmpty()
        }

        assert(SavedGoalsResponse.body() != null)
        assert(SavedCategoriesResponse.body() != null)

        assertEquals(composeTestRule.onNodeWithTag("TitleText").fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text, "Edit Goal")

        composeTestRule.onNodeWithTag("GoalDropdownMenu").assertExists()
        composeTestRule.onNodeWithTag("PeriodDropdownMenu").assertExists()
        composeTestRule.onNodeWithTag("CategoryPicker").assertExists()
        composeTestRule.onNodeWithTag("LimitField").assertExists()
        composeTestRule.onNodeWithTag("DatePicker").assertExists()
        composeTestRule.onNodeWithTag("TitleText").assertExists()
        composeTestRule.onNodeWithTag("ClosePage").assertExists()
        composeTestRule.onNodeWithTag("SaveButton").assertExists()

        composeTestRule.onNodeWithText("Test Housing").assertExists()
        composeTestRule.onNodeWithText("percentage").assertExists()
        composeTestRule.onNodeWithText("Month").assertExists()
        composeTestRule.onNodeWithText("10.0").assertExists()
        composeTestRule.onNodeWithText("2025-03-01").assertExists()
    }

    // full integration tests
    @Test
    fun testAddGoal() {
        useMockApi = "default"
        setUp()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onAllNodes(hasTestTag("LoadingSpinner")).fetchSemanticsNodes().isEmpty()
        }

        val toggleButton = composeTestRule.onNodeWithTag("ToggleSpendingGoalsButton")

        toggleButton.assert(hasClickAction())

        toggleButton.performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("AddGoalButton"))

        composeTestRule.onNodeWithTag("AddGoalButton").performClick()
        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onAllNodes(hasTestTag("CategoryPicker")).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("CategoryPicker").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Test Housing").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("GoalDropdownMenu").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithTag("PercentageDropdownMenuItem")[0].performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("PeriodDropdownMenu").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithTag("PeriodDropdownMenuItem")[0].performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("LimitField").performTextReplacement("10.0")
        composeTestRule.waitForIdle()

        toggleButton.assertDoesNotExist()

        composeTestRule.onNodeWithTag("SaveButton").performClick()
        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onAllNodes(hasTestTag("ToggleSpendingGoalsButton")).fetchSemanticsNodes().isNotEmpty()
        }

        toggleButton.assertExists()

        // datepicker needs to be skipped

    }

    @Test
    fun testEditGoal() = runTest {
        useMockApi = "default"
        setUp()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onAllNodes(hasTestTag("LoadingSpinner")).fetchSemanticsNodes().isEmpty()
        }

        val toggleButton = composeTestRule.onNodeWithTag("ToggleSpendingGoalsButton")

        toggleButton.assert(hasClickAction())

        toggleButton.performClick()
        composeTestRule.waitForIdle()

        val cardEditNodes = composeTestRule.onAllNodes(
            hasTestTag("EditButton") and hasClickAction()
        )

        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("EditButton"))
        cardEditNodes[0].performClick()

        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onAllNodes(hasTestTag("CategoryPicker")).fetchSemanticsNodes().isNotEmpty()
        }

        assert(SavedGoalsResponse.body() != null)
        assert(SavedCategoriesResponse.body() != null)

        assertEquals(composeTestRule.onNodeWithTag("TitleText").fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text, "Edit Goal")

        assert(SavedGoalsResponse.body() != null)

        composeTestRule.onNodeWithText("Test Housing").assertExists() // make sure we actually pick something diff

        composeTestRule.onNodeWithTag("CategoryPicker").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Test Savings").performClick()
        composeTestRule.waitForIdle()

        toggleButton.assertDoesNotExist()

        composeTestRule.onNodeWithTag("SaveButton").performClick()
        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onAllNodes(hasTestTag("ToggleSpendingGoalsButton")).fetchSemanticsNodes().isNotEmpty()
        }

        toggleButton.assertExists()

    }

    @Test
    fun testIncorrectInputs() {
        useMockApi = "default"
        setUp()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onAllNodes(hasTestTag("LoadingSpinner")).fetchSemanticsNodes().isEmpty()
        }

        val toggleButton = composeTestRule.onNodeWithTag("ToggleSpendingGoalsButton")

        toggleButton.assert(hasClickAction())

        toggleButton.performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("AddGoalButton"))

        composeTestRule.onNodeWithTag("AddGoalButton").performClick()
        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onAllNodes(hasTestTag("CategoryPicker")).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("LimitField").performTextReplacement("anything")
        composeTestRule.waitForIdle()

        toggleButton.assertDoesNotExist()
        composeTestRule.onNodeWithTag("ErrorMessage").assertDoesNotExist()

        Espresso.closeSoftKeyboard()

        composeTestRule.onNodeWithTag("SaveButton").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("ErrorMessage").assertExists()

        composeTestRule.onNodeWithTag("LimitField").performTextReplacement("20.0")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("CategoryPicker").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Test Savings").performClick()
        composeTestRule.waitForIdle()

        Espresso.closeSoftKeyboard()

        composeTestRule.onNodeWithTag("SaveButton").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onAllNodes(hasTestTag("ToggleSpendingGoalsButton")).fetchSemanticsNodes().isNotEmpty()
        }

        toggleButton.assertExists()

    }

    @Test
    fun testAddPageFailedAPIRequest() {
        useMockApi = "api fail in edit"
        setUp()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onAllNodes(hasTestTag("LoadingSpinner")).fetchSemanticsNodes().isEmpty()
        }

        val toggleButton = composeTestRule.onNodeWithTag("ToggleSpendingGoalsButton")

        toggleButton.assert(hasClickAction())

        toggleButton.performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("AddGoalButton"))

        composeTestRule.onNodeWithTag("AddGoalButton").performClick()
        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onAllNodes(hasTestTag("ErrorMessage")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()

        val errorScreen = composeTestRule.onNodeWithTag("ErrorMessage").onChildren().fetchSemanticsNodes()
        assert(errorScreen.size == 2)
        assertEquals(errorScreen[0].config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text, "Failed to load goals.\n")
        assertEquals(errorScreen[1].config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text, "Retry")
    }

    @Test
    fun testDeleteGoal() = runTest {
        useMockApi = "default"
        setUp()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onAllNodes(hasTestTag("LoadingSpinner")).fetchSemanticsNodes().isEmpty()
        }

        val toggleButton = composeTestRule.onNodeWithTag("ToggleSpendingGoalsButton")

        toggleButton.assert(hasClickAction())

        toggleButton.performClick()
        composeTestRule.waitForIdle()

        val cardDeleteNodes = composeTestRule.onAllNodes(
            hasTestTag("DeleteButton") and hasClickAction()
        )

        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("DeleteButton"))
        cardDeleteNodes[0].performClick()
        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onAllNodes(hasTestTag("ConfirmDeleteDialog")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("ConfirmDeleteDialog").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ConfirmDeleteDialog").assertDoesNotExist()

    }

}
