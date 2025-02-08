package com.cs446.expensetracker.api.models

data class RegistrationRequest(
    val username: String,
    val password: String,
    val firstname: String,
    val lastname: String
)
