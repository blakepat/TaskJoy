package com.example.taskjoy.model

import androidx.annotation.DrawableRes

data class Emotion(
    val id: Int,
    @DrawableRes val iconRes: Int,
    val name: String
)