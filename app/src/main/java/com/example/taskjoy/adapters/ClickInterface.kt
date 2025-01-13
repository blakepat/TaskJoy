package com.example.taskjoy.adapters

import com.example.taskjoy.model.DailyRoutine
import com.example.taskjoy.model.Routine
import com.example.taskjoy.model.Step

interface RoutineClickListener {
    fun onRoutineClick(routine: DailyRoutine)
    fun onEditClick(routine: DailyRoutine)
    fun onDeleteClick(routine: DailyRoutine)
}

interface StepClickListener {
    fun onStepClick(step: Step)
    fun onEditClick(step: Step)
    fun onDeleteClick(step: Step)
    fun onStepOrderChanged(steps: List<Step>)
}

interface ChildClickListener {
    fun onChildClick(id: String)
    fun onEditClick(id: String)
    fun onDeleteClick(id: String)
}