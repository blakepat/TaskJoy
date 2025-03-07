package com.example.taskjoy.repository

/**
 * Service façade for authentication operations
 */
class AuthService(
    private val authRepository: AuthRepository = FirebaseAuthRepository()
) {
    /**
     * Sign in with email and password
     */
    suspend fun login(
        email: String,
        password: String
    ): Result<Unit> = authRepository.login(email, password)

    /**
     * Create a new user account with email and password
     */
    suspend fun createAccount(
        email: String,
        name: String,
        password: String
    ): Result<Unit> = authRepository.createAccount(email, name, password)

    /**
     * Check if a user is currently authenticated
     */
    fun isUserAuthenticated(): Boolean = authRepository.isUserAuthenticated()

    /**
     * Get the current user's ID
     */
    fun getCurrentUserId(): String? = authRepository.getCurrentUserId()

    /**
     * Sign out the current user
     */
    fun signOut() = authRepository.signOut()
}