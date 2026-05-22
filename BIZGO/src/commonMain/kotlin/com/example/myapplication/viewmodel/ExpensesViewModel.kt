package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.myapplication.database.AppRepository
import com.example.myapplication.models.Expense
import com.example.myapplication.models.ExpenseCategory
import com.example.myapplication.utils.PlatformUtils
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*

data class ExpensesUiState(
    val showAddDialog: Boolean = false,
    val showRangeDialog: Boolean = false,
    val showManageCategories: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@OptIn(FlowPreview::class)
class ExpensesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ExpensesUiState())
    val uiState: StateFlow<ExpensesUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _dateFilter = MutableStateFlow("الكل")
    val dateFilter: StateFlow<String> = _dateFilter.asStateFlow()

    private val _customRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val customRange: StateFlow<Pair<Long, Long>?> = _customRange.asStateFlow()

    val categories: StateFlow<List<ExpenseCategory>> = AppRepository.expenseCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val expenses: StateFlow<List<Expense>> = combine(
        AppRepository.expenses,
        _searchQuery.debounce { if (it.isEmpty()) 0L else 300L },
        _dateFilter,
        _customRange
    ) { list, query, filter, range ->
        var filteredList = if (query.isBlank()) list
        else list.filter { item ->
            item.description.contains(query, ignoreCase = true) ||
            item.category.contains(query, ignoreCase = true)
        }

        val currentTime = PlatformUtils.currentTimeMillis()
        
        filteredList = when (filter) {
            "اليوم" -> {
                val today = Instant.fromEpochMilliseconds(currentTime)
                    .toLocalDateTime(TimeZone.currentSystemDefault()).date
                filteredList.filter { item ->
                    Instant.fromEpochMilliseconds(item.date)
                        .toLocalDateTime(TimeZone.currentSystemDefault()).date == today
                }
            }
            "هذا الشهر" -> {
                val now = Instant.fromEpochMilliseconds(currentTime)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                filteredList.filter { item ->
                    val itemDateTime = Instant.fromEpochMilliseconds(item.date)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                    (itemDateTime.month == now.month) && (itemDateTime.year == now.year)
                }
            }
            "مخصص" -> {
                range?.let { (start, end) ->
                    filteredList.filter { item -> item.date in start..end }
                } ?: filteredList
            }
            else -> filteredList
        }
        filteredList
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setDateFilter(filter: String) {
        _dateFilter.value = filter
        if (filter != "مخصص") _customRange.value = null
    }

    fun setCustomRange(start: Long, end: Long) {
        _customRange.value = Pair(start, end)
        _dateFilter.value = "مخصص"
    }

    fun toggleAddDialog(show: Boolean) { _uiState.update { it.copy(showAddDialog = show) } }
    fun toggleRangeDialog(show: Boolean) { _uiState.update { it.copy(showRangeDialog = show) } }
    fun toggleManageCategories(show: Boolean) { _uiState.update { it.copy(showManageCategories = show) } }

    fun addExpense(expense: Expense) {
        viewModelScope.launch {
            try {
                AppRepository.addExpense(expense)
                toggleAddDialog(show = false)
            } catch (_: Exception) {
                _uiState.update { it.copy(error = "فشل في إضافة المصروف") }
            }
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            try {
                AppRepository.addExpense(expense) 
            } catch (_: Exception) {
                _uiState.update { it.copy(error = "فشل في تحديث المصروف") }
            }
        }
    }

    fun deleteExpense(id: String, onUndoAvailable: (Expense) -> Unit) {
        viewModelScope.launch {
            try {
                val expenseToDelete = expenses.value.find { it.id == id }
                if (expenseToDelete != null) {
                    AppRepository.deleteExpense(id)
                    onUndoAvailable(expenseToDelete)
                    _uiState.update { it.copy(error = null) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "فشل في حذف المصروف: ${e.message}") }
            }
        }
    }

    fun restoreExpense(expense: Expense) {
        viewModelScope.launch {
            try {
                AppRepository.addExpense(expense)
                _uiState.update { it.copy(error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "فشل في استعادة المصروف: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            try {
                AppRepository.addExpenseCategory(ExpenseCategory(
                    id = "CAT-${PlatformUtils.currentTimeMillis()}",
                    name = name
                ))
                _uiState.update { it.copy(error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "فشل في إضافة التصنيف: ${e.message}") }
            }
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            try {
                AppRepository.deleteExpenseCategory(id)
                _uiState.update { it.copy(error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "فشل في حذف التصنيف: ${e.message}") }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ExpensesViewModel()
            }
        }
    }
}
