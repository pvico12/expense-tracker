package com.cs446.expensetracker

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import com.auth0.jwt.JWT
import com.cs446.expensetracker.api.BaseAPIService
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.DealRetrievalResponse
import com.cs446.expensetracker.api.models.FcmTokenUploadRequest
import com.cs446.expensetracker.api.models.LoginRequest
import com.cs446.expensetracker.session.UserSession
import com.cs446.expensetracker.ui.dashboard.Dashboard
import com.google.android.gms.tasks.Tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Response

class DashboardUITests {

    private var useMockApi: Boolean = false

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun clickSave() {
        composeTestRule.onNodeWithText("Update").performClick()
    }

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
            val responses: Array<Response<out Any>>? = if (useMockApi) runBlocking { createDashboardMockAPIRequests() } else null
            val dashboard = Dashboard()

            Log.d("TEMPHERE", "setUp: $useMockApi")

            composeTestRule.setContent {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                    if(useMockApi) {
                        if (responses != null) {
                            dashboard.DashboardScreen(drawerState, rememberNavController(), true, responses)
                        }
                    } else {
                        dashboard.DashboardScreen(drawerState, rememberNavController())
                    }
                }
            }
        }
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
                .setResponseCode(200)
                .setBody(
                    """
            {
              "total_spend": 2500.00,
              "category_breakdown": [
                {
                  "category_name": "Test Housing",
                  "total_amount": 1200,
                  "percentage": 46.85466377440347,
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

            val mockLevelResponse = MockResponse()
                .setResponseCode(200)
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

            val mockCategoriesResponse = MockResponse()
                .setResponseCode(200)
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

            val mockGoalsResponse = MockResponse()
                .setResponseCode(200)
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
        useMockApi = true
        setUp()
//         this gives the api time to respond
//        val latch = CountDownLatch(1)
//        latch.await(5, TimeUnit.SECONDS)
        composeTestRule.waitForIdle()

//        assertNotNull(response)
//        assertEquals(123.45, response.body()?.total_spend)

        composeTestRule.onNodeWithTag("ErrorMessage").assertDoesNotExist()
        composeTestRule.onNodeWithTag("LoadingSpinner").assertDoesNotExist()
        composeTestRule.onNodeWithTag("totalSpending").assertExists()
    }







    // full integration tests

}
