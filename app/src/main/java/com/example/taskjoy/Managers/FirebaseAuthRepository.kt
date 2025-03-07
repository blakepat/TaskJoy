package com.example.taskjoy.repository

import android.util.Log
import com.example.taskjoy.model.Parent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * Firebase implementation of the AuthRepository interface
 */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth = Firebase.auth,
    private val db: FirebaseFirestore = Firebase.firestore
) : AuthRepository {
    private val TAG = "FirebaseAuthRepository"

    override fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Login failed", exception)
                onError(exception)
            }
    }

    override fun createAccount(
        email: String,
        name: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val firebaseUser = auth.currentUser

                if (firebaseUser != null) {
                    val parent = Parent(
                        id = firebaseUser.uid,
                        email = email,
                        name = name,
                        children = listOf()
                    )

                    db.collection("parents")
                        .document(firebaseUser.uid)
                        .set(parent)
                        .addOnSuccessListener {
                            onSuccess()
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "Failed to create user profile", exception)
                            onError(exception)
                        }
                } else {
                    val error = Exception("Firebase user was null after successful authentication")
                    Log.e(TAG, error.message.toString())
                    onError(error)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to create account", exception)
                onError(exception)
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