package com.cs446.expensetracker.api.models

data class LoginResponse(
    val access_token: String,
    val refresh_token: String
)