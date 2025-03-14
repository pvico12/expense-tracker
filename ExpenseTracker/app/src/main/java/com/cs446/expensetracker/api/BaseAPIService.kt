package com.cs446.expensetracker.api

import com.cs446.expensetracker.api.models.HealthcheckResponse
import com.cs446.expensetracker.api.models.LoginRequest
import com.cs446.expensetracker.api.models.LoginResponse
import com.cs446.expensetracker.api.models.RegistrationRequest
import com.cs446.expensetracker.api.models.TokenRefreshRequest
import com.cs446.expensetracker.api.models.TokenRefreshResponse
import com.cs446.expensetracker.api.models.UserProfileResponse
import com.cs446.expensetracker.api.models.Category
import com.cs446.expensetracker.api.models.CategoryRequest
import com.cs446.expensetracker.api.models.CategoryResponse
import com.cs446.expensetracker.api.models.FcmTokenUploadRequest
import com.cs446.expensetracker.api.models.OcrResponse
import com.cs446.expensetracker.api.models.Transaction
import com.cs446.expensetracker.api.models.TransactionResponse
import com.cs446.expensetracker.api.models.UserProfileUpdateRequest
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

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

    @POST("/auth/fcm_token")
    suspend fun uploadFcmToken(@Body request: FcmTokenUploadRequest): Response<Unit>

//    @POST("/auth/token/identify")
//    suspend fun identifyUserToken(): Response<UserTokenIdentificationResponse>

    // ==================== User ====================
    @GET("/user/profile/{userId}")
    suspend fun getUserProfile(@Path("userId") userId: Int): Response<UserProfileResponse>

    @PUT("/user/profile")
    suspend fun updateUserProfile(@Body request: UserProfileUpdateRequest): Response<Void>

    // ===================== Transactions ======================
    @POST("/transactions/")
    suspend fun addTransaction(@Body transaction: Transaction): Response<Void>

    @GET("/transactions/")
    suspend fun getTransactions(
        @Query("skip") skip: Int,
        @Query("limit") limit: Int
    ): Response<List<TransactionResponse>>


    @Multipart
    @POST("/transactions/csv")
    suspend fun uploadCsv(
        @Part file: MultipartBody.Part,
        @Query("create_transactions") createTransaction: Int
    ): Response<List<Transaction>>

    @GET("transactions/csv/template")
    suspend fun getCsvTemplate(): Response<ResponseBody>

    @Multipart
    @POST("transactions/receipt/scan")
    suspend fun scanReceipt(@Part file: MultipartBody.Part): Response<OcrResponse>

    // ====================== Categories ===========================
    @GET("/categories/")
    suspend fun getCategories(): Response<List<Category>>

    // ====================== Tools ===========================
    @POST("tools/categories/suggestion")
    suspend fun getCategorySuggestion(@Body request: CategoryRequest): Response<CategoryResponse>

}