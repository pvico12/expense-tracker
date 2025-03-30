package com.cs446.expensetracker

import android.util.Log
import com.auth0.jwt.JWT
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.DealSubRetrievalResponse
import com.cs446.expensetracker.api.models.FcmTokenUploadRequest
import com.cs446.expensetracker.api.models.LoginRequest
import com.cs446.expensetracker.session.UserSession
import com.cs446.expensetracker.ui.deals.createSub
import com.cs446.expensetracker.ui.deals.deleteSub
import com.cs446.expensetracker.ui.deals.getSub
import com.cs446.expensetracker.ui.deals.updateSub
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class SubsUnitTests {
    private var lastSubId : Int = 1

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

            val subsResponse: Response<List<DealSubRetrievalResponse>> =
                RetrofitInstance.apiService.getSubs()
            if (subsResponse.isSuccessful) {
                val responseBody = subsResponse.body()
                Log.d("Response", "Subs Response: $responseBody")
                val listOfSubs = responseBody?.map { x ->
                    DealSubRetrievalResponse(
                        id = x.id,
                        user_id = x.user_id,
                        address = x.address,
                        longitude = x.longitude,
                        latitude = x.latitude
                    )
                } ?: emptyList()
                lastSubId = listOfSubs.lastOrNull()?.id ?: 1
            }
        }
    }

    // ==================== Add Sub ====================
    @Test
    fun testAddSub_noAddress() {
        runBlocking {
            val response = createSub(
                "",
                -80.5380946,
                43.4624441
            )
            assertEquals(false, response)
        }
    }

    @Test
    fun testAddSub_invalidLng() {
        runBlocking {
            val response = createSub(
                "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada",
                -200.0,
                43.4624441
            )
            assertEquals(false, response)
        }
    }

    @Test
    fun testAddSub_invalidLat() {
        runBlocking {
            val response = createSub(
                "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada",
                -80.5380946,
                100.4624441
            )
            assertEquals(false, response)
        }
    }

    @Test
    fun testAddSub_valid() {
        runBlocking {
            val response = createSub(
                "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada",
                -80.5380946,
                43.4624441
            )
            assertEquals(true, response)
        }
    }

    // ==================== Get Specific Sub ====================
    @Test
    fun testGetSub_wrongId() {
        runBlocking {
            val response = getSub(-1)
            assertNull(response)
        }
    }

    @Test
    fun testGetSub_valid() {
        runBlocking {
            val response = getSub(lastSubId)
            assertNotNull(response)
        }
    }

    // ==================== Edit Sub ====================
    @Test
    fun testEditSub_wrongId() {
        runBlocking {
            val response = updateSub(
                -1,
                "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada",
                -80.5380946,
                43.4624441
            )
            assertEquals(false, response)
        }
    }

    @Test
    fun testEditSub_noAddr() {
        runBlocking {
            val response = updateSub(
                lastSubId,
                "",
                -80.5380946,
                43.4624441
            )
            assertEquals(false, response)
        }
    }

    @Test
    fun testEditSub_invalidLng() {
        runBlocking {
            val response = updateSub(
                lastSubId,
                "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada",
                -200.0,
                43.4624441
            )
            assertEquals(false, response)
        }
    }

    @Test
    fun testEditSub_invalidLat() {
        runBlocking {
            val response = updateSub(
                lastSubId,
                "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada",
                -80.5380946,
                100.4624441
            )
            assertEquals(false, response)
        }
    }

    @Test
    fun testEditSub_valid() {
        runBlocking {
            val response = updateSub(
                lastSubId,
                "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada",
                -80.5380946,
                43.4624441
            )
            assertEquals(true, response)
        }
    }

    // ==================== Delete Sub ====================
    @Test
    fun testDeleteSub_wrongId() {
        runBlocking {
            val response = deleteSub(-1)
            assertEquals(false, response)
        }
    }

    @Test
    fun testDeleteSub_valid() {
        runBlocking {
            val response = deleteSub(lastSubId)
            assertEquals(true, response)
        }
    }
}