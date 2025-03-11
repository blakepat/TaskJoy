package com.example.taskjoy.repository

import com.example.taskjoy.adapters.UserManagementAdapter
import com.example.taskjoy.model.EndUser

interface UserRepository {

    suspend fun getChildren(parentId: String): Result<List<EndUser>>

    suspend fun deleteEndUser(parentId: String, endUserId: String): Result<Unit>

    suspend fun checkParentPermission(endUserId: String, currentUserId: String): Result<Boolean>

    suspend fun checkUserRole(endUserId: String, currentUserId: String): Result<Boolean>

    suspend fun loadUserAccess(endUserId: String): Result<List<UserManagementAdapter.UserItem>>

    suspend fun removeUserAccess(endUserId: String, userId: String): Result<Unit>
}