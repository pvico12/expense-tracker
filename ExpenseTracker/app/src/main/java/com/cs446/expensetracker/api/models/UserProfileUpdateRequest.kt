package com.cs446.expensetracker.api.models

data class UserProfileUpdateRequest(
    val firstname: String,
    val lastname: String,
    val username: String
)

data class LevelRequest(
    val level: Int,
    val current_xp: Int,
    val remaining_xp_until_next_level: Int,
    val total_xp_for_next_level: Int,
)