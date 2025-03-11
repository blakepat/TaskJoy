package com.example.taskjoy.screens.Login

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.taskjoy.databinding.CreateAccountScreenBinding
import com.example.taskjoy.viewmodels.CreateAccountViewModel
import com.google.android.material.snackbar.Snackbar

class CreateAccountActivity : AppCompatActivity() {

    private lateinit var binding: CreateAccountScreenBinding

    private val viewModel: CreateAccountViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CreateAccountScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Create Account"

        setupClickListeners()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnSignup.isEnabled = !isLoading
        }

        viewModel.createAccountSuccess.observe(this) { success ->
            if (success) {
                Snackbar.make(binding.root, "Account created successfully", Snackbar.LENGTH_SHORT).show()
                finish()
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
        binding.btnSignup.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val name = binding.etName.text.toString()
            val passwordOne = binding.etPasswordOne.text.toString()
            val passwordTwo = binding.etPasswordTwo.text.toString()

            val (isValid, errorMessage) = viewModel.validateForm(email, name, passwordOne, passwordTwo)

            if (isValid) {
                viewModel.createAccount(email, name, passwordOne)
            } else {
                errorMessage?.let {
                    Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                }
            }
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