package com.example.taskjoy.repository

import com.example.taskjoy.model.DailyRoutine
import java.util.Calendar

interface RoutineRepository {

    suspend fun createDailyRoutinesIfNeeded(
        endUserId: String,
        date: Calendar
    ): Result<Unit>

    suspend fun getDailyRoutines(
        endUserId: String,
        date: Calendar
    ): Result<List<DailyRoutine>>

    suspend fun getAllEndUserDailyRoutines(
        parentId: String,
        date: Calendar
    ): Result<List<DailyRoutine>>

    suspend fun deleteRoutine(
        endUserId: String,
        routine: DailyRoutine
    ): Result<Unit>
}