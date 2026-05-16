package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.myapplication.database.AppRepository
import com.example.myapplication.models.Product
import com.example.myapplication.models.SaleRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SalesViewModel : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    val products: StateFlow<List<Product>> = AppRepository.products
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val sales: StateFlow<List<SaleRecord>> = AppRepository.sales
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun makeSale(product: Product, quantity: Int, actualSoldPrice: Double) {
        viewModelScope.launch {
            try {
                if (product.stockQuantity >= quantity) {
                    AppRepository.makeSale(product, quantity, actualSoldPrice)
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "الكمية المطلوبة غير متوفرة في المخزن"
                }
            } catch (e: Exception) {
                _errorMessage.value = "فشل في تسجيل عملية البيع: ${e.message}"
            }
        }
    }

    fun deleteSale(sale: SaleRecord, onUndoAvailable: (SaleRecord) -> Unit) {
        viewModelScope.launch {
            try {
                AppRepository.deleteSale(sale.saleId)
                onUndoAvailable(sale)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "فشل في حذف السجل: ${e.message}"
            }
        }
    }

    fun restoreSale(sale: SaleRecord) {
        viewModelScope.launch {
            try {
                AppRepository.restoreSale(sale)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "فشل في استعادة السجل: ${e.message}"
            }
        }
    }

    fun updateSale(sale: SaleRecord) {
        viewModelScope.launch {
            try {
                AppRepository.updateSale(sale)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "فشل في تحديث السجل: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SalesViewModel()
            }
        }
    }
}
