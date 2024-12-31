package com.example.taskjoy.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.taskjoy.R
import com.example.taskjoy.TaskDetailsActivity
import com.example.taskjoy.model.Task
import com.example.taskjoy.model.TaskClickListener
import com.example.taskjoy.model.TaskIcon

class TaskAdapter(

    private val tasks: List<Task>,
    private val context: Context,
    private val listener: TaskClickListener
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.task_icon)
        val name: TextView = itemView.findViewById(R.id.task_name)
        val stepQuantity: TextView = itemView.findViewById(R.id.task_step_quantity)
        val editIcon: ImageView = itemView.findViewById(R.id.task_edit_icon)

        fun bind(task: Task) {
            name.text = task.name
            stepQuantity.text = "${task.stepQuantity} steps"
            icon.setImageResource(getIconResource(task.icon))

            // Set click listeners
            itemView.setOnClickListener {
                listener.onTaskClick(task)
                navigateToTaskDetails(task)
            }
            editIcon.setOnClickListener { listener.onEditClick(task) }
        }

        private fun navigateToTaskDetails(task: Task) {
            val intent = Intent(context, TaskDetailsActivity::class.java).apply {
                putExtra("taskName", task.name)
                putExtra("stepQuantity", task.stepQuantity)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.task_item, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int = tasks.size

    private fun getIconResource(icon: TaskIcon): Int {
        return when (icon) {
            TaskIcon.MORNING -> R.drawable.ic_morning
            TaskIcon.SCHOOL -> R.drawable.ic_school
            TaskIcon.LUNCH -> R.drawable.ic_lunch
            TaskIcon.BEDTIME -> R.drawable.ic_bedtime
            TaskIcon.CUSTOM -> R.drawable.ic_custom
        }
    }
}
