package com.example.taskjoy.model

interface RoutineClickListener {
    fun onRoutineClick(routine: Routine)
    fun onEditClick(routine: Routine)
}

interface StepClickListener {
    fun onStepClick(step: Step)
    fun onEditClick(step: Step)
}