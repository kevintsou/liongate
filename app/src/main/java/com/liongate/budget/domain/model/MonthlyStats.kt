package com.liongate.budget.domain.model

data class MonthlyStats(
    val month: String,
    val totalIncome: Double,
    val totalExpense: Double,
    val netAmount: Double = totalIncome - totalExpense,
    val categoryBreakdown: Map<Long, Double> = emptyMap()
)
