package com.example.taskjoy.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskjoy.adapters.UserManagementAdapter.UserItem
import com.example.taskjoy.repository.RepositoryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class UserManagementViewModel(
    private val repository: RepositoryService = RepositoryService()
) : ViewModel() {

    // UI state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _users = MutableLiveData<List<UserItem>>()
    val users: LiveData<List<UserItem>> = _users

    private val _isParent = MutableLiveData<Boolean>()
    val isParent: LiveData<Boolean> = _isParent

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _removeSuccess = MutableLiveData<Boolean>()
    val removeSuccess: LiveData<Boolean> = _removeSuccess

    fun checkUserRole(endUserId: String, currentUserId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val result = withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine<Boolean> { continuation ->
                        repository.checkUserRole(
                            endUserId = endUserId,
                            currentUserId = currentUserId,
                            onSuccess = { isParent ->
                                continuation.resume(isParent)
                            },
                            onError = { exception ->
                                continuation.resumeWithException(exception)
                            }
                        )
                    }
                }
                _isParent.value = result
            } catch (e: Exception) {
                _error.value = e.message ?: "Error checking user role"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadUsers(endUserId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val result = withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine<List<UserItem>> { continuation ->
                        repository.loadUserAccess(
                            endUserId = endUserId,
                            onSuccess = { users ->
                                continuation.resume(users)
                            },
                            onError = { exception ->
                                continuation.resumeWithException(exception)
                            }
                        )
                    }
                }
                _users.value = result
            } catch (e: Exception) {
                _error.value = e.message ?: "Error loading users"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun removeUser(endUserId: String, userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _removeSuccess.value = false

            try {
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine<Unit> { continuation ->
                        repository.removeUserAccess(
                            endUserId = endUserId,
                            userId = userId,
                            onSuccess = {
                                continuation.resume(Unit)
                            },
                            onError = { exception ->
                                continuation.resumeWithException(exception)
                            }
                        )
                    }
                }
                _removeSuccess.value = true

                // Reload the user list
                loadUsers(endUserId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Error removing user"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}