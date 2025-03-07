package com.example.taskjoy.screens.Login

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.taskjoy.databinding.CreateAccountScreenBinding
import com.example.taskjoy.model.Parent
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class CreateAccountActivity : AppCompatActivity() {

    private lateinit var binding: CreateAccountScreenBinding
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CreateAccountScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Create Account"

        setupClickListeners()
    }


    private fun setupClickListeners() {
        binding.btnSignup.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val name = binding.etName.text.toString()
            val passwordOne = binding.etPasswordOne.text.toString()
            val passwordTwo = binding.etPasswordTwo.text.toString()

            if (formsFilledOutCorrectly(email, name, passwordOne, passwordTwo)) {
                createAccount(email, name, passwordOne)
            }
        }
    }


    private fun formsFilledOutCorrectly(email: String, name: String, passwordOne: String, passwordTwo: String): Boolean {
        if (email.isEmpty() || name.isEmpty() || passwordOne.isEmpty() || passwordTwo.isEmpty()) {
            Snackbar.make(binding.root, "Please ensure all fields are filled out", Snackbar.LENGTH_SHORT).show()
            return false
        } else if ((passwordOne.length < 6) || (passwordOne.length > 20)) {
            Snackbar.make(binding.root, "Please ensure password meets requirements", Snackbar.LENGTH_SHORT).show()
            return false
        } else if (passwordOne != passwordTwo) {
            Snackbar.make(binding.root, "Passwords do not match", Snackbar.LENGTH_SHORT).show()
            return false
        } else {
            return true
        }
    }


    private fun createAccount(email: String, name: String, password: String) {
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
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Log.e("CreateAccount", "Database error: ${e.message}", e)
                            Snackbar.make(binding.root, "Failed to create user profile. Please try again.", Snackbar.LENGTH_SHORT).show()
                        }
                } else {
                    Log.e("CreateAccount", "Firebase user was null after successful authentication")
                    Snackbar.make(binding.root, "Account creation error. Please try again.", Snackbar.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Snackbar.make(binding.root, "Failed to create authorization $e", Snackbar.LENGTH_SHORT).show()
            }
    }



    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}