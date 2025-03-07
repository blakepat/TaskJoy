package com.example.taskjoy.screens.Login

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.taskjoy.databinding.LoginScreenBinding
import com.example.taskjoy.screens.HomePage.MainActivity
import com.example.taskjoy.viewmodels.LoginViewModel
import com.google.android.material.snackbar.Snackbar

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: LoginScreenBinding

    // Initialize the ViewModel using the by viewModels() delegate
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()

        if (viewModel.isUserAuthenticated()) {
            val intent = Intent(this@LoginActivity, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnLogin.isEnabled = !isLoading
            binding.btnSignup.isEnabled = !isLoading
        }

        viewModel.loginSuccess.observe(this) { success ->
            if (success) {
                Snackbar.make(binding.root, "Login Successful", Snackbar.LENGTH_SHORT).show()
                val intent = Intent(this@LoginActivity, MainActivity::class.java)


                startActivity(intent)
            }
        }

        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            login()
        }

        binding.btnSignup.setOnClickListener {
            signup()
        }
    }

    private fun login() {
        if (binding.etEmail.text?.isNotEmpty() == true && (binding.etPassword.text?.isNotEmpty() == true)) {
            val emailFromUI = binding.etEmail.text.toString()
            val passwordFromUI = binding.etPassword.text.toString()

            viewModel.login(emailFromUI, passwordFromUI)
        } else {
            Snackbar.make(binding.root, "Ensure both forms are filled out", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun signup() {
        val intent = Intent(this@LoginActivity, CreateAccountActivity::class.java)
        startActivity(intent)
    }
}