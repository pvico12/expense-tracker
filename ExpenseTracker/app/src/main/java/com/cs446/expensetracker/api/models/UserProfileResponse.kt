package com.cs446.expensetracker.api.models

data class UserProfileResponse(
    val username: String,
    val password: String,
    val firstname: String,
    val lastname: String
)