package com.example.taskjoy.repository

import android.util.Log
import com.example.taskjoy.model.Parent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

/**
 * Firebase implementation of the AuthRepository interface
 */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth = Firebase.auth,
    private val db: FirebaseFirestore = Firebase.firestore
) : AuthRepository {
    private val TAG = "FirebaseAuthRepository"

    override suspend fun login(
        email: String,
        password: String
    ): Result<Unit> = try {
        auth.signInWithEmailAndPassword(email, password).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Login failed", e)
        if (e is CancellationException) throw e
        Result.failure(e)
    }

    override suspend fun createAccount(
        email: String,
        name: String,
        password: String
    ): Result<Unit> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Firebase user was null after successful authentication"))

            val parent = Parent(
                id = firebaseUser.uid,
                email = email,
                name = name,
                children = listOf()
            )

            db.collection("parents")
                .document(firebaseUser.uid)
                .set(parent)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Create account failed", e)
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override fun isUserAuthenticated(): Boolean {
        return auth.currentUser != null
    }

    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    override fun signOut() {
        auth.signOut()
    }
}