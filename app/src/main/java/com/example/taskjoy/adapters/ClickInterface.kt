package com.example.taskjoy.adapters

import com.example.taskjoy.model.Routine
import com.example.taskjoy.model.Step

interface RoutineClickListener {
    fun onRoutineClick(routine: Routine)
    fun onEditClick(routine: Routine)
}

interface StepClickListener {
    fun onStepClick(step: Step)
    fun onEditClick(step: Step)
}

interface ChildClickListener {
    fun onChildClick(id: String)
    fun onDeleteClick(id: String)
}