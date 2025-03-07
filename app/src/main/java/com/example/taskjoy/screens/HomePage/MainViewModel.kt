package com.example.taskjoy.screens.HomePage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskjoy.model.EndUser
import com.example.taskjoy.repository.RepositoryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
                    suspendCancellableCoroutine<List<EndUser>> { continuation ->
                        repository.getChildren(
                            parentId = parentId,
                            onSuccess = { childList ->
                                continuation.resume(childList)
                            },
                            onError = { exception ->
                                continuation.resumeWithException(exception)
                            }
                        )
                    }
                }
                _children.value = result
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
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine<Unit> { continuation ->
                        repository.deleteEndUser(
                            parentId = parentId,
                            endUserId = endUserId,
                            onSuccess = {
                                continuation.resume(Unit)
                            },
                            onError = { exception ->
                                continuation.resumeWithException(exception)
                            }
                        )
                    }
                }

                // Refresh the children list after successful deletion
                getChildren(parentId)
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