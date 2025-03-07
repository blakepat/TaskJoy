package com.example.taskjoy.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskjoy.model.EndUser
import com.example.taskjoy.repository.RepositoryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
    private val repository: RepositoryService = RepositoryService()
) : ViewModel() {

    // UI state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _children = MutableLiveData<List<EndUser>>()
    val children: LiveData<List<EndUser>> = _children

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun getChildren(parentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val result = withContext(Dispatchers.IO) {
                    repository.getChildren(parentId)
                }

                result.fold(
                    onSuccess = { childList ->
                        _children.value = childList
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Unknown error occurred"
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteEndUser(parentId: String, endUserId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val result = withContext(Dispatchers.IO) {
                    repository.deleteEndUser(parentId, endUserId)
                }

                result.fold(
                    onSuccess = {
                        // Refresh the children list after successful deletion
                        getChildren(parentId)
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Error deleting user"
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Error deleting user"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}