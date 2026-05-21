package com.liongate.budget.data.repository

import com.liongate.budget.data.local.dao.BudgetDao
import com.liongate.budget.data.local.entity.BudgetEntity
import com.liongate.budget.domain.model.Budget
import com.liongate.budget.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override fun getBudgetsByMonth(monthYear: String): Flow<List<Budget>> {
        return budgetDao.getBudgetsByMonth(monthYear).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getBudgetsByMonthList(monthYear: String): List<Budget> {
        return budgetDao.getBudgetsByMonthList(monthYear).map { it.toDomain() }
    }

    override suspend fun getBudgetByCategoryAndMonth(categoryId: Long, monthYear: String): Budget? {
        return budgetDao.getBudgetByCategoryAndMonth(categoryId, monthYear)?.toDomain()
    }

    override suspend fun insertBudget(budget: Budget): Long {
        return budgetDao.insertBudget(budget.toEntity())
    }

    override suspend fun updateBudget(budget: Budget) {
        budgetDao.updateBudget(budget.toEntity())
    }

    override suspend fun deleteBudget(budget: Budget) {
        budgetDao.deleteBudget(budget.toEntity())
    }

    override suspend fun deleteBudgetById(id: Long) {
        budgetDao.deleteBudgetById(id)
    }

    override suspend fun deleteBudgetByCategoryAndMonth(categoryId: Long, monthYear: String) {
        budgetDao.deleteBudgetByCategoryAndMonth(categoryId, monthYear)
    }

    private fun BudgetEntity.toDomain(): Budget {
        return Budget(
            id = id,
            categoryId = categoryId,
            monthYear = monthYear,
            limitAmount = limitAmount
        )
    }

    private fun Budget.toEntity(): BudgetEntity {
        return BudgetEntity(
            id = id,
            categoryId = categoryId,
            monthYear = monthYear,
            limitAmount = limitAmount
        )
    }
}
