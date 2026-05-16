package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.myapplication.database.AppRepository
import com.example.myapplication.models.RepairStatus
import com.example.myapplication.models.Expense
import com.example.myapplication.models.PurchaseRecord
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*

class AdminReportsViewModel(repository: AppRepository) : ViewModel() {

    private val _dateRange = MutableStateFlow(Pair(0L, Long.MAX_VALUE))
    private val _selectedFilter = MutableStateFlow("الكل")
    val selectedFilter = _selectedFilter.asStateFlow()
    
    val profitStats = combine(
        repository.sales,
        repository.devices,
        repository.expenses,
        repository.purchases,
        _dateRange,
    ) { sales, devices, expenses, purchases, range ->
        val (start, end) = range
        
        // تصفية البيانات حسب التاريخ المختار
        val filteredSales = sales.filter { (it.date in start..end) }
        val filteredDevices = devices.filter { (it.entryDate in start..end) && (it.status == RepairStatus.COMPLETED || it.status == RepairStatus.DELIVERED) }
        val filteredExpenses = expenses.filter { (it.date in start..end) }
        val filteredPurchases = purchases.filter { (it.date in start..end) }

        val totalRevenue = filteredSales.sumOf { it.amount } + filteredDevices.sumOf { it.estimatedCost }
        val totalSalesProfit = filteredSales.sumOf { it.profit }
        val repairRevenue = filteredDevices.sumOf { it.estimatedCost }
        val totalExpenses = filteredExpenses.sumOf { it.amount }
        val totalPurchasesValue = filteredPurchases.sumOf { it.totalCost }

        // الصافي الحقيقي = (أرباح المبيعات + إيراد الصيانة) - المصاريف
        val netProfit = (totalSalesProfit + repairRevenue) - totalExpenses

        ProfitData(
            totalRevenue = totalRevenue,
            netProfit = netProfit,
            repairRevenue = repairRevenue,
            salesProfit = totalSalesProfit,
            expenseTotal = totalExpenses,
            purchaseTotal = totalPurchasesValue,
            repairCount = filteredDevices.size,
            salesCount = filteredSales.size,
            expensesList = filteredExpenses,
            purchasesList = filteredPurchases,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfitData())

    fun setFilter(filterType: String) {
        _selectedFilter.value = filterType
        val now = Clock.System.now()
        val timeZone = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(timeZone).date
        
        when (filterType) {
            "اليوم" -> {
                val start = today.atStartOfDayIn(timeZone).toEpochMilliseconds()
                val end = today.atTime(23, 59, 59).toInstant(timeZone).toEpochMilliseconds()
                _dateRange.value = Pair(start, end)
            }
            "هذا الأسبوع" -> {
                // نعتبر الأسبوع يبدأ من يوم السبت (بداية الأسبوع في بعض الدول العربية) أو الأحد
                // للتبسيط، نأخذ آخر 7 أيام
                val start = now.minus(7, DateTimeUnit.DAY, timeZone).toEpochMilliseconds()
                _dateRange.value = Pair(start, Long.MAX_VALUE)
            }
            "هذا الشهر" -> {
                val start = LocalDate(today.year, today.month, 1).atStartOfDayIn(timeZone).toEpochMilliseconds()
                _dateRange.value = Pair(start, Long.MAX_VALUE)
            }
            "الكل" -> {
                _dateRange.value = Pair(0L, Long.MAX_VALUE)
            }
        }
    }

    fun setCustomRange(start: Long, end: Long) {
        _selectedFilter.value = "مخصص"
        _dateRange.value = Pair(start, end)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { AdminReportsViewModel(AppRepository) }
        }
    }
}

data class ProfitData(
    val totalRevenue: Double = 0.0,
    val netProfit: Double = 0.0,
    val repairRevenue: Double = 0.0,
    val salesProfit: Double = 0.0,
    val expenseTotal: Double = 0.0,
    val purchaseTotal: Double = 0.0,
    val repairCount: Int = 0,
    val salesCount: Int = 0,
    val expensesList: List<Expense> = emptyList(),
    val purchasesList: List<PurchaseRecord> = emptyList(),
)
