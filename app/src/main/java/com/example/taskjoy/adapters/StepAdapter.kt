package com.example.taskjoy.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.taskjoy.R
import com.example.taskjoy.databinding.StepItemBinding
import com.example.taskjoy.model.Step
import com.example.taskjoy.model.TaskJoyIcon

class StepAdapter(
    private var steps: List<Step>,
    private val listener: StepClickListener
) : RecyclerView.Adapter<StepAdapter.ViewHolder>() {

    private var isEditMode = false

    inner class ViewHolder(val binding: StepItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = StepItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = steps.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currItem: Step = steps[position]

        with(holder.binding) {
            stepName.text = currItem.name
            stepNotes.text = currItem.notes
            stepIcon.setImageResource(TaskJoyIcon.fromString(currItem.image).getDrawableResource())

            // Handle edit icon visibility
            editIconContainer.visibility = if (isEditMode) View.VISIBLE else View.GONE

            // Handle completion indicator
            completionIndicatorContainer.visibility = if (currItem.completed && !isEditMode) {
                View.VISIBLE
            } else {
                View.GONE
            }

            root.setOnClickListener {
                listener.onStepClick(currItem)
            }

            editIconContainer.setOnClickListener {
                listener.onEditClick(currItem)
            }
        }
    }

    fun setEditMode(enabled: Boolean) {
        isEditMode = enabled
        notifyDataSetChanged()
    }
}