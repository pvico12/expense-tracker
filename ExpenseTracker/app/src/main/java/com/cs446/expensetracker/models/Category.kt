package com.cs446.expensetracker.models

data class Category(
    val id: Int,
    val name: String,
    val subcategories: List<Subcategory>
)

data class Subcategory(
    val id: Int,
    val name: String,
    val parent_id: Int
)
