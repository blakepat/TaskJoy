package com.example.taskjoy.screens.StepDetails

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskjoy.model.Step
import com.example.taskjoy.repository.RepositoryService
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class StepDetailsViewModel(
    private val repository: RepositoryService = RepositoryService()
) : ViewModel() {

    // UI state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _step = MutableLiveData<Step>()
    val step: LiveData<Step> = _step

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _saveNotesSuccess = MutableLiveData<Boolean>()
    val saveNotesSuccess: LiveData<Boolean> = _saveNotesSuccess

    private val _allStepsCompleted = MutableLiveData<Boolean>()
    val allStepsCompleted: LiveData<Boolean> = _allStepsCompleted

    fun getStep(endUserId: String, routineId: String, stepId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val result = withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine<Step> { continuation ->
                        repository.getStep(
                            endUserId = endUserId,
                            routineId = routineId,
                            stepId = stepId,
                            onSuccess = { step ->
                                continuation.resume(step)
                            },
                            onError = { exception ->
                                continuation.resumeWithException(exception)
                            }
                        )
                    }
                }
                _step.value = result
            } catch (e: Exception) {
                _error.value = e.message ?: "Error loading step"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markStepAsComplete(endUserId: String, routineId: String, stepId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val timestamp = withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine<Timestamp> { continuation ->
                        repository.markStepAsComplete(
                            endUserId = endUserId,
                            routineId = routineId,
                            stepId = stepId,
                            onSuccess = { timestamp ->
                                continuation.resume(timestamp)
                            },
                            onError = { exception ->
                                continuation.resumeWithException(exception)
                            }
                        )
                    }
                }

                // Update the local step object
                _step.value = _step.value?.copy(completed = true, completedAt = timestamp)
            } catch (e: Exception) {
                _error.value = e.message ?: "Error marking step as complete"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markStepAsIncomplete(endUserId: String, routineId: String, stepId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine<Unit> { continuation ->
                        repository.markStepAsIncomplete(
                            endUserId = endUserId,
                            routineId = routineId,
                            stepId = stepId,
                            onSuccess = {
                                continuation.resume(Unit)
                            },
                            onError = { exception ->
                                continuation.resumeWithException(exception)
                            }
                        )
                    }
                }

                // Update the local step object
                _step.value = _step.value?.copy(completed = false, completedAt = null)
            } catch (e: Exception) {
                _error.value = e.message ?: "Error marking step as incomplete"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveStepNotes(endUserId: String, routineId: String, stepId: String, notes: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _saveNotesSuccess.value = false

            try {
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine<Unit> { continuation ->
                        repository.saveStepNotes(
                            endUserId = endUserId,
                            routineId = routineId,
                            stepId = stepId,
                            notes = notes,
                            onSuccess = {
                                continuation.resume(Unit)
                            },
                            onError = { exception ->
                                continuation.resumeWithException(exception)
                            }
                        )
                    }
                }

                // Update the local step object
                _step.value = _step.value?.copy(notes = notes)
                _saveNotesSuccess.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Error saving notes"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun completeAllSteps(endUserId: String, routineId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _allStepsCompleted.value = false

            try {
                val timestamp = withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine<Timestamp> { continuation ->
                        repository.completeAllSteps(
                            endUserId = endUserId,
                            routineId = routineId,
                            onSuccess = { timestamp ->
                                continuation.resume(timestamp)
                            },
                            onError = { exception ->
                                continuation.resumeWithException(exception)
                            }
                        )
                    }
                }

                // Update the local step object to mark it as completed
                _step.value = _step.value?.copy(completed = true, completedAt = timestamp)
                _allStepsCompleted.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Error completing all steps"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}