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
    fun createDailyRoutinesIfNeeded(
        endUserId: String,
        date: Calendar,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    )

    /**
     * Get all daily routines for a specific end user on a specific date
     */
    fun getDailyRoutines(
        endUserId: String,
        date: Calendar,
        onSuccess: (List<DailyRoutine>) -> Unit,
        onError: (Exception) -> Unit
    )

    /**
     * Get all daily routines for all children of a parent on a specific date
     */
    fun getAllEndUserDailyRoutines(
        parentId: String,
        date: Calendar,
        onSuccess: (List<DailyRoutine>) -> Unit,
        onError: (Exception) -> Unit
    )

    /**
     * Delete a routine and all associated data
     */
    fun deleteRoutine(
        endUserId: String,
        routine: DailyRoutine,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    )
}