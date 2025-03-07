package com.example.taskjoy.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskjoy.model.DailyRoutine
import com.example.taskjoy.repository.RepositoryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class RoutineListViewModel(
    private val repository: RepositoryService = RepositoryService()
) : ViewModel() {

    // UI state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _routines = MutableLiveData<List<DailyRoutine>>()
    val routines: LiveData<List<DailyRoutine>> = _routines

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun createDailyRoutinesIfNeeded(endUserId: String, date: Calendar) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.createDailyRoutinesIfNeeded(endUserId, date).fold(
                onSuccess = {
                    // After ensuring routines exist, get them
                    getDailyRoutines(endUserId, date)
                },
                onFailure = { e ->
                    _error.value = e.message ?: "Error creating routines"
                    _isLoading.value = false
                }
            )
        }
    }

    fun getDailyRoutines(endUserId: String, date: Calendar) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.getDailyRoutines(endUserId, date).fold(
                onSuccess = { routinesList ->
                    _routines.value = routinesList
                    _isLoading.value = false
                },
                onFailure = { e ->
                    _error.value = e.message ?: "Error fetching routines"
                    _isLoading.value = false
                }
            )
        }
    }

    fun getAllEndUserDailyRoutines(parentId: String, date: Calendar) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.getAllEndUserDailyRoutines(parentId, date).fold(
                onSuccess = { routinesList ->
                    _routines.value = routinesList
                    _isLoading.value = false
                },
                onFailure = { e ->
                    _error.value = e.message ?: "Error fetching routines"
                    _isLoading.value = false
                }
            )
        }
    }

    fun deleteRoutine(endUserId: String, routine: DailyRoutine) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.deleteRoutine(endUserId, routine).fold(
                onSuccess = {
                    // Update local list after successful deletion
                    _routines.value = _routines.value?.filter { it.id != routine.id }
                    _isLoading.value = false
                },
                onFailure = { e ->
                    _error.value = e.message ?: "Error deleting routine"
                    _isLoading.value = false
                }
            )
        }
    }

    fun clearError() {
        _error.value = null
    }
}