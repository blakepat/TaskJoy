package com.example.taskjoy.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.taskjoy.R
import com.example.taskjoy.databinding.StepItemBinding
import com.example.taskjoy.model.Step
import com.example.taskjoy.model.TaskJoyIcon
import java.io.File

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
            stepNotes.text = currItem.description

            // Safely handle icon loading
            try {
                if (currItem.image == TaskJoyIcon.CUSTOM.name && currItem.customIconPath != null) {
                    // Load custom icon using Glide
                    Glide.with(root.context)
                        .load(File(currItem.customIconPath))
                        .centerCrop()
                        .error(R.drawable.ic_brush_teeth)  // Fallback icon if load fails
                        .into(stepIcon)
                } else {
                    try {
                        val icon = TaskJoyIcon.valueOf(currItem.image.uppercase())
                        stepIcon.setImageResource(icon.drawableResId)
                    } catch (e: IllegalArgumentException) {
                        // If the image string doesn't match any enum value, set default
                        stepIcon.setImageResource(R.drawable.ic_brush_teeth)
                    }
                }
            } catch (e: Exception) {
                // Set default icon if there's any other error
                stepIcon.setImageResource(R.drawable.ic_brush_teeth)
            }

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

