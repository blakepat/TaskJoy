package com.example.taskjoy.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.taskjoy.R

// UserManagementAdapter.kt
class UserManagementAdapter(
    private var users: List<UserItem>,
    private val currentUserId: String,
    private val isParent: Boolean,
    private val onRemoveClick: (String) -> Unit
) : RecyclerView.Adapter<UserManagementAdapter.UserViewHolder>() {

    data class UserItem(
        val id: String,
        val email: String,
        val isParent: Boolean
    )

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val emailText: TextView = view.findViewById(R.id.tvUserEmail)
        val roleText: TextView = view.findViewById(R.id.tvUserRole)
        val removeButton: ImageButton = view.findViewById(R.id.btnRemoveUser)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.emailText.text = user.email
        holder.roleText.text = if (user.isParent) "Parent" else "Chaperone"

        // Show remove button only if current user is a parent and not for themselves
        holder.removeButton.isVisible = isParent && user.id != currentUserId
        holder.removeButton.setOnClickListener {
            onRemoveClick(user.id)
        }
    }

    override fun getItemCount() = users.size

    fun updateUsers(newUsers: List<UserItem>) {
        users = newUsers
        notifyDataSetChanged()
    }
}