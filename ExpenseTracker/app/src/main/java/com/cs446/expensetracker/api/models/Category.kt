package com.cs446.expensetracker.api.models

data class Category(
    val id: Int,
    val name: String,
    val color: String
)
data class CategoryRequest(val item_name: String)

data class CategoryResponse(
    val category_id: Int,
    val category_name: String
)