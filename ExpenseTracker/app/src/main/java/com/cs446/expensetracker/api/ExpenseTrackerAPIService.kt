package com.cs446.expensetracker.api

import com.cs446.expensetracker.api.models.HealthcheckResponse
import retrofit2.Response
import retrofit2.http.GET

interface ExpenseTrackerAPIService {

    @GET("/healthcheck")
    suspend fun getBackendConnectionStatus(): Response<HealthcheckResponse>

}