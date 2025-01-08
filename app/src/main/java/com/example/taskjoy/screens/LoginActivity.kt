package com.example.taskjoy.screens

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.taskjoy.databinding.LoginScreenBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: LoginScreenBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        createClickListeners()
    }


    override fun onResume() {
        super.onResume()

        if (auth.currentUser != null) {
            val intent = Intent(this@LoginActivity, MainActivity::class.java)
            startActivity(intent)
        }
    }


    private fun createClickListeners() {
        binding.btnLogin.setOnClickListener {
            login()
        }

        binding.btnSignup.setOnClickListener {
            signup()
        }

        binding.btnGuest.setOnClickListener {
            //TODO: CREATE INTENT TO GO TO THIRD SCREEN
//            val intent = Intent(this@LoginActivity, CreateAccountActivity::class.java)
//            startActivity(intent)
        }
    }


    private fun login() {
        if ((binding.etEmail.text?.isNotEmpty() == true) && (binding.etPassword.text?.isNotEmpty() == true)) {
            val emailFromUI = binding.etEmail.text.toString()
            val passwordFromUI = binding.etPassword.text.toString()

            auth.signInWithEmailAndPassword(emailFromUI, passwordFromUI)
                .addOnCompleteListener(this) {
                        task ->
                    if (task.isSuccessful) {
                        Snackbar.make(binding.root, "Login Successful", Snackbar.LENGTH_SHORT).show()
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                    } else {
                        Snackbar.make(binding.root, "Login Failed", Snackbar.LENGTH_SHORT).show()
                    }
                }
        } else {
            Snackbar.make(binding.root, "Ensure both forms are filled out", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun signup() {
        val intent = Intent(this@LoginActivity, CreateAccountActivity::class.java)
        startActivity(intent)
    }
}