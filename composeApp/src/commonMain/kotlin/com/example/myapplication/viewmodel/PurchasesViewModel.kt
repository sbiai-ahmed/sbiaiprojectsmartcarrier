package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.myapplication.database.AppRepository
import com.example.myapplication.models.PurchaseRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PurchasesViewModel : ViewModel() {

    val purchases: StateFlow<List<PurchaseRecord>> = AppRepository.purchases
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addPurchase(purchase: PurchaseRecord, onComplete: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                AppRepository.addPurchase(purchase)
                onComplete()
            } catch (_: Exception) {
                onError()
            }
        }
    }

    fun updatePurchase(purchase: PurchaseRecord, onComplete: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                AppRepository.updatePurchase(purchase)
                onComplete()
            } catch (_: Exception) {
                onError()
            }
        }
    }

    fun deletePurchase(purchaseId: String, onUndoAvailable: (PurchaseRecord) -> Unit) {
        viewModelScope.launch {
            val purchaseToDelete = purchases.value.find { it.purchaseId == purchaseId }
            if (purchaseToDelete != null) {
                AppRepository.deletePurchase(purchaseId)
                onUndoAvailable(purchaseToDelete)
            }
        }
    }

    fun restorePurchase(purchase: PurchaseRecord) {
        viewModelScope.launch {
            AppRepository.addPurchase(purchase)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PurchasesViewModel()
            }
        }
    }
}
