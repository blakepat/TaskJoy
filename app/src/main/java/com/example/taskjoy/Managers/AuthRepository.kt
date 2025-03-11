package com.example.taskjoy.repository

interface AuthRepository {

    suspend fun login(
        email: String,
        password: String
    ): Result<Unit>

    suspend fun createAccount(
        email: String,
        name: String,
        password: String
    ): Result<Unit>

    fun isUserAuthenticated(): Boolean

    fun getCurrentUserId(): String?

    fun signOut()
}