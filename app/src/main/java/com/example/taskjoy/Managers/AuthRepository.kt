package com.example.taskjoy.repository

/**
 * Repository interface for authentication operations
 */
interface AuthRepository {
    /**
     * Sign in with email and password
     */
    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    )

    /**
     * Create a new user account with email and password
     */
    fun createAccount(
        email: String,
        name: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    )

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