package com.cs446.expensetracker

import com.auth0.jwt.JWT
import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.DealRetrievalRequestWithUser
import com.cs446.expensetracker.api.models.DealRetrievalResponse
import com.cs446.expensetracker.api.models.FcmTokenUploadRequest
import com.cs446.expensetracker.api.models.LoginRequest
import com.cs446.expensetracker.session.UserSession
import com.cs446.expensetracker.ui.deals.createDeal
import com.cs446.expensetracker.ui.deals.deleteDeal
import com.cs446.expensetracker.ui.deals.downvote
import com.cs446.expensetracker.ui.deals.getDeal
import com.cs446.expensetracker.ui.deals.updateDeal
import com.cs446.expensetracker.ui.deals.upvote
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class DealsUnitTests {
    private var lastDealId : Int = 1

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
                lastDealId = listOfDeals.lastOrNull()?.id ?: 1
            }
        }
    }

    // ==================== Add Deal ====================
    @Test
    fun testAddDeal_noName() {
        runBlocking {
            val response = createDeal(
                "",
                "1 box",
                "T&T",
                1.0,
                "2025-03-29T20:16:50",
                "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada",
                -80.5380946,
                43.4624441
            )
            assertEquals(false, response)
        }
    }

    @Test
    fun testAddDeal_noDescription() {
        runBlocking {
            val response = createDeal(
                "Grapes",
                "",
                "T&T",
                1.0,
                "2025-03-29T20:16:50",
                "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada",
                -80.5380946,
                43.4624441
            )
            assertEquals(false, response)
        }
    }

    @Test
    fun testAddDeal_noVendor() {
        runBlocking {
            val response = createDeal(
                "Grapes",
                "1 box",
                "",
                1.0,
                "2025-03-29T20:16:50",
                "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada",
                -80.5380946,
                43.4624441
            )
            assertEquals(false, response)
        }
    }

    @Test
    fun testAddDeal_negPrice() {
        runBlocking {
            val response = createDeal(
                "Grapes",
                "1 box",
                "T&T",
                -0.99,
                "2025-03-29T20:16:50",
                "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada",
                -80.5380946,
                43.4624441
            )
            assertEquals(false, response)
        }
    }

    @Test
    fun testAddDeal_noDate() {
        runBlocking {
            val response = createDeal(
                "Grapes",
                "1 box",
                "T&T",
                1.0,
                "",
                "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada",
                -80.5380946,
                43.4624441
            )
            assertEquals(false, response)
        }
    }

    @Test
    fun testAddDeal_invalidDate() {
        runBlocking {
            val response = createDeal(
                "Grapes",
                "1 box",
                "T&T",
                1.0,
                "2025-03-29T20:16:88",
                "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada",
                -80.5380946,
                43.4624441
            )
            assertEquals(false, response)
        }
    }

    @Test
    fun testAddDeal_noAddress() {
        runBlocking {
            val response = createDeal(
                "Grapes",
                "1 box",
                "T&T",
                1.0,
                "2025-03-29T20:16:50",
                "",
                -80.5380946,
                43.4624441
            )
            assertEquals(false, response)
        }
    }

    @Test
    fun testAddDeal_invalidLng() {
        runBlocking {
            val response = createDeal(
                "Grapes",
                "1 box",
                "T&T",
                1.0,
                "2025-03-29T20:16:50",
                "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada",
                -200.0,
                43.4624441
            )
            assertEquals(false, response)
        }
    }

    @Test
    fun testAddDeal_invalidLat() {
        runBlocking {
            val response = createDeal(
                "Grapes",
                "1 box",
                "T&T",
                1.0,
                "2025-03-29T20:16:50",
                "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada",
                -80.5380946,
                100.4624441
            )
            assertEquals(false, response)
        }
    }

    @Test
    fun testAddDeal_valid() {
        runBlocking {
            val response = createDeal(
                "Grapes",
                "1 box",
                "T&T",
                1.0,
                "2025-03-29T20:16:50",
                "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada",
                -80.5380946,
                43.4624441
            )
            assertEquals(true, response)
        }
    }

    // ==================== Get Specific Deal ====================
    @Test
    fun testGetDeal_noId() {
        runBlocking {
            val response = getDeal("")
            assertNull(response)
        }
    }

    @Test
    fun testGetDeal_wrongId() {
        runBlocking {
            val response = getDeal("-1")
            assertNull(response)
        }
    }

    @Test
    fun testGetDeal_valid() {
        runBlocking {
            val response = getDeal(lastDealId.toString())
            assertNotNull(response)
        }
    }

    // ==================== Upvote Deal ====================
    @Test
    fun testUpvoteDeal_emptyList() {
        runBlocking {
            val response = upvote(emptyList(), lastDealId.toString(), 0)
            assertNull(response)
        }
    }

    @Test
    fun testUpvoteDeal_noId() {
        runBlocking {
            val deal_item = getDeal(lastDealId.toString())
            val response = upvote(listOf(deal_item!!), "", 0)
            assertNull(response)
        }
    }

    @Test
    fun testUpvoteDeal_wrongId() {
        runBlocking {
            val deal_item = getDeal(lastDealId.toString())
            val response = upvote(listOf(deal_item!!), "-1", 0)
            assertNull(response)
        }
    }

    @Test
    fun testUpvoteDeal_wrongIdx() {
        runBlocking {
            val deal_item = getDeal(lastDealId.toString())
            val response = upvote(listOf(deal_item!!), lastDealId.toString(), 1)
            assertNull(response)
        }
    }

    @Test
    fun testUpvoteDeal_valid() {
        runBlocking {
            val deal_item = getDeal(lastDealId.toString())
            val upvotes_old = deal_item!!.upvotes
            val response = upvote(listOf(deal_item), lastDealId.toString(), 0)
            assertNotNull(response)
            assertEquals(upvotes_old + 1, response!![0].upvotes)
        }
    }

    // ==================== Downvote Deal ====================
    @Test
    fun testDownvoteDeal_emptyList() {
        runBlocking {
            val response = downvote(emptyList(), lastDealId.toString(), 0)
            assertNull(response)
        }
    }

    @Test
    fun testDownvoteDeal_noId() {
        runBlocking {
            val deal_item = getDeal(lastDealId.toString())
            val response = downvote(listOf(deal_item!!), "", 0)
            assertNull(response)
        }
    }

    @Test
    fun testDownvoteDeal_wrongId() {
        runBlocking {
            val deal_item = getDeal(lastDealId.toString())
            val response = downvote(listOf(deal_item!!), "-1", 0)
            assertNull(response)
        }
    }

    @Test
    fun testDownvoteDeal_wrongIdx() {
        runBlocking {
            val deal_item = getDeal(lastDealId.toString())
            val response = downvote(listOf(deal_item!!), lastDealId.toString(), 1)
            assertNull(response)
        }
    }

    @Test
    fun testDownvoteDeal_valid() {
        runBlocking {
            val deal_item = getDeal(lastDealId.toString())
            val downvotes_old = deal_item!!.downvotes
            val response = downvote(listOf(deal_item), lastDealId.toString(), 0)
            assertNotNull(response)
            assertEquals(downvotes_old + 1, response!![0].downvotes)
        }
    }

    // ==================== Edit Deal ====================
    @Test
    fun testEditDeal_noId() {
        runBlocking {
            val response = updateDeal(
                "",
                "Grapes",
                "1 box",
                "T&T",
                1.0,
                "2025-03-29T20:16:50",
                "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada",
                -80.5380946,
                43.4624441
            )
            assertEquals(false, response)
        }
    }

    @Test
    fun testEditDeal_wrongId() {
        runBlocking {
            val response = updateDeal(
                "-1",
                "Grapes",
                "1 box",
                "T&T",
                1.0,
                "2025-03-29T20:16:50",
                "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada",
                -80.5380946,
                43.4624441
            )
            assertEquals(false, response)
        }
    }

    @Test
    fun testEditDeal_noName() {
        runBlocking {
            val response = updateDeal(
                lastDealId.toString(),
                "",
                "1 box",
                "T&T",
                1.0,
                "2025-03-29T20:16:50",
                "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada",
                -80.5380946,
                43.4624441
            )
            assertEquals(false, response)
        }
    }

    @Test
    fun testEditDeal_noDescription() {
        runBlocking {
            val response = updateDeal(
                lastDealId.toString(),
                "Grapes",
                "",
                "T&T",
                1.0,
                "2025-03-29T20:16:50",
                "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada",
                -80.5380946,
                43.4624441
            )
            assertEquals(false, response)
        }
    }

    @Test
    fun testEditDeal_noVendor() {
        runBlocking {
            val response = updateDeal(
                lastDealId.toString(),
                "Grapes",
                "1 box",
                "",
                1.0,
                "2025-03-29T20:16:50",
                "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada",
                -80.5380946,
                43.4624441
            )
            assertEquals(false, response)
        }
    }

    @Test
    fun testEditDeal_negPrice() {
        runBlocking {
            val response = updateDeal(
                lastDealId.toString(),
                "Grapes",
                "1 box",
                "T&T",
                -0.99,
                "2025-03-29T20:16:50",
                "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada",
                -80.5380946,
                43.4624441
            )
            assertEquals(false, response)
        }
    }

    @Test
    fun testEditDeal_noDate() {
        runBlocking {
            val response = updateDeal(
                lastDealId.toString(),
                "Grapes",
                "1 box",
                "T&T",
                1.0,
                "",
                "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada",
                -80.5380946,
                43.4624441
            )
            assertEquals(false, response)
        }
    }

    @Test
    fun testEditDeal_invalidDate() {
        runBlocking {
            val response = updateDeal(
                lastDealId.toString(),
                "Grapes",
                "1 box",
                "T&T",
                1.0,
                "2025-03-29T20:16:88",
                "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada",
                -80.5380946,
                43.4624441
            )
            assertEquals(false, response)
        }
    }

    @Test
    fun testEditDeal_noAddress() {
        runBlocking {
            val response = updateDeal(
                lastDealId.toString(),
                "Grapes",
                "1 box",
                "T&T",
                1.0,
                "2025-03-29T20:16:50",
                "",
                -80.5380946,
                43.4624441
            )
            assertEquals(false, response)
        }
    }

    @Test
    fun testEditDeal_invalidLng() {
        runBlocking {
            val response = updateDeal(
                lastDealId.toString(),
                "Grapes",
                "1 box",
                "T&T",
                1.0,
                "2025-03-29T20:16:50",
                "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada",
                -200.0,
                43.4624441
            )
            assertEquals(false, response)
        }
    }

    @Test
    fun testEditDeal_invalidLat() {
        runBlocking {
            val response = updateDeal(
                lastDealId.toString(),
                "Grapes",
                "1 box",
                "T&T",
                1.0,
                "2025-03-29T20:16:50",
                "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada",
                -80.5380946,
                100.4624441
            )
            assertEquals(false, response)
        }
    }

    @Test
    fun testEditDeal_valid() {
        runBlocking {
            val response = updateDeal(
                lastDealId.toString(),
                "Grapes",
                "1 box",
                "T&T",
                1.0,
                "2025-03-29T20:16:50",
                "T&T Supermarket Waterloo Store, Westmount Road North, Waterloo, ON, Canada",
                -80.5380946,
                43.4624441
            )
            assertEquals(true, response)
        }
    }

    // ==================== Delete Deal ====================
    @Test
    fun testDeleteDeal_noId() {
        runBlocking {
            val response = deleteDeal("")
            assertEquals(false, response)
        }
    }

    @Test
    fun testDeleteDeal_wrongId() {
        runBlocking {
            val response = deleteDeal("-1")
            assertEquals(false, response)
        }
    }

    @Test
    fun testDeleteDeal_valid() {
        runBlocking {
            val response = deleteDeal(lastDealId.toString())
            assertEquals(true, response)
        }
    }
}