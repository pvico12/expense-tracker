package com.cs446.expensetracker.api

import com.cs446.expensetracker.api.models.HealthcheckResponse
import com.cs446.expensetracker.api.models.LoginRequest
import com.cs446.expensetracker.api.models.LoginResponse
import com.cs446.expensetracker.api.models.RegistrationRequest
import com.cs446.expensetracker.api.models.TokenRefreshRequest
import com.cs446.expensetracker.api.models.TokenRefreshResponse
import com.cs446.expensetracker.api.models.UserProfileResponse
import com.cs446.expensetracker.api.models.Category
import com.cs446.expensetracker.api.models.DealCreationRequest
import com.cs446.expensetracker.api.models.DealRetrievalRequestWithLocation
import com.cs446.expensetracker.api.models.DealRetrievalRequestWithUser
import com.cs446.expensetracker.api.models.CustomCategoryRequest
import com.cs446.expensetracker.api.models.SuggestionRequest
import com.cs446.expensetracker.api.models.SuggestionResponse
import com.cs446.expensetracker.api.models.FcmTokenUploadRequest
import com.cs446.expensetracker.api.models.DealRetrievalResponse
import com.cs446.expensetracker.api.models.DealSubCreationRequest
import com.cs446.expensetracker.api.models.DealSubRetrievalResponse
import com.cs446.expensetracker.api.models.GoalCreationRequest
import com.cs446.expensetracker.api.models.GoalRetrievalGoals
import com.cs446.expensetracker.api.models.GoalRetrievalResponse
import com.cs446.expensetracker.api.models.GoalUpdateRequest
import com.cs446.expensetracker.api.models.LevelRequest
import com.cs446.expensetracker.api.models.OcrResponse
import com.cs446.expensetracker.api.models.RecurringTransactionRequest
import com.cs446.expensetracker.api.models.RecurringTransactionResponse
import com.cs446.expensetracker.api.models.SpendingSummaryResponse
import com.cs446.expensetracker.api.models.Transaction
import com.cs446.expensetracker.api.models.TransactionResponse
import com.cs446.expensetracker.api.models.UserProfileUpdateRequest
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
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

    @GET("/user/level")
    suspend fun getLevel(): Response<LevelRequest>

    // ===================== Transactions ======================
    @POST("/transactions/")
    suspend fun addTransaction(@Body transaction: Transaction): Response<Void>

    @GET("/transactions/")
    suspend fun getTransactions(
        @Query("skip") skip: Int,
        @Query("limit") limit: Int,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
    ): Response<List<TransactionResponse>>

    @PUT("/transactions/{transaction_id}")
    suspend fun updateTransaction(
        @Path("transaction_id") transactionId: Int,
        @Body transaction: Transaction
    ): Response<Void>

    @DELETE("/transactions/{transaction_id}")
    suspend fun deleteTransaction(@Path("transaction_id") transactionId: Int): Response<Void>

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

    // ================= Recurring Transactions ====================

    @POST("transactions/recurring/")
    suspend fun createRecurringTransaction(
        @Body request: RecurringTransactionRequest
    ): Response<RecurringTransactionResponse>

    // ====================== Categories ===========================
    @GET("/categories/")
    suspend fun getCategories(): Response<List<Category>>

    // ====================== Statistics ===========================
    @GET("/statistics/summary_spend")
    suspend fun getSpendingSummary(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String
    ): Response<SpendingSummaryResponse>

    // ====================== Deals ===========================
    @POST("/deals/list")
    suspend fun getDeals(@Body DealRetrievalRequestWithUser: DealRetrievalRequestWithUser): Response<List<DealRetrievalResponse>>

    @POST("/deals/list")
    suspend fun getDeals(@Body DealRetrievalRequestWithLocation: DealRetrievalRequestWithLocation): Response<List<DealRetrievalResponse>>

    @GET("/deals/{deal_id}")
    suspend fun getSpecificDeal(@Path("deal_id") dealId: String): Response<DealRetrievalResponse>

    @DELETE("/deals/{deal_id}")
    suspend fun deleteDeal(@Path("deal_id") dealId: String): Response<String>

    @PUT("/deals/{deal_id}")
    suspend fun updateDeal(@Path("deal_id") dealId: String, @Body DealCreationRequest: DealCreationRequest): Response<String>

    @POST("/deals/")
    suspend fun addDeal(@Body DealCreationRequest: DealCreationRequest): Response<String>

    @POST("/deals/upvote/{deal_id}")
    suspend fun upvoteDeal(@Path("deal_id") dealId: String): Response<String>

    @POST("/deals/downvote/{deal_id}")
    suspend fun downvoteDeal(@Path("deal_id") dealId: String): Response<String>

    @POST("/deals/cancel_vote/{deal_id}")
    suspend fun cancelvoteDeal(@Path("deal_id") dealId: String): Response<String>

    @POST("categories/custom")
    suspend fun createCustomCategory(@Body category: CustomCategoryRequest): Response<Category>

    // ====================== Deals ===========================

    @GET("/deals/subscriptions/all")
    suspend fun getSubs(): Response<List<DealSubRetrievalResponse>>

    @GET("/deals/subscription/{deal_sub_id}")
    suspend fun getSpecificSub(@Path("deal_sub_id") dealSubId: Int): Response<DealSubRetrievalResponse>

    @DELETE("/deals/subscription/{deal_sub_id}")
    suspend fun deleteSub(@Path("deal_sub_id") dealSubId: Int): Response<String>

    @PUT("/deals/subscription/{deal_sub_id}")
    suspend fun updateSub(@Path("deal_sub_id") dealSubId: Int, @Body request: DealSubCreationRequest): Response<DealSubRetrievalResponse>

    @POST("/deals/subscription")
    suspend fun addSub(@Body request: DealSubCreationRequest): Response<DealSubRetrievalResponse>

    // ====================== Tools ===========================
    @POST("tools/categories/suggestion")
    suspend fun getCategorySuggestion(@Body request: SuggestionRequest): Response<SuggestionResponse>

    // ====================== Goals ===========================

    @GET("/goals/")
    suspend fun getGoals(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String
    ): Response<GoalRetrievalResponse>

    @GET("/goals/")
    suspend fun getGoals(): Response<GoalRetrievalResponse>

    @POST("/goals/")
    suspend fun addGoal(@Body GoalCreationRequest: GoalCreationRequest): Response<GoalRetrievalResponse>

    @PUT("/goals/{goal_id}")
    suspend fun updateGoal(@Path("goal_id") goalId: String, @Body GoalUpdateRequest: GoalUpdateRequest): Response<GoalRetrievalGoals>

    @DELETE("/goals/{goal_id}")
    suspend fun deleteGoal(@Path("goal_id") goalId: String): Response<String>

}