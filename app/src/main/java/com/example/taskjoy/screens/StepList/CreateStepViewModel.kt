package com.example.taskjoy.screens.StepList

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskjoy.model.Step
import com.example.taskjoy.model.TaskJoyIcon
import com.example.taskjoy.repository.RepositoryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CreateStepViewModel(
    private val repository: RepositoryService = RepositoryService()
) : ViewModel() {

    // UI state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _step = MutableLiveData<Step>()
    val step: LiveData<Step> = _step

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

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

    fun createStep(
        endUserId: String,
        routineId: String,
        name: String,
        description: String,
        icon: TaskJoyIcon,
        customIconPath: String?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _saveSuccess.value = false

            try {
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine<Unit> { continuation ->
                        repository.createStep(
                            endUserId = endUserId,
                            routineId = routineId,
                            name = name,
                            description = description,
                            icon = icon,
                            customIconPath = customIconPath,
                            onSuccess = {
                                continuation.resume(Unit)
                            },
                            onError = { exception ->
                                continuation.resumeWithException(exception)
                            }
                        )
                    }
                }
                _saveSuccess.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Error creating step"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateStep(
        endUserId: String,
        routineId: String,
        stepId: String,
        templateStepId: String?,
        name: String,
        description: String,
        icon: TaskJoyIcon,
        customIconPath: String?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _saveSuccess.value = false

            try {
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine<Unit> { continuation ->
                        repository.updateStep(
                            endUserId = endUserId,
                            routineId = routineId,
                            stepId = stepId,
                            templateStepId = templateStepId,
                            name = name,
                            description = description,
                            icon = icon,
                            customIconPath = customIconPath,
                            onSuccess = {
                                continuation.resume(Unit)
                            },
                            onError = { exception ->
                                continuation.resumeWithException(exception)
                            }
                        )
                    }
                }
                _saveSuccess.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Error updating step"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}