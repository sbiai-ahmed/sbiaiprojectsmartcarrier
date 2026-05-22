package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.myapplication.database.AppRepository
import com.example.myapplication.models.PhoneDevice
import com.example.myapplication.models.RepairStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RepairsViewModel : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    val devices: StateFlow<List<PhoneDevice>> = AppRepository.devices
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateDevice(device: PhoneDevice, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                AppRepository.updateDevice(device)
                onSuccess()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "فشل في تحديث الجهاز: ${e.message}"
            }
        }
    }

    fun updateStatus(deviceId: String, newStatus: RepairStatus) {
        viewModelScope.launch {
            try {
                AppRepository.updateDeviceStatus(deviceId, newStatus)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "فشل في تحديث الحالة: ${e.message}"
            }
        }
    }

    fun deleteDevice(deviceId: String, onUndoAvailable: (PhoneDevice) -> Unit) {
        viewModelScope.launch {
            try {
                val deviceToDelete = devices.value.find { it.deviceId == deviceId }
                if (deviceToDelete != null) {
                    AppRepository.deleteDevice(deviceId)
                    onUndoAvailable(deviceToDelete)
                    _errorMessage.value = null
                }
            } catch (e: Exception) {
                _errorMessage.value = "فشل في حذف الجهاز: ${e.message}"
            }
        }
    }

    fun restoreDevice(device: PhoneDevice) {
        viewModelScope.launch {
            try {
                AppRepository.addDevice(device)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "فشل في استعادة الجهاز: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                RepairsViewModel()
            }
        }
    }
}
