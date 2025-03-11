package com.example.taskjoy.repository

import com.example.taskjoy.model.DailyRoutine
import com.example.taskjoy.model.Step
import com.example.taskjoy.model.TaskJoyIcon
import com.google.firebase.Timestamp


interface StepRepository {

    suspend fun getRoutineWithSteps(
        endUserId: String,
        routineId: String
    ): Result<Pair<DailyRoutine, List<Step>>>

    suspend fun saveStepOrder(
        endUserId: String,
        routineId: String,
        steps: List<Step>
    ): Result<Unit>

    suspend fun updateRemainingStepsOrder(
        endUserId: String,
        routineId: String,
        steps: List<Step>
    ): Result<Unit>

    suspend fun deleteStep(
        endUserId: String,
        routineId: String,
        step: Step
    ): Result<Unit>

    suspend fun getStep(
        endUserId: String,
        routineId: String,
        stepId: String
    ): Result<Step>


    suspend fun createStep(
        endUserId: String,
        routineId: String,
        name: String,
        description: String,
        icon: TaskJoyIcon,
        customIconPath: String?
    ): Result<Unit>


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

    suspend fun markStepAsComplete(
        endUserId: String,
        routineId: String,
        stepId: String
    ): Result<Timestamp>

    suspend fun markStepAsIncomplete(
        endUserId: String,
        routineId: String,
        stepId: String
    ): Result<Unit>


    suspend fun saveStepNotes(
        endUserId: String,
        routineId: String,
        stepId: String,
        notes: String
    ): Result<Unit>


    suspend fun completeAllSteps(
        endUserId: String,
        routineId: String
    ): Result<Timestamp>
}