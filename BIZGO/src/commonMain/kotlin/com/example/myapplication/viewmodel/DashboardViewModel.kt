package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.myapplication.database.AppRepository
import com.example.myapplication.models.RepairStatus
import com.example.myapplication.models.User
import com.example.myapplication.utils.PlatformUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DashboardViewModel(private val repository: AppRepository) : ViewModel() {

    private val _devices = repository.devices
    private val _sales = repository.sales
    private val _expenses = repository.expenses
    private val _purchases = repository.purchases
    private val _products = repository.products
    
    val auditLogs = repository.auditLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val dashboardState = combine(_devices, _sales, _expenses, _purchases, _products) { devices, sales, expenses, purchases, products ->
        val repairingCount = devices.count { (it.status == RepairStatus.REPAIRING) || (it.status == RepairStatus.PENDING) }
        
        // حساب إيرادات الصيانة للأجهزة التي اكتملت أو سُلمت
        val repairRevenue = devices.filter { it.status == RepairStatus.COMPLETED || it.status == RepairStatus.DELIVERED }
            .sumOf { it.estimatedCost }
            
        // حساب أرباح المبيعات (السعر الفعلي - سعر الشراء)
        val salesRevenue = sales.sumOf { it.amount }
        val totalSalesProfit = sales.sumOf { it.profit }
        
        val totalExpenses = expenses.sumOf { it.amount }
        val totalPurchasesValue = purchases.sumOf { it.totalCost }
        
        // المعادلة المطلوبة: صافي الربح = (أرباح السلع + إيرادات الصيانة) - المصاريف
        val netProfit = (totalSalesProfit + repairRevenue) - totalExpenses

        val lowStockCount = products.count { it.stockQuantity <= it.minStockAlert }

        val now = PlatformUtils.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L
        val weeklyRevenue = (0..6).map { dayIndex ->
            val start = now - (dayIndex + 1) * dayMillis
            val end = now - dayIndex * dayMillis
            val dRepair = devices.filter { (it.status == RepairStatus.COMPLETED || it.status == RepairStatus.DELIVERED) && it.entryDate in start..end }.sumOf { it.estimatedCost }
            val dSales = sales.filter { it.date in start..end }.sumOf { it.amount }
            dRepair + dSales
        }.reversed()

        val expenseDistribution = expenses.groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } }

        DashboardData(
            netProfit = netProfit,
            repairingCount = repairingCount,
            lowStockCount = lowStockCount,
            totalRevenue = salesRevenue + repairRevenue,
            totalExpenses = totalExpenses,
            totalPurchases = totalPurchasesValue,
            weeklyRevenue = weeklyRevenue,
            expenseDistribution = expenseDistribution
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardData()
    )

    fun updateProfile(updatedUser: User, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.updateUser(updatedUser)
                AppRepository.currentUser = updatedUser
                onResult(true)
            } catch (_: Exception) {
                onResult(false)
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DashboardViewModel(AppRepository)
            }
        }
    }
}

data class DashboardData(
    val netProfit: Double = 0.0,
    val repairingCount: Int = 0,
    val lowStockCount: Int = 0,
    val totalRevenue: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val totalPurchases: Double = 0.0,
    val weeklyRevenue: List<Double> = emptyList(),
    val expenseDistribution: Map<String, Double> = emptyMap()
)
