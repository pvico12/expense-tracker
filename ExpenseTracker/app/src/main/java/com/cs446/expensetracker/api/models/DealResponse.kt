package com.cs446.expensetracker.api.models

data class DealRetrievalResponse (
    val id: Int,
    val name: String,
    val description: String,
    val vendor: String,
    val price: Double,
    val date: String,
    val address: String,
    val longitude: String,
    val latitude: String,
    var upvotes: Int,
    var downvotes: Int,
    var user_vote: Int,
    val maps_link: String
)

data class DealRetrievalRequestWithUser (
    val user_id: Int
)

data class DealRetrievalRequestWithLocation (
    val location: DealLocation
)

data class DealLocation(
    val longitude: Double,
    val latitude: Double,
    val distance: Double,
)

data class DealCreationRequest (
    val name: String,
    val description: String,
    val vendor: String,
    val price: Double,
    val date: String, // ISO 8601 format
    val address: String,
    val longitude: Double,
    val latitude: Double
)

data class DealAddResponse (
    val id: Int
)
