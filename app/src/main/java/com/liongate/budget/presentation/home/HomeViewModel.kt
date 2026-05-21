package com.liongate.budget.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liongate.budget.domain.model.Category
import com.liongate.budget.domain.model.Transaction
import com.liongate.budget.domain.model.TransactionType
import com.liongate.budget.domain.repository.CategoryRepository
import com.liongate.budget.domain.repository.TransactionRepository
import com.liongate.budget.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val currentMonth: String = DateUtils.getCurrentMonthYear(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netAmount: Double = 0.0,
    val recentTransactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val currentMonth = DateUtils.getCurrentMonthYear()
        viewModelScope.launch {
            combine(
                transactionRepository.getTransactionsByMonth(currentMonth),
                categoryRepository.getAllCategories()
            ) { transactions, categories ->
                val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                HomeUiState(
                    isLoading = false,
                    currentMonth = currentMonth,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    netAmount = totalIncome - totalExpense,
                    recentTransactions = transactions.take(5),
                    categories = categories
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
        }
    }
}
