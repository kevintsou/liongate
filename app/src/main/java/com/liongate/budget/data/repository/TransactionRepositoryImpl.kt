package com.liongate.budget.data.repository

import com.liongate.budget.data.local.dao.CategoryStat
import com.liongate.budget.data.local.dao.MonthSummary
import com.liongate.budget.data.local.dao.MonthlyStat
import com.liongate.budget.data.local.dao.TransactionDao
import com.liongate.budget.data.local.entity.TransactionEntity
import com.liongate.budget.domain.model.Transaction
import com.liongate.budget.domain.model.TransactionType
import com.liongate.budget.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTransactionsByMonth(month: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByMonth(month).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getTransactionById(id)?.toDomain()
    }

    override suspend fun insertTransaction(transaction: Transaction): Long {
        return transactionDao.insertTransaction(transaction.toEntity())
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction.toEntity())
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction.toEntity())
    }

    override suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteTransactionById(id)
    }

    override fun getMonthlyStats(): Flow<List<MonthlyStat>> =
        transactionDao.getMonthlyStats()

    override fun getCategoryStatsByMonth(month: String): Flow<List<CategoryStat>> =
        transactionDao.getCategoryStatsByMonth(month)

    override fun getMonthlyStatsForYear(year: String): Flow<List<MonthlyStat>> =
        transactionDao.getMonthlyStatsForYear(year)

    override fun getCategoryStatsByYear(year: String): Flow<List<CategoryStat>> =
        transactionDao.getCategoryStatsByYear(year)

    override suspend fun getMonthSummary(month: String): MonthSummary? =
        transactionDao.getMonthSummary(month)

    override suspend fun getYearSummary(year: String): MonthSummary? =
        transactionDao.getYearSummary(year)

    override suspend fun getAllTransactionsList(): List<Transaction> =
        transactionDao.getAllTransactionsList().map { it.toDomain() }

    private fun TransactionEntity.toDomain(): Transaction {
        return Transaction(
            id = id,
            amount = amount,
            type = if (type == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE,
            categoryId = categoryId,
            description = description,
            date = date,
            createdAt = createdAt
        )
    }

    private fun Transaction.toEntity(): TransactionEntity {
        return TransactionEntity(
            id = id,
            amount = amount,
            type = type.name,
            categoryId = categoryId,
            description = description,
            date = date,
            createdAt = createdAt
        )
    }
}
