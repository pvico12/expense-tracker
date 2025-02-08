package com.cs446.expensetracker.api

import com.cs446.expensetracker.api.models.HealthcheckResponse
import com.cs446.expensetracker.api.models.LoginRequest
import com.cs446.expensetracker.api.models.LoginResponse
import com.cs446.expensetracker.api.models.RegistrationRequest
import com.cs446.expensetracker.api.models.TokenRefreshRequest
import com.cs446.expensetracker.api.models.TokenRefreshResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface BaseAPIService {

    @GET("/healthcheck")
    suspend fun getBackendConnectionStatus(): Response<HealthcheckResponse>

    // ==================== Authentication ====================
    @POST("/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("/auth/register")
    suspend fun register(@Body request: RegistrationRequest)

    @POST("/auth/refresh")
    suspend fun refreshToken(@Body request: TokenRefreshRequest): Response<TokenRefreshResponse>
}