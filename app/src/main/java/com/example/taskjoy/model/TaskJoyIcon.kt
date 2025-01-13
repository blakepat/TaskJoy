package com.example.taskjoy.model

import androidx.constraintlayout.motion.widget.Key.CUSTOM
import com.example.taskjoy.R

enum class TaskJoyIcon(val drawableResId: Int) {
    CUSTOM(-1),  // Special case for custom icons
    MORNING(R.drawable.ic_morning),
    SCHOOL(R.drawable.ic_school),
    BEDTIME(R.drawable.ic_bed_time),
    BACKPACK(R.drawable.ic_backpack),
    BOOK(R.drawable.ic_book),
    BRUSHTEETH(R.drawable.ic_brush_teeth),
    CAR(R.drawable.ic_car),
    DRAW(R.drawable.ic_draw),
    MAKEBED(R.drawable.ic_make_bed),
    SLEEP(R.drawable.ic_sleep),
    SPORTS(R.drawable.ic_sports),
    TSHIRT(R.drawable.ic_tshirt),
    WAKEUP(R.drawable.ic_wake_up),
    WASHHANDS(R.drawable.ic_wash_hands),
    BOYRUNNING(R.drawable.ic_boy_running),
    GIRLRUNNING(R.drawable.ic_girl_running),
    LAPTOP(R.drawable.ic_laptop);

    fun getDrawableResource(): Int {
        return drawableResId
    }

    companion object {
        fun fromString(value: String?): TaskJoyIcon {
            if (value == null) return MORNING
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                CUSTOM
            }
        }
    }
}


