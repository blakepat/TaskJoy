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
    suspend fun getChildren(parentId: String): Result<List<EndUser>> =
        userRepository.getChildren(parentId)

    suspend fun deleteEndUser(parentId: String, endUserId: String): Result<Unit> =
        userRepository.deleteEndUser(parentId, endUserId)

    suspend fun checkParentPermission(endUserId: String, currentUserId: String): Result<Boolean> =
        userRepository.checkParentPermission(endUserId, currentUserId)

    // User management operations
    suspend fun checkUserRole(endUserId: String, currentUserId: String): Result<Boolean> =
        userRepository.checkUserRole(endUserId, currentUserId)

    suspend fun loadUserAccess(endUserId: String): Result<List<UserManagementAdapter.UserItem>> =
        userRepository.loadUserAccess(endUserId)

    suspend fun removeUserAccess(endUserId: String, userId: String): Result<Unit> =
        userRepository.removeUserAccess(endUserId, userId)

    // ====== ROUTINE OPERATIONS ======

    suspend fun createDailyRoutinesIfNeeded(endUserId: String, date: Calendar): Result<Unit> =
        routineRepository.createDailyRoutinesIfNeeded(endUserId, date)

    suspend fun getDailyRoutines(endUserId: String, date: Calendar): Result<List<DailyRoutine>> =
        routineRepository.getDailyRoutines(endUserId, date)

    suspend fun getAllEndUserDailyRoutines(parentId: String, date: Calendar): Result<List<DailyRoutine>> =
        routineRepository.getAllEndUserDailyRoutines(parentId, date)

    suspend fun deleteRoutine(endUserId: String, routine: DailyRoutine): Result<Unit> =
        routineRepository.deleteRoutine(endUserId, routine)

    // ====== TEMPLATE OPERATIONS ======

    suspend fun getRoutineTemplate(
        routineId: String,
        endUserId: String,
        currentUserId: String
    ): Result<RoutineTemplate> =
        templateRepository.getRoutineTemplate(routineId, endUserId, currentUserId)

    suspend fun saveRoutine(
        routineId: String?,
        dailyRoutineId: String?,
        endUserId: String,
        name: String,
        icon: TaskJoyIcon,
        currentUserId: String,
        selectedDate: Calendar
    ): Result<Unit> =
        templateRepository.saveRoutine(
            routineId, dailyRoutineId, endUserId, name, icon,
            currentUserId, selectedDate
        )

    // ====== STEP OPERATIONS ======

    suspend fun getRoutineWithSteps(
        endUserId: String,
        routineId: String
    ): Result<Pair<DailyRoutine, List<Step>>> =
        stepRepository.getRoutineWithSteps(endUserId, routineId)

    suspend fun saveStepOrder(
        endUserId: String,
        routineId: String,
        steps: List<Step>
    ): Result<Unit> =
        stepRepository.saveStepOrder(endUserId, routineId, steps)

    suspend fun updateRemainingStepsOrder(
        endUserId: String,
        routineId: String,
        steps: List<Step>
    ): Result<Unit> =
        stepRepository.updateRemainingStepsOrder(endUserId, routineId, steps)

    suspend fun deleteStep(
        endUserId: String,
        routineId: String,
        step: Step
    ): Result<Unit> =
        stepRepository.deleteStep(endUserId, routineId, step)

    suspend fun getStep(
        endUserId: String,
        routineId: String,
        stepId: String
    ): Result<Step> =
        stepRepository.getStep(endUserId, routineId, stepId)

    suspend fun createStep(
        endUserId: String,
        routineId: String,
        name: String,
        description: String,
        icon: TaskJoyIcon,
        customIconPath: String?
    ): Result<Unit> =
        stepRepository.createStep(endUserId, routineId, name, description, icon, customIconPath)

    suspend fun updateStep(
        endUserId: String,
        routineId: String,
        stepId: String,
        templateStepId: String?,
        name: String,
        description: String,
        icon: TaskJoyIcon,
        customIconPath: String?
    ): Result<Unit> =
        stepRepository.updateStep(
            endUserId, routineId, stepId, templateStepId,
            name, description, icon, customIconPath
        )

    suspend fun markStepAsComplete(
        endUserId: String,
        routineId: String,
        stepId: String
    ): Result<Timestamp> =
        stepRepository.markStepAsComplete(endUserId, routineId, stepId)

    suspend fun markStepAsIncomplete(
        endUserId: String,
        routineId: String,
        stepId: String
    ): Result<Unit> =
        stepRepository.markStepAsIncomplete(endUserId, routineId, stepId)

    suspend fun saveStepNotes(
        endUserId: String,
        routineId: String,
        stepId: String,
        notes: String
    ): Result<Unit> =
        stepRepository.saveStepNotes(endUserId, routineId, stepId, notes)

    suspend fun completeAllSteps(
        endUserId: String,
        routineId: String
    ): Result<Timestamp> =
        stepRepository.completeAllSteps(endUserId, routineId)
}