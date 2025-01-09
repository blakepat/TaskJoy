package com.example.taskjoy.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.taskjoy.databinding.StepItemBinding
import com.example.taskjoy.databinding.ThreeLineRowLayoutBinding
import com.example.taskjoy.model.EndUser

class ChildAdapter(
    private val children: List<EndUser>,
    private val listener: ChildClickListener,
    private val currentUserId: String
) : RecyclerView.Adapter<ChildAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ThreeLineRowLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ThreeLineRowLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return children.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val child: EndUser = children[position]

        holder.binding.tvRow1.text = child.name
        holder.binding.tvRow2.text = "Age: ${child.age}"

        // Check if current user is a parent
        val isParent = child.parents.contains(currentUserId)

        // Show/hide edit and delete buttons based on user role
        holder.binding.btnEdit.visibility = if (isParent) View.VISIBLE else View.GONE
        holder.binding.btnDelete.visibility = if (isParent) View.VISIBLE else View.GONE

        //Go to routine list for single child
        holder.binding.root.setOnClickListener {
            listener.onChildClick(child.id)
        }
        holder.binding.btnEdit.setOnClickListener {
            listener.onEditClick(child.id)
        }
        holder.binding.btnDelete.setOnClickListener {
            listener.onDeleteClick(child.id)
        }
    }
}