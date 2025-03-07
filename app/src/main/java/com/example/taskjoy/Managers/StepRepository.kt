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
    suspend fun getRoutineWithSteps(
        endUserId: String,
        routineId: String
    ): Result<Pair<DailyRoutine, List<Step>>>

    /**
     * Save the order of steps
     */
    suspend fun saveStepOrder(
        endUserId: String,
        routineId: String,
        steps: List<Step>
    ): Result<Unit>

    /**
     * Update the order of the remaining steps after deletion
     */
    suspend fun updateRemainingStepsOrder(
        endUserId: String,
        routineId: String,
        steps: List<Step>
    ): Result<Unit>

    /**
     * Delete a step
     */
    suspend fun deleteStep(
        endUserId: String,
        routineId: String,
        step: Step
    ): Result<Unit>

    /**
     * Load an existing step for editing
     */
    suspend fun getStep(
        endUserId: String,
        routineId: String,
        stepId: String
    ): Result<Step>

    /**
     * Create a new step
     */
    suspend fun createStep(
        endUserId: String,
        routineId: String,
        name: String,
        description: String,
        icon: TaskJoyIcon,
        customIconPath: String?
    ): Result<Unit>

    /**
     * Update an existing step
     */
    suspend fun updateStep(
        endUserId: String,
        routineId: String,
        stepId: String,
        templateStepId: String?,
        name: String,
        description: String,
        icon: TaskJoyIcon,
        customIconPath: String?
    ): Result<Unit>

    /**
     * Mark a step as complete
     */
    suspend fun markStepAsComplete(
        endUserId: String,
        routineId: String,
        stepId: String
    ): Result<Timestamp>

    /**
     * Mark a step as incomplete
     */
    suspend fun markStepAsIncomplete(
        endUserId: String,
        routineId: String,
        stepId: String
    ): Result<Unit>

    /**
     * Save notes for a step
     */
    suspend fun saveStepNotes(
        endUserId: String,
        routineId: String,
        stepId: String,
        notes: String
    ): Result<Unit>

    /**
     * Mark all steps in a routine as complete
     */
    suspend fun completeAllSteps(
        endUserId: String,
        routineId: String
    ): Result<Timestamp>
}