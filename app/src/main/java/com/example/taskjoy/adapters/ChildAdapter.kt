package com.example.taskjoy.adapters

import android.app.ProgressDialog.show
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taskjoy.R
import com.example.taskjoy.databinding.StepItemBinding
import com.example.taskjoy.databinding.ThreeLineRowLayoutBinding
import com.example.taskjoy.model.EndUser
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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

        // Set click listener on the content area instead of the whole root
        holder.binding.contentArea.setOnClickListener {
            listener.onChildClick(child.id)
        }

        holder.binding.btnEdit.setOnClickListener {
            listener.onEditClick(child.id)
        }
        holder.binding.btnDelete.setOnClickListener {
            showDeleteConfirmationDialog(holder.itemView.context, child)
        }
    }


    private fun showDeleteConfirmationDialog(context: Context, child: EndUser) {
        MaterialAlertDialogBuilder(context, R.style.DeleteConfirmationDialog)
            .setTitle("Delete ${child.name}?")
            .setMessage("Are you sure you want to delete this child profile? This action cannot be undone.")
            .setIcon(R.drawable.ic_warning)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                listener.onDeleteClick(child.id)
            }
            .create()
            .apply {
                window?.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
                show()
            }
    }
}