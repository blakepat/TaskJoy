package com.example.taskjoy.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskjoy.repository.AuthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginViewModel(
    private val authService: AuthService = AuthService()
) : ViewModel() {

    // UI state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _loginSuccess = MutableLiveData<Boolean>()
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val result = withContext(Dispatchers.IO) {
                    authService.login(email, password)
                }

                result.fold(
                    onSuccess = {
                        _loginSuccess.value = true
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Login failed"
                        _loginSuccess.value = false
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Login failed"
                _loginSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun isUserAuthenticated(): Boolean {
        return authService.isUserAuthenticated()
    }

    fun clearError() {
        _error.value = null
    }
}