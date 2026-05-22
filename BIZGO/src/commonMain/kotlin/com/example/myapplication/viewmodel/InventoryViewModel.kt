package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.myapplication.database.AppRepository
import com.example.myapplication.models.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InventoryViewModel : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    val products: StateFlow<List<Product>> = AppRepository.products
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addProduct(product: Product) {
        viewModelScope.launch {
            try {
                AppRepository.addProduct(product)
                _errorMessage.value = null
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "فشل في إضافة المنتج: ${e.message}"
            }
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            try {
                AppRepository.updateProduct(product)
                _errorMessage.value = null
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "فشل في تحديث المنتج: ${e.message}"
            }
        }
    }

    fun deleteProduct(productId: String, onUndoAvailable: (Product) -> Unit) {
        viewModelScope.launch {
            val productToDelete = products.value.find { it.productId == productId }
            if (productToDelete != null) {
                try {
                    AppRepository.deleteProduct(productId)
                    onUndoAvailable(productToDelete)
                    _errorMessage.value = null
                } catch (e: Exception) {
                    e.printStackTrace()
                    _errorMessage.value = "فشل الحذف: ${e.message}"
                }
            }
        }
    }

    fun restoreProduct(product: Product) {
        viewModelScope.launch {
            try {
                AppRepository.addProduct(product)
                _errorMessage.value = null
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "فشل استرجاع المنتج: ${e.message}"
            }
        }
    }

    fun clearError() { _errorMessage.value = null }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { 
                InventoryViewModel()
            }
        }
    }
}
