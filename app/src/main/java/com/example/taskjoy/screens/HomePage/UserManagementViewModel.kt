package com.example.taskjoy.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskjoy.adapters.UserManagementAdapter.UserItem
import com.example.taskjoy.repository.RepositoryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserManagementViewModel(
    private val repository: RepositoryService = RepositoryService()
) : ViewModel() {


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
                    repository.checkUserRole(endUserId, currentUserId)
                }

                result.fold(
                    onSuccess = { isParent ->
                        _isParent.value = isParent
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Error checking user role"
                    }
                )
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
                    repository.loadUserAccess(endUserId)
                }

                result.fold(
                    onSuccess = { userList ->
                        _users.value = userList
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Error loading users"
                    }
                )
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
                val result = withContext(Dispatchers.IO) {
                    repository.removeUserAccess(endUserId, userId)
                }

                result.fold(
                    onSuccess = {
                        _removeSuccess.value = true
                        // Reload the user list
                        loadUsers(endUserId)
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Error removing user"
                    }
                )
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