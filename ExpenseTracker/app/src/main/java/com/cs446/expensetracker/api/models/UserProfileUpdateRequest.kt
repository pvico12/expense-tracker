package com.cs446.expensetracker.api.models

data class UserProfileUpdateRequest(
    val firstname: String,
    val lastname: String,
    val username: String
)