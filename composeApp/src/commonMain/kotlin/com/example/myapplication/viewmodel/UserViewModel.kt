package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.myapplication.database.AppRepository
import com.example.myapplication.models.User
import com.example.myapplication.models.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    val users: StateFlow<List<User>> = AppRepository.users
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun registerEmployee(name: String, email: String, pass: String, role: UserRole, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                // نستخدم الدالة السحابية لربط الموظف بالمحل آلياً
                val success = AppRepository.registerNewEmployee(email, pass, name)
                if (success) {
                    onSuccess()
                } else {
                    _errorMessage.value = "فشل في تسجيل الموظف. قد يكون البريد مسجلاً مسبقاً."
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "خطأ غير متوقع"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            try {
                AppRepository.deleteUser(userId)
            } catch (e: Exception) {
                _errorMessage.value = "فشل في حذف المستخدم"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                UserViewModel()
            }
        }
    }
}
