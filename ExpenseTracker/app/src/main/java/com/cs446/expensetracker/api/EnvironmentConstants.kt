package com.cs446.expensetracker.api

enum class TargetBackendEnvironment {
    PROD, DEV
}

object EnvironmentConstants {
    val TARGET_BACKEND_ENV = TargetBackendEnvironment.PROD
}