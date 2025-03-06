package com.example.taskjoy.repository

import com.example.taskjoy.model.RoutineTemplate
import com.example.taskjoy.model.TaskJoyIcon
import java.util.Calendar

/**
 * Repository interface for routine template operations
 */
interface TemplateRepository {
    /**
     * Get a routine template, checking permissions
     */
    fun getRoutineTemplate(
        routineId: String,
        endUserId: String,
        currentUserId: String,
        onSuccess: (RoutineTemplate) -> Unit,
        onFailure: (Exception) -> Unit
    )

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
    )
}