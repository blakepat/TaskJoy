package com.example.taskjoy.repository

import com.example.taskjoy.adapters.UserManagementAdapter
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

    // Basic user operations
    fun getChildren(
        parentId: String,
        onSuccess: (List<EndUser>) -> Unit,
        onError: (Exception) -> Unit
    ) = userRepository.getChildren(parentId, onSuccess, onError)

    fun deleteEndUser(
        parentId: String,
        endUserId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) = userRepository.deleteEndUser(parentId, endUserId, onSuccess, onError)

    fun checkParentPermission(
        endUserId: String,
        currentUserId: String,
        onSuccess: (Boolean) -> Unit,
        onFailure: (Exception) -> Unit
    ) = userRepository.checkParentPermission(endUserId, currentUserId, onSuccess, onFailure)

    // User management operations
    fun checkUserRole(
        endUserId: String,
        currentUserId: String,
        onSuccess: (Boolean) -> Unit,
        onError: (Exception) -> Unit
    ) = userRepository.checkUserRole(endUserId, currentUserId, onSuccess, onError)

    fun loadUserAccess(
        endUserId: String,
        onSuccess: (List<UserManagementAdapter.UserItem>) -> Unit,
        onError: (Exception) -> Unit
    ) = userRepository.loadUserAccess(endUserId, onSuccess, onError)

    fun removeUserAccess(
        endUserId: String,
        userId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) = userRepository.removeUserAccess(endUserId, userId, onSuccess, onError)

    // ====== ROUTINE OPERATIONS ======

    fun createDailyRoutinesIfNeeded(
        endUserId: String,
        date: Calendar,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) = routineRepository.createDailyRoutinesIfNeeded(endUserId, date, onSuccess, onError)

    fun getDailyRoutines(
        endUserId: String,
        date: Calendar,
        onSuccess: (List<DailyRoutine>) -> Unit,
        onError: (Exception) -> Unit
    ) = routineRepository.getDailyRoutines(endUserId, date, onSuccess, onError)

    fun getAllEndUserDailyRoutines(
        parentId: String,
        date: Calendar,
        onSuccess: (List<DailyRoutine>) -> Unit,
        onError: (Exception) -> Unit
    ) = routineRepository.getAllEndUserDailyRoutines(parentId, date, onSuccess, onError)

    fun deleteRoutine(
        endUserId: String,
        routine: DailyRoutine,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) = routineRepository.deleteRoutine(endUserId, routine, onSuccess, onError)

    // ====== TEMPLATE OPERATIONS ======

    fun getRoutineTemplate(
        routineId: String,
        endUserId: String,
        currentUserId: String,
        onSuccess: (RoutineTemplate) -> Unit,
        onFailure: (Exception) -> Unit
    ) = templateRepository.getRoutineTemplate(routineId, endUserId, currentUserId, onSuccess, onFailure)

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

    fun getRoutineWithSteps(
        endUserId: String,
        routineId: String,
        onSuccess: (DailyRoutine, List<Step>) -> Unit,
        onError: (Exception) -> Unit
    ) = stepRepository.getRoutineWithSteps(endUserId, routineId, onSuccess, onError)

    fun saveStepOrder(
        endUserId: String,
        routineId: String,
        steps: List<Step>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) = stepRepository.saveStepOrder(endUserId, routineId, steps, onSuccess, onError)

    fun updateRemainingStepsOrder(
        endUserId: String,
        routineId: String,
        steps: List<Step>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) = stepRepository.updateRemainingStepsOrder(endUserId, routineId, steps, onSuccess, onError)

    fun deleteStep(
        endUserId: String,
        routineId: String,
        step: Step,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) = stepRepository.deleteStep(endUserId, routineId, step, onSuccess, onError)

    fun getStep(
        endUserId: String,
        routineId: String,
        stepId: String,
        onSuccess: (Step) -> Unit,
        onError: (Exception) -> Unit
    ) = stepRepository.getStep(endUserId, routineId, stepId, onSuccess, onError)

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

    fun markStepAsComplete(
        endUserId: String,
        routineId: String,
        stepId: String,
        onSuccess: (Timestamp) -> Unit,
        onError: (Exception) -> Unit
    ) = stepRepository.markStepAsComplete(endUserId, routineId, stepId, onSuccess, onError)

    fun markStepAsIncomplete(
        endUserId: String,
        routineId: String,
        stepId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) = stepRepository.markStepAsIncomplete(endUserId, routineId, stepId, onSuccess, onError)

    fun saveStepNotes(
        endUserId: String,
        routineId: String,
        stepId: String,
        notes: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) = stepRepository.saveStepNotes(endUserId, routineId, stepId, notes, onSuccess, onError)

    fun completeAllSteps(
        endUserId: String,
        routineId: String,
        onSuccess: (Timestamp) -> Unit,
        onError: (Exception) -> Unit
    ) = stepRepository.completeAllSteps(endUserId, routineId, onSuccess, onError)
}