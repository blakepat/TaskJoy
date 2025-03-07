package com.example.taskjoy.repository

import android.util.Log
import com.example.taskjoy.adapters.UserManagementAdapter
import com.example.taskjoy.model.EndUser
import com.example.taskjoy.model.Parent
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * Firebase implementation of the UserRepository interface
 */
class FirebaseUserRepository(
    private val db: FirebaseFirestore = Firebase.firestore
) : UserRepository {
    private val TAG = "FirebaseUserRepository"

    override fun getChildren(
        parentId: String,
        onSuccess: (List<EndUser>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("parents")
            .document(parentId)
            .get()
            .addOnSuccessListener { parentDoc ->
                val childrenIds = parentDoc.get("children") as? List<String>
                if (!childrenIds.isNullOrEmpty()) {
                    fetchEndUsers(childrenIds, onSuccess, onError)
                } else {
                    // Return empty list if no children
                    onSuccess(emptyList())
                }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error getting parent", error)
                onError(error)
            }
    }

    private fun fetchEndUsers(childrenIds: List<String>, onSuccess: (List<EndUser>) -> Unit, onError: (Exception) -> Unit) {
        db.collection("endUser")
            .whereIn(FieldPath.documentId(), childrenIds)
            .get()
            .addOnSuccessListener { endUserResults ->
                val childList = mutableListOf<EndUser>()
                for (endUserDoc in endUserResults) {
                    val childFromDB = endUserDoc.toObject(EndUser::class.java)
                    childList.add(childFromDB)
                }
                onSuccess(childList)
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error getting endUsers", error)
                onError(error)
            }
    }

    override fun deleteEndUser(
        parentId: String,
        endUserId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        // Implementation from previous version
        // ...
    }

    override fun checkParentPermission(
        endUserId: String,
        currentUserId: String,
        onSuccess: (Boolean) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("endUser")
            .document(endUserId)
            .get()
            .addOnSuccessListener { userDoc ->
                val parents = userDoc.get("parents") as? List<*>
                val isParent = parents?.contains(currentUserId) == true
                onSuccess(isParent)
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error checking parent permissions", error)
                onFailure(error)
            }
    }

    override fun checkUserRole(
        endUserId: String,
        currentUserId: String,
        onSuccess: (Boolean) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("endUser")
            .document(endUserId)
            .get()
            .addOnSuccessListener { document ->
                val endUser = document.toObject(EndUser::class.java)
                val isParent = endUser?.parents?.contains(currentUserId) == true
                onSuccess(isParent)
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error checking user role", error)
                onError(error)
            }
    }

    override fun loadUserAccess(
        endUserId: String,
        onSuccess: (List<UserManagementAdapter.UserItem>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("endUser")
            .document(endUserId)
            .get()
            .addOnSuccessListener { document ->
                val endUser = document.toObject(EndUser::class.java)
                val userIds = (endUser?.parents ?: emptyList()) + (endUser?.chaperones ?: emptyList())

                if (userIds.isEmpty()) {
                    onSuccess(emptyList())
                    return@addOnSuccessListener
                }

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
                                email = parent.email,
                                isParent = isParent
                            )
                        }
                        onSuccess(users)
                    }
                    .addOnFailureListener { error ->
                        Log.e(TAG, "Error loading users", error)
                        onError(error)
                    }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error loading end user", error)
                onError(error)
            }
    }

    override fun removeUserAccess(
        endUserId: String,
        userId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
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
                                onSuccess()
                            }
                            .addOnFailureListener { error ->
                                Log.e(TAG, "Error removing user access", error)
                                onError(error)
                            }
                    }
                    .addOnFailureListener { error ->
                        Log.e(TAG, "Error getting parent document", error)
                        onError(error)
                    }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error getting end user document", error)
                onError(error)
            }
    }
}