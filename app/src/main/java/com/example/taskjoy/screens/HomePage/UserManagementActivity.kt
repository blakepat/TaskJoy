package com.example.taskjoy.screens.HomePage

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.taskjoy.adapters.UserManagementAdapter
import com.example.taskjoy.databinding.ActivityUserManagementBinding
import com.example.taskjoy.viewmodels.UserManagementViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class UserManagementActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserManagementBinding
    private lateinit var adapter: UserManagementAdapter
    private lateinit var auth: FirebaseAuth
    private var endUserId: String = ""

    // Initialize the ViewModel using the by viewModels() delegate
    private val viewModel: UserManagementViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        endUserId = intent.getStringExtra("endUser") ?: run {
            finish()
            return
        }

        setupObservers()

        // Start by checking user role
        auth.currentUser?.uid?.let { currentUserId ->
            viewModel.checkUserRole(endUserId, currentUserId)
        }
    }

    private fun setupObservers() {
        // Observe isParent to setup RecyclerView
        viewModel.isParent.observe(this) { isParent ->
            setupRecyclerView(isParent)

            // Load users after role is determined
            viewModel.loadUsers(endUserId)
        }

        // Observe users to update adapter
        viewModel.users.observe(this) { users ->
            adapter.updateUsers(users)
        }


        // Observe error messages
        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        // Observe removal success
        viewModel.removeSuccess.observe(this) { success ->
            if (success) {
                Snackbar.make(binding.root, "User removed successfully", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView(isParent: Boolean) {
        adapter = UserManagementAdapter(
            users = emptyList(),
            currentUserId = auth.currentUser?.uid ?: "",
            isParent = isParent
        ) { userId ->
            showRemoveUserDialog(userId)
        }
        binding.rvUsers.adapter = adapter
    }

    private fun showRemoveUserDialog(userId: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Remove User")
            .setMessage("Are you sure you want to remove this user's access?")
            .setPositiveButton("Remove") { _, _ ->
                viewModel.removeUser(endUserId, userId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}