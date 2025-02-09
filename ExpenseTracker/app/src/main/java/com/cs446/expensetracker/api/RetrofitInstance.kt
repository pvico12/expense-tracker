package com.cs446.expensetracker.api

import com.cs446.expensetracker.session.UserSession
import okhttp3.Interceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor

object RetrofitInstance {

    private const val BASE_PROD_URL = "https://expense-tracker-backend-prod-pvico.k8s.csclub.cloud/"
    private const val BASE_DEV_URL = "https://expense-tracker-backend-prod-pvico.k8s.csclub.cloud/"

    private val logging = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)

    private val authInterceptor = Interceptor { chain ->
        val original: Request = chain.request()
        val requestBuilder: Request.Builder = original.newBuilder()
            .header("Authorization", "Bearer ${UserSession.access_token}")
        val request: Request = requestBuilder.build()
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor(authInterceptor)
        .build()

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


    val apiService: BaseAPIService by lazy {
        when (EnvironmentConstants.TARGET_BACKEND_ENV) {
            TargetBackendEnvironment.PROD -> retrofitProd.create(BaseAPIService::class.java)
            TargetBackendEnvironment.DEV -> retrofitDev.create(BaseAPIService::class.java)
            else -> retrofitProd.create(BaseAPIService::class.java)
        }
    }

    val apiService2: BaseAPIService by lazy {
        when (EnvironmentConstants.TARGET_BACKEND_ENV) {
            TargetBackendEnvironment.PROD -> retrofitDev.create(BaseAPIService::class.java)
            TargetBackendEnvironment.DEV -> retrofitProd.create(BaseAPIService::class.java)
            else -> retrofitDev.create(BaseAPIService::class.java)
        }
    }
}