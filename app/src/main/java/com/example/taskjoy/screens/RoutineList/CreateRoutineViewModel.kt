package com.example.taskjoy.screens.RoutineList

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskjoy.model.RoutineTemplate
import com.example.taskjoy.model.TaskJoyIcon
import com.example.taskjoy.repository.RepositoryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CreateRoutineViewModel(
    private val repository: RepositoryService = RepositoryService()
) : ViewModel() {

    // UI state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _routineTemplate = MutableLiveData<RoutineTemplate>()
    val routineTemplate: LiveData<RoutineTemplate> = _routineTemplate

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    fun getRoutineTemplate(routineId: String, endUserId: String, currentUserId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val result = withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine<RoutineTemplate> { continuation ->
                        repository.getRoutineTemplate(
                            routineId = routineId,
                            endUserId = endUserId,
                            currentUserId = currentUserId,
                            onSuccess = { template ->
                                continuation.resume(template)
                            },
                            onFailure = { exception ->
                                continuation.resumeWithException(exception)
                            }
                        )
                    }
                }
                _routineTemplate.value = result
            } catch (e: Exception) {
                _error.value = e.message ?: "Error loading routine template"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun checkParentPermission(endUserId: String, currentUserId: String): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()

        viewModelScope.launch {
            _isLoading.value = true

            try {
                val hasPermission = withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine<Boolean> { continuation ->
                        repository.checkParentPermission(
                            endUserId = endUserId,
                            currentUserId = currentUserId,
                            onSuccess = { hasPermission ->
                                continuation.resume(hasPermission)
                            },
                            onFailure = { exception ->
                                continuation.resumeWithException(exception)
                            }
                        )
                    }
                }
                result.value = hasPermission
            } catch (e: Exception) {
                _error.value = e.message ?: "Error checking permissions"
                result.value = false
            } finally {
                _isLoading.value = false
            }
        }

        return result
    }

    fun saveRoutine(
        routineId: String?,
        dailyRoutineId: String?,
        endUserId: String,
        name: String,
        icon: TaskJoyIcon,
        currentUserId: String,
        selectedDate: Calendar
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _saveSuccess.value = false

            try {
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine<Unit> { continuation ->
                        repository.saveRoutine(
                            routineId = routineId,
                            dailyRoutineId = dailyRoutineId,
                            endUserId = endUserId,
                            name = name,
                            icon = icon,
                            currentUserId = currentUserId,
                            selectedDate = selectedDate,
                            onSuccess = {
                                continuation.resume(Unit)
                            },
                            onFailure = { exception ->
                                continuation.resumeWithException(exception)
                            }
                        )
                    }
                }
                _saveSuccess.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Error saving routine"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}