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
    suspend fun getChildren(parentId: String): Result<List<EndUser>>

    /**
     * Delete an end user and all associated data
     */
    suspend fun deleteEndUser(parentId: String, endUserId: String): Result<Unit>

    /**
     * Check if a user has parent permissions for an end user
     */
    suspend fun checkParentPermission(endUserId: String, currentUserId: String): Result<Boolean>

    /**
     * Check if the current user is a parent of the end user
     */
    suspend fun checkUserRole(endUserId: String, currentUserId: String): Result<Boolean>

    /**
     * Load all users (parents and chaperones) for an end user
     */
    suspend fun loadUserAccess(endUserId: String): Result<List<UserManagementAdapter.UserItem>>

    /**
     * Remove user access (parent or chaperone) from an end user
     */
    suspend fun removeUserAccess(endUserId: String, userId: String): Result<Unit>
}