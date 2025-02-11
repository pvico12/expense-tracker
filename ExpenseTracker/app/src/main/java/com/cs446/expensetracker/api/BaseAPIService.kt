package com.cs446.expensetracker.api

import com.cs446.expensetracker.api.models.HealthcheckResponse
import com.cs446.expensetracker.api.models.LoginRequest
import com.cs446.expensetracker.api.models.LoginResponse
import com.cs446.expensetracker.api.models.RegistrationRequest
import com.cs446.expensetracker.api.models.TokenRefreshRequest
import com.cs446.expensetracker.api.models.TokenRefreshResponse
import com.cs446.expensetracker.api.models.UserProfileResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class Category(
    val id: Int,
    val name: String,
    val subcategories: List<Subcategory>
)

data class Subcategory(
    val id: Int,
    val name: String,
    val parent_id: Int
)

data class Transaction(
    val amount: Double,
    val category_id: Int,
    val transaction_type: String = "expense", // Default to "expense"
    val note: String,
    val date: String // Must be in ISO 8601 format: "YYYY-MM-DDTHH:mm:ss.SSSZ"
)

interface BaseAPIService {

    @GET("/healthcheck")
    suspend fun getBackendConnectionStatus(): Response<HealthcheckResponse>

    // ==================== Authentication ====================
    @POST("/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("/auth/register")
    suspend fun register(@Body request: RegistrationRequest): Response<Unit>

    @POST("/auth/refresh")
    suspend fun refreshToken(@Body request: TokenRefreshRequest): Response<TokenRefreshResponse>

//    @POST("/auth/token/identify")
//    suspend fun identifyUserToken(): Response<UserTokenIdentificationResponse>

    // ==================== User ====================
    @GET("/user/profile/{userId}")
    suspend fun getUserProfile(@Path("userId") userId: Int): Response<UserProfileResponse>

    // ===================== Transactions ======================
    @POST("/transactions/")
    suspend fun addTransaction(
        @Header("Authorization") authHeader: String,
        @Body transaction: Transaction
    ): Response<Void>

    @GET("/transactions/categories")
    suspend fun getCategories(
        @Header("Authorization") authHeader: String
    ): Response<List<Category>>

}