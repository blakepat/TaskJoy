package com.example.taskjoy.screens

import android.os.Bundle
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

            if (formsFilledOutCorrectly()) {
                createAccount(email, passwordOne, name)
            }
        }
    }


    private fun formsFilledOutCorrectly(): Boolean {
        val email = binding.etEmail.text.toString()
        val name = binding.etName.text.toString()
        val passwordOne = binding.etPasswordOne.text.toString()
        val passwordTwo = binding.etPasswordTwo.text.toString()

        if (email.isEmpty() or name.isEmpty() or passwordOne.isEmpty() or passwordTwo.isEmpty()) {
            Snackbar.make(binding.root, "Please ensure all fields are filled out", Snackbar.LENGTH_SHORT).show()
            return false
        } else if ((passwordOne.count() < 6) or (passwordOne.count() > 20)) {
            Snackbar.make(binding.root, "Please ensure password meets requirements", Snackbar.LENGTH_SHORT).show()
            return false
        } else if (passwordOne != passwordTwo) {
            Snackbar.make(binding.root, "Passwords do not match", Snackbar.LENGTH_SHORT).show()
            return false
        } else {
            return true
        }
    }




    private fun createAccount(email: String, password: String, name: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val firebaseUser = auth.currentUser
                val user = Parent(
                    id = firebaseUser?.uid ?: "",
                    email = email,
                    name = name,
                    children = listOf()
                )

                firebaseUser?.uid?.let { id ->
                    db.collection("parents")
                        .document(id)
                        .set(user)
                        .addOnSuccessListener {
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Snackbar.make(binding.root, "Failed to create user $e ", Snackbar.LENGTH_SHORT).show()
                        }
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