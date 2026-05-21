package com.liongate.budget.presentation.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liongate.budget.domain.model.Budget
import com.liongate.budget.domain.model.Category
import com.liongate.budget.domain.model.TransactionType
import com.liongate.budget.domain.repository.BudgetRepository
import com.liongate.budget.domain.repository.CategoryRepository
import com.liongate.budget.domain.repository.TransactionRepository
import com.liongate.budget.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetUiState(
    val isLoading: Boolean = true,
    val currentMonth: String = DateUtils.getCurrentMonthYear(),
    val budgetItems: List<BudgetItem> = emptyList()
)

data class BudgetItem(
    val category: Category,
    val budget: Budget?,
    val spent: Double,
    val percentUsed: Float = if ((budget?.limitAmount ?: 0.0) > 0) (spent / budget!!.limitAmount * 100).toFloat() else 0f,
    val isOverBudget: Boolean = budget != null && spent > budget.limitAmount
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    init {
        loadData(DateUtils.getCurrentMonthYear())
    }

    fun loadData(month: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, currentMonth = month)
        viewModelScope.launch {
            combine(
                categoryRepository.getAllCategories(),
                budgetRepository.getBudgetsByMonth(month),
                transactionRepository.getTransactionsByMonth(month)
            ) { categories, budgets, transactions ->
                val budgetMap = budgets.associateBy { it.categoryId }
                val spentMap = transactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .groupBy { it.categoryId }
                    .mapValues { (_, txs) -> txs.sumOf { it.amount } }

                val expenseCategories = categories.filter {
                    it.type.name == "EXPENSE" || it.type.name == "BOTH"
                }

                expenseCategories.map { category ->
                    val budget = budgetMap[category.id]
                    val spent = spentMap[category.id] ?: 0.0
                    BudgetItem(
                        category = category,
                        budget = budget,
                        spent = spent
                    )
                }.sortedByDescending { it.spent }
            }.collect { items ->
                _uiState.value = _uiState.value.copy(isLoading = false, budgetItems = items)
            }
        }
    }

    fun navigateMonth(forward: Boolean) {
        val current = _uiState.value.currentMonth
        val newMonth = if (forward) DateUtils.getNextMonth(current) else DateUtils.getPreviousMonth(current)
        loadData(newMonth)
    }

    fun setBudget(categoryId: Long, limitAmount: Double) {
        viewModelScope.launch {
            val month = _uiState.value.currentMonth
            val existing = budgetRepository.getBudgetByCategoryAndMonth(categoryId, month)
            if (existing != null) {
                budgetRepository.updateBudget(existing.copy(limitAmount = limitAmount))
            } else {
                budgetRepository.insertBudget(
                    Budget(categoryId = categoryId, monthYear = month, limitAmount = limitAmount)
                )
            }
        }
    }

    fun removeBudget(categoryId: Long) {
        viewModelScope.launch {
            budgetRepository.deleteBudgetByCategoryAndMonth(categoryId, _uiState.value.currentMonth)
        }
    }
}
