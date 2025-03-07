package com.example.taskjoy.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskjoy.model.RoutineTemplate
import com.example.taskjoy.model.TaskJoyIcon
import com.example.taskjoy.repository.RepositoryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

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

            repository.getRoutineTemplate(routineId, endUserId, currentUserId).fold(
                onSuccess = { template ->
                    _routineTemplate.value = template
                    _isLoading.value = false
                },
                onFailure = { e ->
                    _error.value = e.message ?: "Error loading routine template"
                    _isLoading.value = false
                }
            )
        }
    }

    fun checkParentPermission(endUserId: String, currentUserId: String): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        viewModelScope.launch {
            repository.checkParentPermission(endUserId, currentUserId).fold(
                onSuccess = { hasPermission ->
                    result.value = hasPermission
                },
                onFailure = { e ->
                    _error.value = e.message ?: "Error checking permissions"
                    result.value = false
                }
            )
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

            repository.saveRoutine(
                routineId, dailyRoutineId, endUserId, name, icon,
                currentUserId, selectedDate
            ).fold(
                onSuccess = {
                    _saveSuccess.value = true
                    _isLoading.value = false
                },
                onFailure = { e ->
                    _error.value = e.message ?: "Error saving routine"
                    _isLoading.value = false
                }
            )
        }
    }

    fun clearError() {
        _error.value = null
    }
}