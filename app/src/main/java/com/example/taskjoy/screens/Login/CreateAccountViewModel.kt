package com.example.taskjoy.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskjoy.repository.AuthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CreateAccountViewModel(
    private val authService: AuthService = AuthService()
) : ViewModel() {

    // UI state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _createAccountSuccess = MutableLiveData<Boolean>()
    val createAccountSuccess: LiveData<Boolean> = _createAccountSuccess

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    /**
     * Validate the form fields
     */
    fun validateForm(email: String, name: String, passwordOne: String, passwordTwo: String): Pair<Boolean, String?> {
        if (email.isEmpty() || name.isEmpty() || passwordOne.isEmpty() || passwordTwo.isEmpty()) {
            return Pair(false, "Please ensure all fields are filled out")
        } else if ((passwordOne.length < 6) || (passwordOne.length > 20)) {
            return Pair(false, "Please ensure password meets requirements")
        } else if (passwordOne != passwordTwo) {
            return Pair(false, "Passwords do not match")
        } else {
            return Pair(true, null)
        }
    }

    /**
     * Create a new user account
     */
    fun createAccount(email: String, name: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine<Unit> { continuation ->
                        authService.createAccount(
                            email = email,
                            name = name,
                            password = password,
                            onSuccess = {
                                continuation.resume(Unit)
                            },
                            onError = { exception ->
                                continuation.resumeWithException(exception)
                            }
                        )
                    }
                }
                _createAccountSuccess.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to create account"
                _createAccountSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}