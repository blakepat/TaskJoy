package com.example.taskjoy.model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.taskjoy.R

class StepAdapter(private val steps: List<String>) :
    RecyclerView.Adapter<StepAdapter.StepViewHolder>() {

    inner class StepViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textStep)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.step_item, parent, false)
        return StepViewHolder(view)
    }

    override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
        holder.textView.text = steps[position]
    }

    override fun getItemCount(): Int = steps.size
}
