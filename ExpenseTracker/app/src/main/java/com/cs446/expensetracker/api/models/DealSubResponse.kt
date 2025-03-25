package com.cs446.expensetracker.api.models

data class DealSubRetrievalResponse (
    val id: Int,
    val user_id: Int,
    val address: String,
    val longitude: String,
    val latitude: String
)

data class DealSubCreationRequest (
    val address: String,
    val longitude: Double,
    val latitude: Double
)
