package com.liongate.budget.domain.repository

import com.liongate.budget.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getBudgetsByMonth(monthYear: String): Flow<List<Budget>>
    suspend fun getBudgetsByMonthList(monthYear: String): List<Budget>
    suspend fun getBudgetByCategoryAndMonth(categoryId: Long, monthYear: String): Budget?
    suspend fun insertBudget(budget: Budget): Long
    suspend fun updateBudget(budget: Budget)
    suspend fun deleteBudget(budget: Budget)
    suspend fun deleteBudgetById(id: Long)
    suspend fun deleteBudgetByCategoryAndMonth(categoryId: Long, monthYear: String)
}
