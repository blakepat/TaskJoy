package com.example.taskjoy.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.taskjoy.databinding.StepItemBinding
import com.example.taskjoy.model.Step
import com.example.taskjoy.model.TaskJoyIcon

class StepAdapter(private var steps: List<Step>, private val listener: StepClickListener) : RecyclerView.Adapter<StepAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: StepItemBinding) : RecyclerView.ViewHolder (binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = StepItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }


    override fun getItemCount(): Int {
        return steps.size
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currItem: Step = steps[position]

        holder.binding.stepName.text = currItem.name
        holder.binding.stepIcon.setImageResource(TaskJoyIcon.fromString(currItem.image).getDrawableResource())

        holder.binding.root.setOnClickListener {
            listener.onStepClick(currItem)
        }
    }
}