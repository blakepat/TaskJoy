package com.example.taskjoy.model

data class CustomIcon(
    val id: Long,
    val filepath: String,
    val timestamp: Long = System.currentTimeMillis()
)