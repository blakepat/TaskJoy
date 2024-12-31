package com.example.taskjoy.model

interface TaskClickListener {
    fun onTaskClick(task: Task)
    fun onEditClick(task: Task)
}
