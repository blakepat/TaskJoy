package com.example.taskjoy.repository

import com.example.taskjoy.model.RoutineTemplate
import com.example.taskjoy.model.TaskJoyIcon
import java.util.Calendar


interface TemplateRepository {

    suspend fun getRoutineTemplate(
        routineId: String,
        endUserId: String,
        currentUserId: String
    ): Result<RoutineTemplate>

    suspend fun saveRoutine(
        routineId: String?,
        dailyRoutineId: String?,
        endUserId: String,
        name: String,
        icon: TaskJoyIcon,
        currentUserId: String,
        selectedDate: Calendar
    ): Result<Unit>
}