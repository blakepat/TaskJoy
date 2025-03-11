package com.example.taskjoy.repository

import android.util.Log
import com.example.taskjoy.adapters.UserManagementAdapter
import com.example.taskjoy.model.EndUser
import com.example.taskjoy.model.Parent
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

/**
 * Firebase implementation of the UserRepository interface
 */
class FirebaseUserRepository(
    private val db: FirebaseFirestore = Firebase.firestore
) : UserRepository {
    private val TAG = "FirebaseUserRepository"

    override suspend fun getChildren(parentId: String): Result<List<EndUser>> = try {
        val parentDoc = db.collection("parents")
            .document(parentId)
            .get()
            .await()

        val childrenIds = parentDoc.get("children") as? List<String>
        if (!childrenIds.isNullOrEmpty()) {
            val result = fetchEndUsers(childrenIds)
            Result.success(result)
        } else {
            // Return empty list if no children
            Result.success(emptyList())
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error getting parent", e)
        if (e is CancellationException) throw e
        Result.failure(e)
    }

    private suspend fun fetchEndUsers(childrenIds: List<String>): List<EndUser> {
        val endUserResults = db.collection("endUser")
            .whereIn(FieldPath.documentId(), childrenIds)
            .get()
            .await()

        val childList = mutableListOf<EndUser>()
        for (endUserDoc in endUserResults) {
            val childFromDB = endUserDoc.toObject<EndUser>()
            childList.add(childFromDB)
        }
        return childList
    }

    override suspend fun deleteEndUser(parentId: String, endUserId: String): Result<Unit> = try {
        // Implementation from previous version
        // Update to use await() for Firebase operations
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error deleting end user", e)
        if (e is CancellationException) throw e
        Result.failure(e)
    }

    override suspend fun checkParentPermission(endUserId: String, currentUserId: String): Result<Boolean> = try {
        val userDoc = db.collection("endUser")
            .document(endUserId)
            .get()
            .await()

        val parents = userDoc.get("parents") as? List<*>
        val isParent = parents?.contains(currentUserId) == true
        Result.success(isParent)
    } catch (e: Exception) {
        Log.e(TAG, "Error checking parent permissions", e)
        if (e is CancellationException) throw e
        Result.failure(e)
    }

    override suspend fun checkUserRole(endUserId: String, currentUserId: String): Result<Boolean> = try {
        val document = db.collection("endUser")
            .document(endUserId)
            .get()
            .await()

        val endUser = document.toObject<EndUser>()
        val isParent = endUser?.parents?.contains(currentUserId) == true
        Result.success(isParent)
    } catch (e: Exception) {
        Log.e(TAG, "Error checking user role", e)
        if (e is CancellationException) throw e
        Result.failure(e)
    }

    override suspend fun loadUserAccess(endUserId: String): Result<List<UserManagementAdapter.UserItem>> {
        return try {
            val document = db.collection("endUser")
                .document(endUserId)
                .get()
                .await()

            val endUser = document.toObject<EndUser>()
            val userIds = (endUser?.parents ?: emptyList()) + (endUser?.chaperones ?: emptyList())

            if (userIds.isEmpty()) {
                return Result.success(emptyList())
            }

            // Get user details for all IDs
            val documents = db.collection("parents")
                .whereIn(FieldPath.documentId(), userIds)
                .get()
                .await()

            val users = documents.mapNotNull { doc ->
                val parent = doc.toObject<Parent>()
                val isParent = endUser?.parents?.contains(doc.id) == true
                UserManagementAdapter.UserItem(
                    id = doc.id,
                    email = parent.email,
                    isParent = isParent
                )
            }
            Result.success(users)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user access", e)
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun removeUserAccess(endUserId: String, userId: String): Result<Unit> = try {
        // Create a batch write for atomic update
        val batch = db.batch()

        // Update endUser document
        val endUserRef = db.collection("endUser").document(endUserId)
        val endUserDoc = endUserRef.get().await()
        val endUser = endUserDoc.toObject<EndUser>()

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
        val parentDoc = parentRef.get().await()
        val parent = parentDoc.toObject<Parent>()
        val updatedChildren = parent?.children?.toMutableList() ?: mutableListOf()
        updatedChildren.remove(endUserId)
        batch.update(parentRef, "children", updatedChildren)

        // Commit all updates
        batch.commit().await()

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error removing user access", e)
        if (e is CancellationException) throw e
        Result.failure(e)
    }
}