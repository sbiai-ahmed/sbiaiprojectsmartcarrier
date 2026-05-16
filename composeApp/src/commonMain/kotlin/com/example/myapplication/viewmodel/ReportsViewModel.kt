package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.myapplication.database.AppRepository
import com.example.myapplication.models.PhoneDevice
import com.example.myapplication.models.RepairStatus
import com.example.myapplication.models.SaleRecord
import kotlinx.coroutines.flow.*

class ReportsViewModel(private val repository: AppRepository) : ViewModel() {

    private val _devices = repository.devices
    private val _sales = repository.sales
    private val _expenses = repository.expenses
    private val _purchases = repository.purchases

    val reportsState = combine(_devices, _sales, _expenses, _purchases) { devices, sales, expenses, purchases ->
        val completedDevices = devices.filter { it.status == RepairStatus.COMPLETED || it.status == RepairStatus.DELIVERED }
        
        // 1. الإيرادات الأساسية
        val repairRevenue = completedDevices.sumOf { it.estimatedCost }
        val salesRevenue = sales.sumOf { it.amount }
        val totalRevenue = repairRevenue + salesRevenue
        
        val totalExpenses = expenses.sumOf { it.amount }
        val totalPurchases = purchases.sumOf { it.totalCost }
        val totalProfitFromSales = sales.sumOf { it.profit }
        
        val netProfit = totalRevenue - totalExpenses - totalPurchases

        // 2. تحليل أداء الموظفين
        // نجمع بين مبيعات الموظف وإصلاحاته
        val employeePerf = mutableMapOf<String, Double>()
        
        // إصلاحات الموظفين (نستخدم assignedTechnicianId)
        completedDevices.forEach { dev ->
            val emp = dev.assignedTechnicianId.ifBlank { "غير محدد" }
            employeePerf[emp] = (employeePerf[emp] ?: 0.0) + dev.estimatedCost
        }
        // مبيعات الموظفين
        sales.forEach { sale ->
            employeePerf[sale.processedBy] = (employeePerf[sale.processedBy] ?: 0.0) + sale.amount
        }

        // 3. تحليل ربحية الأقسام
        val sectionProfit = mapOf(
            "الصيانة" to repairRevenue, // نعتبر تكلفة الصيانة هنا هي كامل المبلغ للإيراد
            "المبيعات" to totalProfitFromSales
        )

        // 4. البيانات الشهرية (مبسطة)
        val monthlyData = listOf(
            MonthlyReport("الإجمالي الحالي", totalRevenue, totalExpenses + totalPurchases, netProfit)
        )

        // 5. توزيع المصاريف
        val expenseDistribution = expenses.groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } }

        ReportsData(
            devices = devices,
            totalRevenue = totalRevenue,
            netProfit = netProfit,
            deviceCount = completedDevices.size,
            repairRevenue = repairRevenue,
            salesRevenue = salesRevenue,
            monthlyReports = monthlyData,
            expenseDistribution = expenseDistribution,
            employeePerformance = employeePerf,
            sectionProfitability = sectionProfit
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsData()
    )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ReportsViewModel(AppRepository)
            }
        }
    }
}

data class MonthlyReport(val month: String, val revenue: Double, val expenses: Double, val profit: Double)

data class ReportsData(
    val devices: List<PhoneDevice> = emptyList(),
    val totalRevenue: Double = 0.0,
    val netProfit: Double = 0.0,
    val deviceCount: Int = 0,
    val repairRevenue: Double = 0.0,
    val salesRevenue: Double = 0.0,
    val monthlyReports: List<MonthlyReport> = emptyList(),
    val expenseDistribution: Map<String, Double> = emptyMap(),
    val employeePerformance: Map<String, Double> = emptyMap(),
    val sectionProfitability: Map<String, Double> = emptyMap()
)
