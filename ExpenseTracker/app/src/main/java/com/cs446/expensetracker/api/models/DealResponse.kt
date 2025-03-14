package com.cs446.expensetracker.api.models

data class DealRetrievalResponse (
    val id: Int,
    val name: String,
    val description: String,
    val price: Int,
    val date: String,
    val address: String,
    val longitude: String,
    val latitude: String,
    val upvotes: Int,
    val downvotes: Int,
)

data class DealRetrievalRequest (
    val user_id: Int,
    val location: DealLocation,
)

data class DealLocation(
    val longitude: Double,
    val latitude: Double,
    val distance: Double,
)

data class DealCreationRequest (
    val name: String,
    val description: String,
    val price: Double,
    val date: String, // ISO 8601 format
    val address: String,
    val longitude: Double,
    val latitude: Double
)
