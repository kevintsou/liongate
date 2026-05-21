package com.liongate.budget.domain.repository

import com.liongate.budget.data.local.dao.CategoryStat
import com.liongate.budget.data.local.dao.MonthSummary
import com.liongate.budget.data.local.dao.MonthlyStat
import com.liongate.budget.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionsByMonth(month: String): Flow<List<Transaction>>
    suspend fun getTransactionById(id: Long): Transaction?
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun deleteTransactionById(id: Long)
    fun getMonthlyStats(): Flow<List<MonthlyStat>>
    fun getCategoryStatsByMonth(month: String): Flow<List<CategoryStat>>
    fun getMonthlyStatsForYear(year: String): Flow<List<MonthlyStat>>
    fun getCategoryStatsByYear(year: String): Flow<List<CategoryStat>>
    suspend fun getMonthSummary(month: String): MonthSummary?
    suspend fun getYearSummary(year: String): MonthSummary?
    suspend fun getAllTransactionsList(): List<Transaction>
}
