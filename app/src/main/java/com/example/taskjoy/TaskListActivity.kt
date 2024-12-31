package com.example.taskjoy

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taskjoy.adapters.TaskAdapter
import com.example.taskjoy.model.Task
import com.example.taskjoy.model.TaskClickListener
import com.example.taskjoy.model.TaskIcon

class TaskListActivity : AppCompatActivity(), TaskClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private val taskList = mutableListOf<Task>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task)

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerViewTasks)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Prepare the task list
        taskList.addAll(
            listOf(
                Task("Morning Routine", 5, TaskIcon.MORNING),
                Task("School Preparation", 3, TaskIcon.SCHOOL),
                Task("Lunch Prep", 4, TaskIcon.LUNCH),
                Task("Bedtime Routine", 2, TaskIcon.BEDTIME),
                Task("Custom Task", 1, TaskIcon.CUSTOM)
            )
        )

        // Set up the adapter
        taskAdapter = TaskAdapter(taskList, this, this) // Pass the task list, context, and listener
        recyclerView.adapter = taskAdapter
    }

    // Click listener methods
    override fun onTaskClick(task: Task) {
        val intent = Intent(this, TaskDetailsActivity::class.java).apply {
            putExtra("taskName", task.name)
            putExtra("stepQuantity", task.stepQuantity)
        }
        startActivity(intent)
    }

    override fun onEditClick(task: Task) {
        Toast.makeText(this, "Edit Task: ${task.name}", Toast.LENGTH_SHORT).show()
    }
}
