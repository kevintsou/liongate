package com.liongate.budget.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.liongate.budget.data.local.dao.BudgetDao
import com.liongate.budget.data.local.dao.CategoryDao
import com.liongate.budget.data.local.dao.TransactionDao
import com.liongate.budget.data.local.entity.BudgetEntity
import com.liongate.budget.data.local.entity.CategoryEntity
import com.liongate.budget.data.local.entity.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [TransactionEntity::class, CategoryEntity::class, BudgetEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BudgetDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: BudgetDatabase? = null

        fun getDatabase(context: Context): BudgetDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BudgetDatabase::class.java,
                    "budget_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateDefaultCategories(database.categoryDao())
                }
            }
        }

        suspend fun populateDefaultCategories(categoryDao: CategoryDao) {
            val defaultCategories = listOf(
                // Expense categories
                CategoryEntity(name = "飲食", colorHex = "#FF5252", icon = "🍜", type = "EXPENSE"),
                CategoryEntity(name = "交通", colorHex = "#FF6D00", icon = "🚇", type = "EXPENSE"),
                CategoryEntity(name = "購物", colorHex = "#FFAB40", icon = "🛍️", type = "EXPENSE"),
                CategoryEntity(name = "娛樂", colorHex = "#69F0AE", icon = "🎮", type = "EXPENSE"),
                CategoryEntity(name = "醫療", colorHex = "#40C4FF", icon = "🏥", type = "EXPENSE"),
                CategoryEntity(name = "居家", colorHex = "#7C4DFF", icon = "🏠", type = "EXPENSE"),
                CategoryEntity(name = "教育", colorHex = "#F06292", icon = "📚", type = "EXPENSE"),
                CategoryEntity(name = "其他", colorHex = "#90A4AE", icon = "💰", type = "EXPENSE"),
                // Income categories
                CategoryEntity(name = "薪資", colorHex = "#4CAF50", icon = "💼", type = "INCOME"),
                CategoryEntity(name = "兼職", colorHex = "#8BC34A", icon = "💵", type = "INCOME"),
                CategoryEntity(name = "投資", colorHex = "#00BCD4", icon = "📈", type = "INCOME"),
                CategoryEntity(name = "其他收入", colorHex = "#FFC107", icon = "🎁", type = "INCOME")
            )
            categoryDao.insertCategories(defaultCategories)
        }
    }
}
