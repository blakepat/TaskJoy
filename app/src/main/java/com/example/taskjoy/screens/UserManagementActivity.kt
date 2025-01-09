package com.example.taskjoy.screens

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.taskjoy.adapters.UserManagementAdapter
import com.example.taskjoy.databinding.ActivityUserManagementBinding
import com.example.taskjoy.model.EndUser
import com.example.taskjoy.model.Parent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

// UserManagementActivity.kt
class UserManagementActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserManagementBinding
    private lateinit var adapter: UserManagementAdapter
    private lateinit var auth: FirebaseAuth
    private var db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        val endUserId = intent.getStringExtra("endUser") ?: return finish()

        // First check if current user is a parent
        checkUserRole(endUserId) { isParent ->
            setupRecyclerView(endUserId, isParent)
            loadUsers(endUserId)
        }
    }

    private fun checkUserRole(endUserId: String, callback: (Boolean) -> Unit) {
        db.collection("endUser")
            .document(endUserId)
            .get()
            .addOnSuccessListener { document ->
                val endUser = document.toObject(EndUser::class.java)
                val isParent = endUser?.parents?.contains(auth.currentUser?.uid) == true
                callback(isParent)
            }
    }

    private fun setupRecyclerView(endUserId: String, isParent: Boolean) {
        adapter = UserManagementAdapter(
            users = emptyList(),
            currentUserId = auth.currentUser?.uid ?: "",
            isParent = isParent
        ) { userId ->
            showRemoveUserDialog(endUserId, userId)
        }
        binding.rvUsers.adapter = adapter
    }

    private fun loadUsers(endUserId: String) {
        db.collection("endUser")
            .document(endUserId)
            .get()
            .addOnSuccessListener { document ->
                val endUser = document.toObject(EndUser::class.java)
                val userIds = (endUser?.parents ?: emptyList()) + (endUser?.chaperones ?: emptyList())

                // Get user details for all IDs
                db.collection("parents")
                    .whereIn(FieldPath.documentId(), userIds)
                    .get()
                    .addOnSuccessListener { documents ->
                        val users = documents.mapNotNull { doc ->
                            val parent = doc.toObject(Parent::class.java)
                            val isParent = endUser?.parents?.contains(doc.id) == true
                            UserManagementAdapter.UserItem(
                                id = doc.id,
                                email = parent.email ?: "",
                                isParent = isParent
                            )
                        }
                        adapter.updateUsers(users)
                    }
            }
    }

    private fun showRemoveUserDialog(endUserId: String, userId: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Remove User")
            .setMessage("Are you sure you want to remove this user's access?")
            .setPositiveButton("Remove") { _, _ ->
                removeUser(endUserId, userId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun removeUser(endUserId: String, userId: String) {
        // Create a batch write for atomic update
        val batch = db.batch()

        // Update endUser document
        val endUserRef = db.collection("endUser").document(endUserId)
        db.collection("endUser")
            .document(endUserId)
            .get()
            .addOnSuccessListener { endUserDoc ->
                val endUser = endUserDoc.toObject(EndUser::class.java)

                // Remove from appropriate list (parents or chaperones)
                if (endUser?.parents?.contains(userId) == true) {
                    val updatedParents = endUser.parents.toMutableList()
                    updatedParents.remove(userId)
                    batch.update(endUserRef, "parents", updatedParents)
                } else if (endUser?.chaperones?.contains(userId) == true) {
                    val updatedChaperones = endUser.chaperones.toMutableList()
                    updatedChaperones.remove(userId)
                    batch.update(endUserRef, "chaperones", updatedChaperones)
                }

                // Update parent document
                val parentRef = db.collection("parents").document(userId)
                db.collection("parents")
                    .document(userId)
                    .get()
                    .addOnSuccessListener { parentDoc ->
                        val parent = parentDoc.toObject(Parent::class.java)
                        val updatedChildren = parent?.children?.toMutableList() ?: mutableListOf()
                        updatedChildren.remove(endUserId)
                        batch.update(parentRef, "children", updatedChildren)

                        // Commit all updates atomically
                        batch.commit()
                            .addOnSuccessListener {
                                Snackbar.make(binding.root, "User removed successfully", Snackbar.LENGTH_SHORT).show()
                                loadUsers(endUserId)  // Reload the list
                            }
                            .addOnFailureListener { error ->
                                Snackbar.make(binding.root, "Failed to remove user: $error", Snackbar.LENGTH_SHORT).show()
                            }
                    }
            }
    }
}