package com.example.taskjoy.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskjoy.model.Step
import com.example.taskjoy.repository.RepositoryService
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StepDetailsViewModel(
    val repository: RepositoryService = RepositoryService()
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

    private val _hasIncompleteSteps = MutableLiveData<Boolean>()
    val hasIncompleteSteps: LiveData<Boolean> = _hasIncompleteSteps

    fun getStep(endUserId: String, routineId: String, stepId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val result = withContext(Dispatchers.IO) {
                    repository.getStep(endUserId, routineId, stepId)
                }

                result.fold(
                    onSuccess = { step ->
                        _step.value = step
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Error loading step"
                    }
                )
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
                val result = withContext(Dispatchers.IO) {
                    repository.markStepAsComplete(endUserId, routineId, stepId)
                }

                result.fold(
                    onSuccess = { timestamp ->
                        // Update the local step object
                        _step.value = _step.value?.copy(completed = true, completedAt = timestamp)
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Error marking step as complete"
                    }
                )
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
                val result = withContext(Dispatchers.IO) {
                    repository.markStepAsIncomplete(endUserId, routineId, stepId)
                }

                result.fold(
                    onSuccess = {
                        // Update the local step object
                        _step.value = _step.value?.copy(completed = false, completedAt = null)
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Error marking step as incomplete"
                    }
                )
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
                val result = withContext(Dispatchers.IO) {
                    repository.saveStepNotes(endUserId, routineId, stepId, notes)
                }

                result.fold(
                    onSuccess = {
                        // Update the local step object
                        _step.value = _step.value?.copy(notes = notes)
                        _saveNotesSuccess.value = true
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Error saving notes"
                    }
                )
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
                val result = withContext(Dispatchers.IO) {
                    repository.completeAllSteps(endUserId, routineId)
                }

                result.fold(
                    onSuccess = { timestamp ->
                        // Update the local step object to mark it as completed
                        _step.value = _step.value?.copy(completed = true, completedAt = timestamp)
                        _allStepsCompleted.value = true
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Error completing all steps"
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Error completing all steps"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Add this method to StepDetailsViewModel class
    fun areAnyStepsIncomplete(
        endUserId: String,
        routineId: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val result = withContext(Dispatchers.IO) {
                    repository.getRoutineWithSteps(endUserId, routineId)
                }

                val hasIncompleteSteps = result.fold(
                    onSuccess = { (_, steps) -> steps.any { !it.completed } },
                    onFailure = {
                        _error.value = it.message ?: "Error checking step completion"
                        true // Assume incomplete on error
                    }
                )

                // Return result on main thread
                withContext(Dispatchers.Main) {
                    onResult(hasIncompleteSteps)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Error checking step completion"
                // Return true (incomplete) on error
                withContext(Dispatchers.Main) {
                    onResult(true)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun checkForIncompleteSteps(endUserId: String, routineId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val result = withContext(Dispatchers.IO) {
                    repository.getRoutineWithSteps(endUserId, routineId)
                }

                result.fold(
                    onSuccess = { (_, steps) ->
                        _hasIncompleteSteps.value = steps.any { !it.completed }
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Error checking routine completion"
                        _hasIncompleteSteps.value = true // Default to true if there's an error
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Error checking routine completion"
                _hasIncompleteSteps.value = true // Default to true if there's an error
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun clearError() {
        _error.value = null
    }
}