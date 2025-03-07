package com.example.taskjoy.repository

/**
 * Repository interface for authentication operations
 */
interface AuthRepository {
    /**
     * Sign in with email and password
     */
    suspend fun login(
        email: String,
        password: String
    ): Result<Unit>

    /**
     * Create a new user account with email and password
     */
    suspend fun createAccount(
        email: String,
        name: String,
        password: String
    ): Result<Unit>

    /**
     * Check if a user is currently authenticated
     */
    fun isUserAuthenticated(): Boolean

    /**
     * Get the current user's ID
     */
    fun getCurrentUserId(): String?

    /**
     * Sign out the current user
     */
    fun signOut()
}