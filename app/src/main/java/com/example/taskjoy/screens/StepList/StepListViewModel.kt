package com.example.taskjoy.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskjoy.model.DailyRoutine
import com.example.taskjoy.model.Step
import com.example.taskjoy.repository.RepositoryService
import kotlinx.coroutines.launch

class StepListViewModel(
    private val repository: RepositoryService = RepositoryService()
) : ViewModel() {


    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _routine = MutableLiveData<DailyRoutine>()
    val routine: LiveData<DailyRoutine> = _routine

    private val _steps = MutableLiveData<List<Step>>()
    val steps: LiveData<List<Step>> = _steps

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _saveOrderSuccess = MutableLiveData<Boolean>()
    val saveOrderSuccess: LiveData<Boolean> = _saveOrderSuccess

    fun getRoutineWithSteps(endUserId: String, routineId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.getRoutineWithSteps(endUserId, routineId).fold(
                onSuccess = { (routine, steps) ->
                    _routine.value = routine
                    _steps.value = steps
                    _isLoading.value = false
                },
                onFailure = { e ->
                    _error.value = e.message ?: "Error fetching routine and steps"
                    _isLoading.value = false
                }
            )
        }
    }

    fun saveStepOrder(endUserId: String, routineId: String, steps: List<Step>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _saveOrderSuccess.value = false

            repository.saveStepOrder(endUserId, routineId, steps).fold(
                onSuccess = {
                    _saveOrderSuccess.value = true
                    _isLoading.value = false
                },
                onFailure = { e ->
                    _error.value = e.message ?: "Error saving step order"
                    _isLoading.value = false
                }
            )
        }
    }

    fun updateRemainingStepsOrder(endUserId: String, routineId: String, steps: List<Step>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.updateRemainingStepsOrder(endUserId, routineId, steps).fold(
                onSuccess = {
                    _steps.value = steps
                    _isLoading.value = false
                },
                onFailure = { e ->
                    _error.value = e.message ?: "Error updating step order"
                    _isLoading.value = false
                }
            )
        }
    }

    fun deleteStep(endUserId: String, routineId: String, step: Step) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.deleteStep(endUserId, routineId, step).fold(
                onSuccess = {
                    _steps.value = _steps.value?.filter { it.id != step.id }
                    _isLoading.value = false
                },
                onFailure = { e ->
                    _error.value = e.message ?: "Error deleting step"
                    _isLoading.value = false
                }
            )
        }
    }

    fun clearError() {
        _error.value = null
    }
}