package com.example.taskjoy.screens.RoutineList

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskjoy.model.DailyRoutine
import com.example.taskjoy.repository.RepositoryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

            try {
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine<Unit> { continuation ->
                        repository.createDailyRoutinesIfNeeded(
                            endUserId = endUserId,
                            date = date,
                            onSuccess = {
                                continuation.resume(Unit)
                            },
                            onError = { exception ->
                                continuation.resumeWithException(exception)
                            }
                        )
                    }
                }
                // After ensuring routines exist, get them
                getDailyRoutines(endUserId, date)
            } catch (e: Exception) {
                _error.value = e.message ?: "Error creating routines"
                _isLoading.value = false
            }
        }
    }

    fun getDailyRoutines(endUserId: String, date: Calendar) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val result = withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine<List<DailyRoutine>> { continuation ->
                        repository.getDailyRoutines(
                            endUserId = endUserId,
                            date = date,
                            onSuccess = { routinesList ->
                                continuation.resume(routinesList)
                            },
                            onError = { exception ->
                                continuation.resumeWithException(exception)
                            }
                        )
                    }
                }
                _routines.value = result
            } catch (e: Exception) {
                _error.value = e.message ?: "Error fetching routines"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getAllEndUserDailyRoutines(parentId: String, date: Calendar) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val result = withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine<List<DailyRoutine>> { continuation ->
                        repository.getAllEndUserDailyRoutines(
                            parentId = parentId,
                            date = date,
                            onSuccess = { routinesList ->
                                continuation.resume(routinesList)
                            },
                            onError = { exception ->
                                continuation.resumeWithException(exception)
                            }
                        )
                    }
                }
                _routines.value = result
            } catch (e: Exception) {
                _error.value = e.message ?: "Error fetching routines"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteRoutine(endUserId: String, routine: DailyRoutine) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine<Unit> { continuation ->
                        repository.deleteRoutine(
                            endUserId = endUserId,
                            routine = routine,
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
                _routines.value = _routines.value?.filter { it.id != routine.id }
            } catch (e: Exception) {
                _error.value = e.message ?: "Error deleting routine"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}