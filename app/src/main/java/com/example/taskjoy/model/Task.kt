package com.example.taskjoy.model

enum class TaskIcon {
    MORNING,
    SCHOOL,
    LUNCH,
    BEDTIME,
    CUSTOM
}

data class Task(
    val name: String,
    val stepQuantity: Int,
    val icon: TaskIcon
)
