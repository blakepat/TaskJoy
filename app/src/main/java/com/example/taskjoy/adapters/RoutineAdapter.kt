package com.example.taskjoy.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.taskjoy.R
import com.example.taskjoy.model.Routine
import com.example.taskjoy.model.TaskJoyIcon

class RoutineAdapter(
    private val routines: List<Routine>,
    private val context: Context,
    private val listener: RoutineClickListener
) : RecyclerView.Adapter<RoutineAdapter.TaskViewHolder>() {

    private var isEditMode = false

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.routine_icon)
        private val name: TextView = itemView.findViewById(R.id.routine_name)
        private val stepQuantity: TextView = itemView.findViewById(R.id.routine_step_quantity)
        private val editIcon: ImageView = itemView.findViewById(R.id.routine_edit_icon)
        private val completionIndicator: ImageView = itemView.findViewById(R.id.routine_completion_indicator)

        fun bind(routine: Routine) {
            name.text = routine.name
            stepQuantity.text = "${routine.steps.size} steps"
            icon.setImageResource(TaskJoyIcon.fromString(routine.image).getDrawableResource())

            // Handle edit icon visibility based on edit mode
            editIcon.visibility = if (isEditMode) View.VISIBLE else View.GONE

            // Handle completion indicator
            completionIndicator.visibility = if (routine.completed && !isEditMode) {
                View.VISIBLE
            } else {
                View.GONE
            }

            // Set click listeners
            itemView.setOnClickListener {
                listener.onRoutineClick(routine)
            }
            editIcon.setOnClickListener {
                listener.onEditClick(routine)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.routine_item, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(routines[position])
    }

    override fun getItemCount(): Int = routines.size

    fun setEditMode(enabled: Boolean) {
        isEditMode = enabled
        notifyDataSetChanged()
    }
}