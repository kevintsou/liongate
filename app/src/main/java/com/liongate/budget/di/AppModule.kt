package com.liongate.budget.di

import android.content.Context
import com.liongate.budget.api.OpenClawApiServer
import com.liongate.budget.data.local.BudgetDatabase
import com.liongate.budget.data.local.dao.BudgetDao
import com.liongate.budget.data.local.dao.CategoryDao
import com.liongate.budget.data.local.dao.TransactionDao
import com.liongate.budget.data.repository.BudgetRepositoryImpl
import com.liongate.budget.data.repository.CategoryRepositoryImpl
import com.liongate.budget.data.repository.TransactionRepositoryImpl
import com.liongate.budget.domain.repository.BudgetRepository
import com.liongate.budget.domain.repository.CategoryRepository
import com.liongate.budget.domain.repository.TransactionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideBudgetDatabase(@ApplicationContext context: Context): BudgetDatabase {
        return BudgetDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideTransactionDao(database: BudgetDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    @Singleton
    fun provideCategoryDao(database: BudgetDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    @Singleton
    fun provideBudgetDao(database: BudgetDatabase): BudgetDao {
        return database.budgetDao()
    }

    @Provides
    @Singleton
    fun provideTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository = impl

    @Provides
    @Singleton
    fun provideCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository = impl

    @Provides
    @Singleton
    fun provideBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository = impl
}
