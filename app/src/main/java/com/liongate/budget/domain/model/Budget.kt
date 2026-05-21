package com.liongate.budget.domain.model

data class Budget(
    val id: Long = 0,
    val categoryId: Long,
    val monthYear: String,
    val limitAmount: Double
)

data class BudgetStatus(
    val budget: Budget,
    val category: Category,
    val spent: Double,
    val isOverBudget: Boolean = spent > budget.limitAmount
)
