package com.cs446.expensetracker.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

object RetrofitInstance {

    private const val BASE_PROD_URL = "https://expense-tracker-backend-prod-pvico.k8s.csclub.cloud/"
    private const val BASE_DEV_URL = "https://expense-tracker-backend-prod-pvico.k8s.csclub.cloud/"

    private val logging = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)

    private val okHttpClient = OkHttpClient.Builder().addInterceptor(logging).build()

    private val retrofitProd: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_PROD_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    private val retrofitDev: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_DEV_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }


    val apiService: ExpenseTrackerAPIService by lazy {
        when (EnvironmentConstants.TARGET_BACKEND_ENV) {
            TargetBackendEnvironment.PROD -> retrofitProd.create(ExpenseTrackerAPIService::class.java)
            TargetBackendEnvironment.DEV -> retrofitDev.create(ExpenseTrackerAPIService::class.java)
            else -> retrofitProd.create(ExpenseTrackerAPIService::class.java)
        }
    }

    val apiService2: ExpenseTrackerAPIService by lazy {
        when (EnvironmentConstants.TARGET_BACKEND_ENV) {
            TargetBackendEnvironment.PROD -> retrofitDev.create(ExpenseTrackerAPIService::class.java)
            TargetBackendEnvironment.DEV -> retrofitProd.create(ExpenseTrackerAPIService::class.java)
            else -> retrofitDev.create(ExpenseTrackerAPIService::class.java)
        }
    }
}