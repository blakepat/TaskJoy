package com.example.taskjoy.model

import com.example.taskjoy.R

enum class TaskJoyIcon(val drawableResId: Int) {
    MORNING(R.drawable.ic_morning),
    SCHOOL(R.drawable.ic_school),
    LUNCH(R.drawable.ic_lunch),
    BEDTIME(R.drawable.ic_bedtime),
    CUSTOM(R.drawable.ic_custom);

    fun getDrawableResource(): Int {
        return drawableResId
    }

    companion object {
        fun fromString(value: String?): TaskJoyIcon {
            if (value == null) return CUSTOM

            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                CUSTOM // Return CUSTOM if string doesn't match any enum
            }
        }
    }
}


