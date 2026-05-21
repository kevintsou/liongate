package com.liongate.budget.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liongate.budget.data.local.dao.CategoryStat
import com.liongate.budget.data.local.dao.MonthlyStat
import com.liongate.budget.domain.model.Category
import com.liongate.budget.domain.repository.CategoryRepository
import com.liongate.budget.domain.repository.TransactionRepository
import com.liongate.budget.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class StatsUiState(
    val isLoading: Boolean = true,
    val selectedTab: Int = 0,
    val currentMonth: String = DateUtils.getCurrentMonthYear(),
    val currentYear: String = Calendar.getInstance().get(Calendar.YEAR).toString(),
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val last6Months: List<MonthlyStat> = emptyList(),
    val monthCategoryStats: List<CategoryStat> = emptyList(),
    val yearlyMonths: List<MonthlyStat> = emptyList(),
    val yearCategoryStats: List<CategoryStat> = emptyList(),
    val yearlyIncome: Double = 0.0,
    val yearlyExpense: Double = 0.0,
    val categories: List<Category> = emptyList()
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
        loadMonthlyStats(_uiState.value.currentMonth)
        loadYearlyStats(_uiState.value.currentYear)
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
    }

    fun selectTab(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun navigateMonth(forward: Boolean) {
        val current = _uiState.value.currentMonth
        val newMonth = if (forward) DateUtils.getNextMonth(current) else DateUtils.getPreviousMonth(current)
        loadMonthlyStats(newMonth)
    }

    fun navigateYear(forward: Boolean) {
        val current = _uiState.value.currentYear.toIntOrNull() ?: return
        val newYear = (current + if (forward) 1 else -1).toString()
        loadYearlyStats(newYear)
    }

    private fun loadMonthlyStats(month: String) {
        _uiState.update { it.copy(currentMonth = month, isLoading = true) }
        viewModelScope.launch {
            combine(
                transactionRepository.getMonthlyStats(),
                transactionRepository.getCategoryStatsByMonth(month)
            ) { last6, catStats ->
                Pair(last6, catStats)
            }.collect { (last6, catStats) ->
                val summary = transactionRepository.getMonthSummary(month)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        last6Months = last6,
                        monthCategoryStats = catStats,
                        monthlyIncome = summary?.totalIncome ?: 0.0,
                        monthlyExpense = summary?.totalExpense ?: 0.0
                    )
                }
            }
        }
    }

    private fun loadYearlyStats(year: String) {
        _uiState.update { it.copy(currentYear = year) }
        viewModelScope.launch {
            combine(
                transactionRepository.getMonthlyStatsForYear(year),
                transactionRepository.getCategoryStatsByYear(year)
            ) { monthStats, catStats ->
                Pair(monthStats, catStats)
            }.collect { (monthStats, catStats) ->
                val totalIncome = monthStats.sumOf { it.totalIncome }
                val totalExpense = monthStats.sumOf { it.totalExpense }
                _uiState.update {
                    it.copy(
                        yearlyMonths = monthStats,
                        yearCategoryStats = catStats,
                        yearlyIncome = totalIncome,
                        yearlyExpense = totalExpense
                    )
                }
            }
        }
    }
}
