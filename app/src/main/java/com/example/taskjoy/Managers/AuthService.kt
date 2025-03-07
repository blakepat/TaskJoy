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
    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) = authRepository.login(email, password, onSuccess, onError)

    /**
     * Create a new user account with email and password
     */
    fun createAccount(
        email: String,
        name: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) = authRepository.createAccount(email, name, password, onSuccess, onError)

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