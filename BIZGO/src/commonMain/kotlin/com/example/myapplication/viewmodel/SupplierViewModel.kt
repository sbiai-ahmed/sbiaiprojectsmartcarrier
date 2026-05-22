package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.myapplication.database.AppRepository
import com.example.myapplication.models.Supplier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SupplierViewModel : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    val suppliers: StateFlow<List<Supplier>> = AppRepository.suppliers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addSupplier(supplier: Supplier) {
        viewModelScope.launch {
            try {
                AppRepository.addSupplier(supplier)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "فشل في إضافة المورد: ${e.message}"
            }
        }
    }

    fun updateSupplier(supplier: Supplier) {
        viewModelScope.launch {
            try {
                AppRepository.updateSupplier(supplier)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "فشل في تحديث بيانات المورد: ${e.message}"
            }
        }
    }

    fun deleteSupplier(id: String, onUndoAvailable: (Supplier) -> Unit) {
        viewModelScope.launch {
            try {
                val supplierToDelete = suppliers.value.find { it.supplierId == id }
                if (supplierToDelete != null) {
                    AppRepository.deleteSupplier(id)
                    onUndoAvailable(supplierToDelete)
                    _errorMessage.value = null
                }
            } catch (e: Exception) {
                _errorMessage.value = "فشل في حذف المورد: ${e.message}"
            }
        }
    }

    fun addSupplierPayment(supplierId: String, amount: Double) {
        viewModelScope.launch {
            try {
                AppRepository.addSupplierPayment(supplierId, amount)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "فشل في تسجيل الدفعة: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SupplierViewModel()
            }
        }
    }
}
