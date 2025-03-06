package com.example.taskjoy.repository

import com.example.taskjoy.model.DailyRoutine
import com.example.taskjoy.model.EndUser
import com.example.taskjoy.model.RoutineTemplate
import com.example.taskjoy.model.Step
import com.example.taskjoy.model.TaskJoyIcon
import com.google.firebase.Timestamp
import java.util.Calendar

/**
 * Service façade that provides a single access point to all repositories
 * Uses dependency injection to allow for testing with mock repositories
 */
class RepositoryService(
    private val userRepository: UserRepository = FirebaseUserRepository(),
    private val routineRepository: RoutineRepository = FirebaseRoutineRepository(),
    private val templateRepository: TemplateRepository = FirebaseTemplateRepository(),
    private val stepRepository: StepRepository = FirebaseStepRepository()
) {
    // ====== USER OPERATIONS ======

    /**
     * Get all children for a parent
     */
    fun getChildren(
        parentId: String,
        onSuccess: (List<EndUser>) -> Unit,
        onError: (Exception) -> Unit
    ) = userRepository.getChildren(parentId, onSuccess, onError)

    /**
     * Delete an end user and all associated data
     */
    fun deleteEndUser(
        parentId: String,
        endUserId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) = userRepository.deleteEndUser(parentId, endUserId, onSuccess, onError)

    /**
     * Check if a user has parent permissions for an end user
     */
    fun checkParentPermission(
        endUserId: String,
        currentUserId: String,
        onSuccess: (Boolean) -> Unit,
        onFailure: (Exception) -> Unit
    ) = userRepository.checkParentPermission(endUserId, currentUserId, onSuccess, onFailure)

    // ====== ROUTINE OPERATIONS ======

    /**
     * Check if daily routines exist for the given date, and create them from templates if not
     */
    fun createDailyRoutinesIfNeeded(
        endUserId: String,
        date: Calendar,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) = routineRepository.createDailyRoutinesIfNeeded(endUserId, date, onSuccess, onError)

    /**
     * Get all daily routines for a specific end user on a specific date
     */
    fun getDailyRoutines(
        endUserId: String,
        date: Calendar,
        onSuccess: (List<DailyRoutine>) -> Unit,
        onError: (Exception) -> Unit
    ) = routineRepository.getDailyRoutines(endUserId, date, onSuccess, onError)

    /**
     * Get all daily routines for all children of a parent on a specific date
     */
    fun getAllEndUserDailyRoutines(
        parentId: String,
        date: Calendar,
        onSuccess: (List<DailyRoutine>) -> Unit,
        onError: (Exception) -> Unit
    ) = routineRepository.getAllEndUserDailyRoutines(parentId, date, onSuccess, onError)

    /**
     * Delete a routine and all associated data
     */
    fun deleteRoutine(
        endUserId: String,
        routine: DailyRoutine,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) = routineRepository.deleteRoutine(endUserId, routine, onSuccess, onError)

    // ====== TEMPLATE OPERATIONS ======

    /**
     * Get a routine template, checking permissions
     */
    fun getRoutineTemplate(
        routineId: String,
        endUserId: String,
        currentUserId: String,
        onSuccess: (RoutineTemplate) -> Unit,
        onFailure: (Exception) -> Unit
    ) = templateRepository.getRoutineTemplate(routineId, endUserId, currentUserId, onSuccess, onFailure)

    /**
     * Save or update a routine template and its corresponding daily routine
     */
    fun saveRoutine(
        routineId: String?,
        dailyRoutineId: String?,
        endUserId: String,
        name: String,
        icon: TaskJoyIcon,
        currentUserId: String,
        selectedDate: Calendar,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) = templateRepository.saveRoutine(
        routineId, dailyRoutineId, endUserId, name, icon,
        currentUserId, selectedDate, onSuccess, onFailure
    )

    // ====== STEP OPERATIONS ======

    /**
     * Get a routine with its steps
     */
    fun getRoutineWithSteps(
        endUserId: String,
        routineId: String,
        onSuccess: (DailyRoutine, List<Step>) -> Unit,
        onError: (Exception) -> Unit
    ) = stepRepository.getRoutineWithSteps(endUserId, routineId, onSuccess, onError)

    /**
     * Save the order of steps
     */
    fun saveStepOrder(
        endUserId: String,
        routineId: String,
        steps: List<Step>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) = stepRepository.saveStepOrder(endUserId, routineId, steps, onSuccess, onError)

    /**
     * Update the order of the remaining steps after deletion
     */
    fun updateRemainingStepsOrder(
        endUserId: String,
        routineId: String,
        steps: List<Step>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) = stepRepository.updateRemainingStepsOrder(endUserId, routineId, steps, onSuccess, onError)

    /**
     * Delete a step
     */
    fun deleteStep(
        endUserId: String,
        routineId: String,
        step: Step,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) = stepRepository.deleteStep(endUserId, routineId, step, onSuccess, onError)

    /**
     * Load an existing step for editing
     */
    fun getStep(
        endUserId: String,
        routineId: String,
        stepId: String,
        onSuccess: (Step) -> Unit,
        onError: (Exception) -> Unit
    ) = stepRepository.getStep(endUserId, routineId, stepId, onSuccess, onError)

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
    ) = stepRepository.createStep(endUserId, routineId, name, description, icon, customIconPath, onSuccess, onError)

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
    ) = stepRepository.updateStep(
        endUserId, routineId, stepId, templateStepId,
        name, description, icon, customIconPath,
        onSuccess, onError
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
    ) = stepRepository.markStepAsComplete(endUserId, routineId, stepId, onSuccess, onError)

    /**
     * Mark a step as incomplete
     */
    fun markStepAsIncomplete(
        endUserId: String,
        routineId: String,
        stepId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) = stepRepository.markStepAsIncomplete(endUserId, routineId, stepId, onSuccess, onError)

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
    ) = stepRepository.saveStepNotes(endUserId, routineId, stepId, notes, onSuccess, onError)

    /**
     * Mark all steps in a routine as complete
     */
    fun completeAllSteps(
        endUserId: String,
        routineId: String,
        onSuccess: (Timestamp) -> Unit,
        onError: (Exception) -> Unit
    ) = stepRepository.completeAllSteps(endUserId, routineId, onSuccess, onError)
}