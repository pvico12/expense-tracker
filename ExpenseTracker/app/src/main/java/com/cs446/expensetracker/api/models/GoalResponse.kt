package com.cs446.expensetracker.api.models

data class GoalRetrievalGoals (
    val id: Int,
    val category_id: Int?,
    val goal_type: String,
    val limit: Double,
    val start_date: String,
    val end_date: String,
    val period: Int,
    val on_track: Boolean,
    val time_left: Int,
    val amount_spent: Double,
    var category_string: String? = null,
)

data class GoalRetrievalStats (
    val completed: Int,
    val incompleted: Int,
    val failed: Int,
)

data class GoalRetrievalResponse (
    val goals: List<GoalRetrievalGoals>,
    val stats: GoalRetrievalStats,
)

data class GoalCreationRequest (
    val category_id: Int,
    val goal_type: String,
    val limit: Double,
    val start_date: String, // ISO 8601 format
    val period: Double,
)

data class GoalUpdateRequest (
    val category_id: Int,
    val limit: Double,
    val start_date: String, // ISO 8601 format
    val end_date: String, // ISO 8601 format
    val goal_type: String,
)