// StepAdapter.kt
package com.example.taskjoy.adapters

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.taskjoy.R
import com.example.taskjoy.databinding.StepItemBinding
import com.example.taskjoy.model.Step
import com.example.taskjoy.model.TaskJoyIcon
import java.io.File
import java.util.Collections


class StepAdapter(
    private var steps: MutableList<Step>, // Changed to MutableList
    private val listener: StepClickListener
) : RecyclerView.Adapter<StepAdapter.ViewHolder>() {

    private var isEditMode = false
    private var touchHelper: ItemTouchHelper? = null

    fun setItemTouchHelper(itemTouchHelper: ItemTouchHelper) {
        this.touchHelper = itemTouchHelper
    }

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

            // Handle drag icon visibility and touch events
            dragHandle.visibility = if (isEditMode) View.VISIBLE else View.GONE
            dragHandle.setOnTouchListener { view, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    touchHelper?.startDrag(holder)
                }
                false
            }

            // Existing icon loading logic
            try {
                if (currItem.image == TaskJoyIcon.CUSTOM.name && currItem.customIconPath != null) {
                    Glide.with(root.context)
                        .load(File(currItem.customIconPath))
                        .centerCrop()
                        .error(R.drawable.ic_brush_teeth)
                        .into(stepIcon)
                } else {
                    try {
                        val icon = TaskJoyIcon.valueOf(currItem.image.uppercase())
                        stepIcon.setImageResource(icon.drawableResId)
                    } catch (e: IllegalArgumentException) {
                        stepIcon.setImageResource(R.drawable.ic_brush_teeth)
                    }
                }
            } catch (e: Exception) {
                stepIcon.setImageResource(R.drawable.ic_brush_teeth)
            }

            editIconContainer.visibility = if (isEditMode) View.VISIBLE else View.GONE
            completionIndicatorContainer.visibility = if (currItem.completed && !isEditMode) {
                View.VISIBLE
            } else {
                View.GONE
            }
            deleteIconContainer.visibility = if (isEditMode) View.VISIBLE else View.GONE

            deleteIconContainer.setOnClickListener {
                listener.onDeleteClick(currItem)
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

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(steps, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(steps, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
        listener.onStepOrderChanged(steps)
    }
}
