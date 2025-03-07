package com.example.taskjoy.repository

import com.example.taskjoy.adapters.UserManagementAdapter
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

    /**
     * Check if the current user is a parent of the end user
     */
    fun checkUserRole(
        endUserId: String,
        currentUserId: String,
        onSuccess: (Boolean) -> Unit,
        onError: (Exception) -> Unit
    )

    /**
     * Load all users (parents and chaperones) for an end user
     */
    fun loadUserAccess(
        endUserId: String,
        onSuccess: (List<UserManagementAdapter.UserItem>) -> Unit,
        onError: (Exception) -> Unit
    )

    /**
     * Remove user access (parent or chaperone) from an end user
     */
    fun removeUserAccess(
        endUserId: String,
        userId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    )
}