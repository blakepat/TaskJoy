package com.example.taskjoy.model

data class EmotionCard(
    val id: Int,
    val emotion: Emotion,
    var isFlipped: Boolean = false
)