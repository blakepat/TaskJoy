package com.example.taskjoy.repository


class AuthService(
    //FOR Testability we have can swap this out for a local AuthRepo.
    private val authRepository: AuthRepository = FirebaseAuthRepository()
) {

    suspend fun login(
        email: String,
        password: String
    ): Result<Unit> = authRepository.login(email, password)


    suspend fun createAccount(
        email: String,
        name: String,
        password: String
    ): Result<Unit> = authRepository.createAccount(email, name, password)

    fun isUserAuthenticated(): Boolean = authRepository.isUserAuthenticated()

    fun getCurrentUserId(): String? = authRepository.getCurrentUserId()

    fun signOut() = authRepository.signOut()
}