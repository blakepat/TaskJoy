package com.example.taskjoy.repository

import com.example.taskjoy.model.DailyRoutine
import java.util.Calendar

/**
 * Repository interface for daily routine operations
 */
interface RoutineRepository {
    /**
     * Check if daily routines exist for the given date, and create them from templates if not
     */
    suspend fun createDailyRoutinesIfNeeded(
        endUserId: String,
        date: Calendar
    ): Result<Unit>

    /**
     * Get all daily routines for a specific end user on a specific date
     */
    suspend fun getDailyRoutines(
        endUserId: String,
        date: Calendar
    ): Result<List<DailyRoutine>>

    /**
     * Get all daily routines for all children of a parent on a specific date
     */
    suspend fun getAllEndUserDailyRoutines(
        parentId: String,
        date: Calendar
    ): Result<List<DailyRoutine>>

    /**
     * Delete a routine and all associated data
     */
    suspend fun deleteRoutine(
        endUserId: String,
        routine: DailyRoutine
    ): Result<Unit>
}