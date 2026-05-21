package com.liongate.budget.presentation.transaction

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

data class TransactionListState(
    val isLoading: Boolean = true,
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedMonth: String = DateUtils.getCurrentMonthYear()
)

data class AddEditTransactionState(
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val transaction: Transaction? = null,
    val categories: List<Category> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _listState = MutableStateFlow(TransactionListState())
    val listState: StateFlow<TransactionListState> = _listState.asStateFlow()

    private val _addEditState = MutableStateFlow(AddEditTransactionState())
    val addEditState: StateFlow<AddEditTransactionState> = _addEditState.asStateFlow()

    init {
        loadTransactions(DateUtils.getCurrentMonthYear())
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                _listState.value = _listState.value.copy(categories = categories)
                _addEditState.value = _addEditState.value.copy(categories = categories)
            }
        }
    }

    fun loadTransactions(month: String) {
        viewModelScope.launch {
            _listState.value = _listState.value.copy(selectedMonth = month)
            transactionRepository.getTransactionsByMonth(month).collect { transactions ->
                _listState.value = _listState.value.copy(
                    isLoading = false,
                    transactions = transactions
                )
            }
        }
    }

    fun loadTransaction(id: Long) {
        viewModelScope.launch {
            _addEditState.value = _addEditState.value.copy(isLoading = true)
            val transaction = transactionRepository.getTransactionById(id)
            _addEditState.value = _addEditState.value.copy(
                isLoading = false,
                transaction = transaction
            )
        }
    }

    fun saveTransaction(
        id: Long?,
        amount: Double,
        type: TransactionType,
        categoryId: Long,
        description: String,
        date: Long
    ) {
        viewModelScope.launch {
            val transaction = Transaction(
                id = id ?: 0,
                amount = amount,
                type = type,
                categoryId = categoryId,
                description = description,
                date = date
            )
            if (id != null && id > 0) {
                transactionRepository.updateTransaction(transaction)
            } else {
                transactionRepository.insertTransaction(transaction)
            }
            _addEditState.value = _addEditState.value.copy(isSaved = true)
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            transactionRepository.deleteTransactionById(id)
        }
    }

    fun resetSaveState() {
        _addEditState.value = _addEditState.value.copy(isSaved = false)
    }
}
