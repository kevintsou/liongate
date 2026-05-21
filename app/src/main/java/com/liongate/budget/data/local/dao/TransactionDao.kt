package com.liongate.budget.data.local.dao

import androidx.room.*
import com.liongate.budget.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class MonthlyStat(
    val month: String,
    val totalIncome: Double,
    val totalExpense: Double
)

data class CategoryStat(
    val categoryId: Long,
    val total: Double
)

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE strftime('%Y-%m', date/1000, 'unixepoch') = :month ORDER BY date DESC")
    fun getTransactionsByMonth(month: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("""
        SELECT
            strftime('%Y-%m', date/1000, 'unixepoch') as month,
            SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END) as totalIncome,
            SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) as totalExpense
        FROM transactions
        GROUP BY month
        ORDER BY month DESC
        LIMIT 6
    """)
    fun getMonthlyStats(): Flow<List<MonthlyStat>>

    @Query("""
        SELECT categoryId, SUM(amount) as total
        FROM transactions
        WHERE type = 'EXPENSE' AND strftime('%Y-%m', date/1000, 'unixepoch') = :month
        GROUP BY categoryId
    """)
    fun getCategoryStatsByMonth(month: String): Flow<List<CategoryStat>>

    @Query("""
        SELECT SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END) as totalIncome,
               SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) as totalExpense
        FROM transactions
        WHERE strftime('%Y-%m', date/1000, 'unixepoch') = :month
    """)
    suspend fun getMonthSummary(month: String): MonthSummary?

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllTransactionsList(): List<TransactionEntity>

    @Query("""
        SELECT
            strftime('%Y-%m', date/1000, 'unixepoch') as month,
            SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END) as totalIncome,
            SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) as totalExpense
        FROM transactions
        WHERE strftime('%Y', date/1000, 'unixepoch') = :year
        GROUP BY month
        ORDER BY month ASC
    """)
    fun getMonthlyStatsForYear(year: String): Flow<List<MonthlyStat>>

    @Query("""
        SELECT categoryId, SUM(amount) as total
        FROM transactions
        WHERE type = 'EXPENSE' AND strftime('%Y', date/1000, 'unixepoch') = :year
        GROUP BY categoryId
        ORDER BY total DESC
    """)
    fun getCategoryStatsByYear(year: String): Flow<List<CategoryStat>>

    @Query("""
        SELECT
            SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END) as totalIncome,
            SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) as totalExpense
        FROM transactions
        WHERE strftime('%Y', date/1000, 'unixepoch') = :year
    """)
    suspend fun getYearSummary(year: String): MonthSummary?
}

data class MonthSummary(
    val totalIncome: Double,
    val totalExpense: Double
)
