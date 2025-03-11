package com.example.taskjoy.screens.StepDetails

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.example.taskjoy.R
import com.example.taskjoy.databinding.ActivityStepDetailsBinding
import com.example.taskjoy.model.Step
import com.example.taskjoy.model.TaskJoyIcon
import com.example.taskjoy.viewmodels.StepDetailsViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@SuppressLint("InflateParams")
class StepDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStepDetailsBinding
    private lateinit var step: Step
    private var endUserId: String? = null
    private var routineId: String = ""
    private var isChildLockEnabled = false

    private var currentPosition: Int = 0
    private lateinit var stepIds: ArrayList<String>

    private val viewModel: StepDetailsViewModel by viewModels()

    private val preferences by lazy { getSharedPreferences("TaskJoyPrefs", Context.MODE_PRIVATE) }

    private val celebrationDialog by lazy {
        val dialogView = layoutInflater.inflate(R.layout.dialog_celebration, null)
        AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Yay! 🎉") { _, _ ->
                if (!isChildLockEnabled) {
                    finish()
                }
            }
            .create()
    }

    private val someUnfinishedDialog by lazy {
        val dialogView = layoutInflater.inflate(R.layout.dialog_did_my_best, null)
        AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .setPositiveButton("I did my best! 👌") { _, _ ->
                if (!isChildLockEnabled) {
                    finish()
                }
            }
            .create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStepDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val stepId = intent.getStringExtra("stepId")
        currentPosition = intent.getIntExtra("currentPosition", 0)
        stepIds = intent.getStringArrayListExtra("stepIds") ?: arrayListOf()
        endUserId = intent.getStringExtra("endUser")
        routineId = intent.getStringExtra("routineId") ?: ""

        setupClickListeners()
        setupObservers()
        getStep(stepId ?: "")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isChildLockEnabled) {
                    showPasswordDialog()
                } else {
                    finish()
                }
            }
        })
    }

    private fun setupObservers() {
        viewModel.step.observe(this) { updatedStep ->
            step = updatedStep
            setupUI()
            updateNavigationButtons()
            updateCompletionStatus()
        }


        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        viewModel.saveNotesSuccess.observe(this) { success ->
            if (success) {
                Snackbar.make(binding.root, "Notes saved!", Snackbar.LENGTH_SHORT).show()
            }
        }

        viewModel.allStepsCompleted.observe(this) { completed ->
            if (completed) {
                Snackbar.make(binding.root, "Routine completed!", Snackbar.LENGTH_SHORT).show()
                showCompletionCelebration()
            }
        }

        viewModel.hasIncompleteSteps.observe(this) { hasIncomplete ->
            if (hasIncomplete != null && currentPosition == stepIds.size - 1) {
                // If we're on the last step and checking completion status
                if (hasIncomplete) {
                    showSomeUnfinishedDialog()
                } else {
                    showCompletionCelebration()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.step_details_menu, menu)
        menu.findItem(R.id.action_child_lock).setIcon(
            if (isChildLockEnabled) R.drawable.ic_lock
            else R.drawable.ic_lock_open
        )
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_child_lock -> {
                toggleChildLock()
                true
            }
            android.R.id.home -> {
                if (isChildLockEnabled) {
                    showPasswordDialog()
                } else {
                    finish()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleChildLock() {
        if (isChildLockEnabled) {

            showPasswordDialog(onSuccess = {
                isChildLockEnabled = false
                updateChildLockUI()
                invalidateOptionsMenu()

                preferences.edit().putBoolean("childLockEnabled", false).apply()

                Snackbar.make(binding.root, "Child Lock Disabled", Snackbar.LENGTH_SHORT).show()
            })
        } else {
            isChildLockEnabled = true
            updateChildLockUI()
            invalidateOptionsMenu()

            preferences.edit().putBoolean("childLockEnabled", true).apply()

            Snackbar.make(binding.root, "Child Lock Enabled", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showPasswordDialog(onSuccess: () -> Unit = { finish() }) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_password, null)
        val passwordInput = dialogView.findViewById<TextInputEditText>(R.id.passwordInput)

        AlertDialog.Builder(this)
            .setTitle("Enter Password to Unlock")
            .setView(dialogView)
            .setPositiveButton("Unlock") { _, _ ->
                val password = passwordInput.text.toString()
                validatePassword(password, onSuccess)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun validatePassword(password: String, onSuccess: () -> Unit) {
        val user = Firebase.auth.currentUser
        val email = user?.email

        if (user != null && email != null) {
            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticateAndRetrieveData(credential)
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener {
                    Snackbar.make(binding.root, "Incorrect password", Snackbar.LENGTH_SHORT).show()
                }
        } else {
            Snackbar.make(binding.root, "Authentication error", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun updateChildLockUI() {
        binding.notesInputLayout.visibility = if (isChildLockEnabled) View.GONE else View.VISIBLE
        binding.btnSaveNotes.visibility = if (isChildLockEnabled) View.GONE else View.VISIBLE
    }

    private fun setupClickListeners() {
        binding.btnPrevStep.setOnClickListener {
            if (currentPosition > 0) {
                loadStep(stepIds[currentPosition - 1], currentPosition - 1)
            }
        }

        binding.btnNextStep.setOnClickListener {
            if (currentPosition < stepIds.size - 1) {
                markStepAsComplete {
                    loadStep(stepIds[currentPosition + 1], currentPosition + 1)
                }
            } else {
                // On last step - check if all steps are complete
                markStepAsComplete {
                    checkRoutineCompletion()
                }
            }
        }


        binding.btnSkipStep.setOnClickListener {
            if (currentPosition < stepIds.size - 1) {
                // If not the last step, just move to next step
                loadStep(stepIds[currentPosition + 1], currentPosition + 1)
            } else {
                // If last step, check completion status using ViewModel
                viewModel.areAnyStepsIncomplete(endUserId ?: return@setOnClickListener, routineId) { hasIncomplete ->
                    if (hasIncomplete) {
                        someUnfinishedDialog.show()
                    } else {
                        showCompletionCelebration()
                    }
                }
            }
        }

        binding.btnResetCompletion.setOnClickListener {
            markStepAsIncomplete()
        }

        binding.btnSaveNotes.setOnClickListener {
            saveNotes()
        }
    }

    private fun checkRoutineCompletion() {
        if (endUserId == null || routineId.isEmpty()) {
            Log.e("StepDetailsActivity", "Cannot check completion: missing required data")
            return
        }

        viewModel.checkForIncompleteSteps(endUserId!!, routineId)
    }

    private fun showSomeUnfinishedDialog() {
        someUnfinishedDialog.show()
    }

    private fun loadStep(stepId: String, newPosition: Int) {
        currentPosition = newPosition
        getStep(stepId)
        updateNavigationButtons()
    }

    private fun showCompletionCelebration() {
        celebrationDialog.show()
    }

    private fun updateNavigationButtons() {
        binding.btnPrevStep.isEnabled = currentPosition > 0

        // Update Next button based on completion status
        binding.btnNextStep.apply {
            // If step is already completed, hide the Next button
            visibility = if (step.completed) View.GONE else View.VISIBLE
            isEnabled = !step.completed
        }

        // Always show the Skip button, but update its text based on position
        binding.btnSkipStep.apply {
            text = if (currentPosition < stepIds.size - 1)
                "Skip"
            else
                "Finish"
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateCompletionStatus() {
        if (step.completed) {
            binding.completionStatusContainer.visibility = View.VISIBLE
            binding.btnResetCompletion.visibility = View.VISIBLE
            binding.completionText.text = "Completed!"
        } else {
            binding.completionStatusContainer.visibility = View.GONE
            binding.btnResetCompletion.visibility = View.GONE
        }

        updateNavigationButtons()
    }

    private fun completeAllSteps() {
        if (endUserId == null || routineId.isEmpty()) {
            Log.e("StepDetailsActivity", "Cannot complete steps: missing required data")
            return
        }

        viewModel.areAnyStepsIncomplete(endUserId!!, routineId) { hasIncomplete ->
            if (hasIncomplete) {
                someUnfinishedDialog.show()
            } else {
                viewModel.completeAllSteps(endUserId!!, routineId)
            }
        }
    }

    private fun markStepAsComplete(onSuccess: (() -> Unit)? = null) {
        val currentStepId = step.id

        val completionObserver = object : androidx.lifecycle.Observer<Step> {
            override fun onChanged(updatedStep: Step) {
                if (updatedStep.id == currentStepId && updatedStep.completed) {
                    onSuccess?.invoke()
                    viewModel.step.removeObserver(this)
                }
            }
        }

        viewModel.step.observe(this, completionObserver)

        viewModel.markStepAsComplete(
            endUserId = endUserId ?: run {
                viewModel.step.removeObserver(completionObserver)
                return
            },
            routineId = routineId,
            stepId = currentStepId
        )
    }

    private fun markStepAsIncomplete() {
        viewModel.markStepAsIncomplete(
            endUserId = endUserId ?: return,
            routineId = routineId,
            stepId = step.id
        )
    }


    private fun saveNotes() {
        val newNotes = binding.notesEditText.text.toString()

        viewModel.saveStepNotes(
            endUserId = endUserId ?: return,
            routineId = routineId,
            stepId = step.id,
            notes = newNotes
        )
    }

    private fun getStep(stepId: String) {
        viewModel.getStep(
            endUserId = endUserId ?: return,
            routineId = routineId,
            stepId = stepId
        )
    }

    private fun setupUI() {
        binding.textStepTitle.text = step.name
        binding.stepDescriptionText.text = step.description
        binding.notesEditText.setText(step.notes)

        try {
            if (step.image == TaskJoyIcon.CUSTOM.name && step.customIconPath != null) {
                Glide.with(this)
                    .load(File(step.customIconPath.toString()))
                    .centerCrop()
                    .error(R.drawable.ic_brush_teeth)
                    .into(binding.stepImage)
            } else {
                try {
                    val icon = TaskJoyIcon.valueOf(step.image.uppercase())
                    binding.stepImage.setImageResource(icon.drawableResId)
                } catch (e: IllegalArgumentException) {
                    binding.stepImage.setImageResource(R.drawable.ic_brush_teeth)
                    Log.e("StepDetailsActivity", "Invalid icon name: ${step.image}", e)
                }
            }
        } catch (e: Exception) {
            binding.stepImage.setImageResource(R.drawable.ic_brush_teeth)
            Log.e("StepDetailsActivity", "Error setting step icon", e)
        }
    }
}