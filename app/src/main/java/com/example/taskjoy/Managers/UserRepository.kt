package com.example.taskjoy.repository

import com.example.taskjoy.model.EndUser

/**
 * Repository interface for user-related operations
 */
interface UserRepository {
    /**
     * Get all children for a parent
     */
    fun getChildren(
        parentId: String,
        onSuccess: (List<EndUser>) -> Unit,
        onError: (Exception) -> Unit
    )

    /**
     * Delete an end user and all associated data
     */
    fun deleteEndUser(
        parentId: String,
        endUserId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    )

    /**
     * Check if a user has parent permissions for an end user
     */
    fun checkParentPermission(
        endUserId: String,
        currentUserId: String,
        onSuccess: (Boolean) -> Unit,
        onFailure: (Exception) -> Unit
    )
}