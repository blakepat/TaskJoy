package com.example.taskjoy.repository

import com.example.taskjoy.model.DailyRoutine
import com.example.taskjoy.model.Step
import com.example.taskjoy.model.TaskJoyIcon
import com.google.firebase.Timestamp

/**
 * Repository interface for step-related operations
 */
interface StepRepository {
    /**
     * Get a routine with its steps
     */
    fun getRoutineWithSteps(
        endUserId: String,
        routineId: String,
        onSuccess: (DailyRoutine, List<Step>) -> Unit,
        onError: (Exception) -> Unit
    )

    /**
     * Save the order of steps
     */
    fun saveStepOrder(
        endUserId: String,
        routineId: String,
        steps: List<Step>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    )

    /**
     * Update the order of the remaining steps after deletion
     */
    fun updateRemainingStepsOrder(
        endUserId: String,
        routineId: String,
        steps: List<Step>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    )

    /**
     * Delete a step
     */
    fun deleteStep(
        endUserId: String,
        routineId: String,
        step: Step,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    )

    /**
     * Load an existing step for editing
     */
    fun getStep(
        endUserId: String,
        routineId: String,
        stepId: String,
        onSuccess: (Step) -> Unit,
        onError: (Exception) -> Unit
    )

    /**
     * Create a new step
     */
    fun createStep(
        endUserId: String,
        routineId: String,
        name: String,
        description: String,
        icon: TaskJoyIcon,
        customIconPath: String?,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    )

    /**
     * Update an existing step
     */
    fun updateStep(
        endUserId: String,
        routineId: String,
        stepId: String,
        templateStepId: String?,
        name: String,
        description: String,
        icon: TaskJoyIcon,
        customIconPath: String?,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    )

    /**
     * Mark a step as complete
     */
    fun markStepAsComplete(
        endUserId: String,
        routineId: String,
        stepId: String,
        onSuccess: (Timestamp) -> Unit,
        onError: (Exception) -> Unit
    )

    /**
     * Mark a step as incomplete
     */
    fun markStepAsIncomplete(
        endUserId: String,
        routineId: String,
        stepId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    )

    /**
     * Save notes for a step
     */
    fun saveStepNotes(
        endUserId: String,
        routineId: String,
        stepId: String,
        notes: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    )

    /**
     * Mark all steps in a routine as complete
     */
    fun completeAllSteps(
        endUserId: String,
        routineId: String,
        onSuccess: (Timestamp) -> Unit,
        onError: (Exception) -> Unit
    )
}