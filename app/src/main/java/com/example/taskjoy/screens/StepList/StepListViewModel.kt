package com.example.taskjoy.screens.StepList

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskjoy.model.DailyRoutine
import com.example.taskjoy.model.Step
import com.example.taskjoy.repository.RepositoryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class StepListViewModel(
    private val repository: RepositoryService = RepositoryService()
) : ViewModel() {

    // UI state
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

            try {
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine<Pair<DailyRoutine, List<Step>>> { continuation ->
                        repository.getRoutineWithSteps(
                            endUserId = endUserId,
                            routineId = routineId,
                            onSuccess = { routine, steps ->
                                continuation.resume(Pair(routine, steps))
                            },
                            onError = { exception ->
                                continuation.resumeWithException(exception)
                            }
                        )
                    }
                }.let { (routine, steps) ->
                    _routine.value = routine
                    _steps.value = steps
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Error fetching routine and steps"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveStepOrder(endUserId: String, routineId: String, steps: List<Step>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _saveOrderSuccess.value = false

            try {
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine<Unit> { continuation ->
                        repository.saveStepOrder(
                            endUserId = endUserId,
                            routineId = routineId,
                            steps = steps,
                            onSuccess = {
                                continuation.resume(Unit)
                            },
                            onError = { exception ->
                                continuation.resumeWithException(exception)
                            }
                        )
                    }
                }
                _saveOrderSuccess.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Error saving step order"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateRemainingStepsOrder(endUserId: String, routineId: String, steps: List<Step>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine<Unit> { continuation ->
                        repository.updateRemainingStepsOrder(
                            endUserId = endUserId,
                            routineId = routineId,
                            steps = steps,
                            onSuccess = {
                                continuation.resume(Unit)
                            },
                            onError = { exception ->
                                continuation.resumeWithException(exception)
                            }
                        )
                    }
                }
                // Update local list to ensure UI is consistent
                _steps.value = steps
            } catch (e: Exception) {
                _error.value = e.message ?: "Error updating step order"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteStep(endUserId: String, routineId: String, step: Step) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine<Unit> { continuation ->
                        repository.deleteStep(
                            endUserId = endUserId,
                            routineId = routineId,
                            step = step,
                            onSuccess = {
                                continuation.resume(Unit)
                            },
                            onError = { exception ->
                                continuation.resumeWithException(exception)
                            }
                        )
                    }
                }

                // Update local list after successful deletion
                _steps.value = _steps.value?.filter { it.id != step.id }
            } catch (e: Exception) {
                _error.value = e.message ?: "Error deleting step"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}