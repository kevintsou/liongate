package com.liongate.budget.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val colorHex: String,
    val icon: String,
    val type: CategoryType
)

enum class CategoryType {
    INCOME, EXPENSE, BOTH
}
