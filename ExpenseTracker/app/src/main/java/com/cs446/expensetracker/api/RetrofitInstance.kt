package com.cs446.expensetracker.api

object RetrofitInstance {

    private const val BASE_PROD_URL = "https://expense-tracker-backend-prod-pvico.k8s.csclub.cloud/"
    private const val BASE_DEV_URL = "https://expense-tracker-backend-dev-pvico.k8s.csclub.cloud/"

    val apiService: BaseAPIService by lazy {
        when (EnvironmentConstants.TARGET_BACKEND_ENV) {
            TargetBackendEnvironment.PROD -> RetrofitFactory.create(BASE_PROD_URL).create(BaseAPIService::class.java)
            TargetBackendEnvironment.DEV -> RetrofitFactory.create(BASE_DEV_URL).create(BaseAPIService::class.java)
            else -> RetrofitFactory.create(BASE_PROD_URL).create(BaseAPIService::class.java)
        }
    }

    val apiService2: BaseAPIService by lazy {
        when (EnvironmentConstants.TARGET_BACKEND_ENV) {
            TargetBackendEnvironment.PROD -> RetrofitFactory.create(BASE_DEV_URL).create(BaseAPIService::class.java)
            TargetBackendEnvironment.DEV -> RetrofitFactory.create(BASE_PROD_URL).create(BaseAPIService::class.java)
            else -> RetrofitFactory.create(BASE_DEV_URL).create(BaseAPIService::class.java)
        }
    }
}
