package com.example.taskjoy.repository

import android.util.Log
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

    private fun fetchEndUsers(
        childrenIds: List<String>,
        onSuccess: (List<EndUser>) -> Unit,
        onError: (Exception) -> Unit
    ) {
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
        // First get parent document to update the children list
        db.collection("parents").document(parentId).get()
            .addOnSuccessListener { document ->
                val parent = document.toObject(Parent::class.java)
                val updatedChildren = parent?.children?.toMutableList() ?: mutableListOf()
                updatedChildren.remove(endUserId)

                updateParentDocument(parentId, updatedChildren, endUserId, onSuccess, onError)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error accessing parent data", e)
                onError(e)
            }
    }

    private fun updateParentDocument(
        parentId: String,
        updatedChildren: List<String>,
        endUserId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("parents").document(parentId)
            .update("children", updatedChildren)
            .addOnSuccessListener {
                // After updating parent, delete all related endUser data
                performEndUserDeletion(parentId, endUserId, onSuccess, onError)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating parent", e)
                onError(e)
            }
    }

    private fun performEndUserDeletion(
        parentId: String,
        endUserId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        // Use transaction to delete all related data
        val batch = db.batch()

        // 1. Delete endUser document
        val endUserRef = db.collection("endUser").document(endUserId)
        batch.delete(endUserRef)

        // 2. Delete from parent's children collection
        val parentChildRef = db.collection("parents").document(parentId)
            .collection("children").document(endUserId)
        batch.delete(parentChildRef)

        // Commit the batch
        batch.commit()
            .addOnSuccessListener {
                // Now handle collections that need separate queries
                deleteEndUserRoutines(endUserId)
                deleteEndUserTemplates(endUserId)
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error in batch deletion", e)
                onError(e)
            }
    }

    private fun deleteEndUserRoutines(endUserId: String) {
        db.collection("endUser")
            .document(endUserId)
            .collection("dailyRoutines")
            .get()
            .addOnSuccessListener { routines ->
                val routineBatch = db.batch()
                routines.forEach { routine ->
                    routineBatch.delete(routine.reference)
                }
                routineBatch.commit().addOnFailureListener { e ->
                    Log.e(TAG, "Error deleting routines", e)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching routines to delete", e)
            }
    }

    private fun deleteEndUserTemplates(endUserId: String) {
        db.collection("routineTemplates")
            .whereEqualTo("endUserId", endUserId)
            .get()
            .addOnSuccessListener { templates ->
                val templateBatch = db.batch()
                templates.forEach { template ->
                    templateBatch.delete(template.reference)
                }
                templateBatch.commit().addOnFailureListener { e ->
                    Log.e(TAG, "Error deleting templates", e)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching templates to delete", e)
            }
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
}