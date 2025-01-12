package com.example.taskjoy.screens

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.taskjoy.R
import com.example.taskjoy.databinding.ActivityStepDetailsBinding
import com.example.taskjoy.model.Step
import com.example.taskjoy.model.TaskJoyIcon
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import android.content.Context
import androidx.activity.OnBackPressedCallback
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.AuthCredential
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.File

class StepDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStepDetailsBinding
    private val db = Firebase.firestore
    private lateinit var step: Step
    private var endUserId: String? = null
    private var routineId: String = ""
    private var isChildLockEnabled = false

    private var currentPosition: Int = 0
    private lateinit var stepIds: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStepDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable the up button in the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val stepId = intent.getStringExtra("stepId")
        currentPosition = intent.getIntExtra("currentPosition", 0)
        stepIds = intent.getStringArrayListExtra("stepIds") ?: arrayListOf()
        endUserId = intent.getStringExtra("endUser")
        routineId = intent.getStringExtra("routineId") ?: ""

        setupClickListeners()
        getStep(stepId ?: "")

        // Handle back press
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
            // If trying to disable child lock, show password dialog
            showPasswordDialog(onSuccess = {
                isChildLockEnabled = false
                updateChildLockUI()
                invalidateOptionsMenu()

                // Save child lock state
                val prefs = getSharedPreferences("TaskJoyPrefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("childLockEnabled", false).apply()

                Snackbar.make(binding.root, "Child Lock Disabled", Snackbar.LENGTH_SHORT).show()
            })
        } else {
            // Enable child lock without password
            isChildLockEnabled = true
            updateChildLockUI()
            invalidateOptionsMenu()

            // Save child lock state
            val prefs = getSharedPreferences("TaskJoyPrefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("childLockEnabled", true).apply()

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

    private fun showPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_password, null)
        val passwordInput = dialogView.findViewById<TextInputEditText>(R.id.passwordInput)

        AlertDialog.Builder(this)
            .setTitle("Enter Password to Exit")
            .setView(dialogView)
            .setPositiveButton("Unlock") { _, _ ->
                val password = passwordInput.text.toString()
                validatePassword(password)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun validatePassword(password: String) {
        val user = Firebase.auth.currentUser
        val email = user?.email

        if (user != null && email != null) {
            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticateAndRetrieveData(credential)
                .addOnSuccessListener {
                    isChildLockEnabled = false
                    finish()
                }
                .addOnFailureListener {
                    Snackbar.make(binding.root, "Incorrect password", Snackbar.LENGTH_SHORT).show()
                }
        } else {
            Snackbar.make(binding.root, "Authentication error", Snackbar.LENGTH_SHORT).show()
        }
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
                completeAllSteps()
            }
        }

        binding.btnResetCompletion.setOnClickListener {
            markStepAsIncomplete()
        }

        binding.btnSaveNotes.setOnClickListener {
            saveNotes()
        }
    }

    private fun loadStep(stepId: String, newPosition: Int) {
        currentPosition = newPosition
        getStep(stepId)
        updateNavigationButtons()
    }

    private fun updateNavigationButtons() {
        binding.btnPrevStep.isEnabled = currentPosition > 0
        binding.btnNextStep.apply {
            isEnabled = true
        }
    }

    private fun updateCompletionStatus() {
        if (step.completed) {
            binding.completionStatusContainer.visibility = View.VISIBLE
            binding.btnResetCompletion.visibility = View.VISIBLE
            binding.completionText.text = "Completed!"
        } else {
            binding.completionStatusContainer.visibility = View.GONE
            binding.btnResetCompletion.visibility = View.GONE
        }
    }

    private fun showCompletionCelebration() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_celebration, null)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Yay! 🎉") { _, _ ->
                if (!isChildLockEnabled) {
                    finish()
                }
            }
            .show()
    }

    private fun completeAllSteps() {
        if (endUserId == null || routineId.isEmpty()) {
            Log.e("StepDetailsActivity", "Cannot complete steps: missing required data")
            return
        }

        val dailyStepsRef = db.collection("endUser")
            .document(endUserId!!)
            .collection("dailyRoutines")
            .document(routineId)
            .collection("dailySteps")

        dailyStepsRef.get()
            .addOnSuccessListener { stepsSnapshot ->
                val batch = db.batch()
                val currentTime = Timestamp.now()

                stepsSnapshot.documents.forEach { stepDoc ->
                    batch.update(stepDoc.reference, mapOf(
                        "completed" to true,
                        "completedAt" to currentTime
                    ))
                }

                batch.commit()
                    .addOnSuccessListener {
                        Snackbar.make(binding.root, "Routine completed!", Snackbar.LENGTH_SHORT).show()
                        step.completed = true
                        step.completedAt = currentTime
                        updateCompletionStatus()
                        showCompletionCelebration()
                    }
                    .addOnFailureListener { error ->
                        Log.e("StepDetailsActivity", "Error completing steps", error)
                        Snackbar.make(binding.root, "Error completing routine", Snackbar.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { error ->
                Log.e("StepDetailsActivity", "Error fetching steps to complete", error)
                Snackbar.make(binding.root, "Error completing routine", Snackbar.LENGTH_SHORT).show()
            }
    }

    private fun markStepAsComplete(onSuccess: (() -> Unit)? = null) {
        val stepRef = db.collection("endUser")
            .document(endUserId!!)
            .collection("dailyRoutines")
            .document(routineId)
            .collection("dailySteps")
            .document(step.id)

        val currentTime = Timestamp.now()

        stepRef.update(mapOf(
            "completed" to true,
            "completedAt" to currentTime
        )).addOnSuccessListener {
            step.completed = true
            step.completedAt = currentTime
            updateCompletionStatus()
            updateNavigationButtons()
            Snackbar.make(binding.root, "Step completed!", Snackbar.LENGTH_SHORT).show()
            onSuccess?.invoke()
        }.addOnFailureListener { error ->
            Log.e("StepDetailsActivity", "Error marking step as complete", error)
            Snackbar.make(binding.root, "Error updating step", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun markStepAsIncomplete() {
        val stepRef = db.collection("endUser")
            .document(endUserId!!)
            .collection("dailyRoutines")
            .document(routineId)
            .collection("dailySteps")
            .document(step.id)

        stepRef.update(mapOf(
            "completed" to false,
            "completedAt" to null
        )).addOnSuccessListener {
            step.completed = false
            step.completedAt = null
            updateCompletionStatus()
            updateNavigationButtons()
            Snackbar.make(binding.root, "Step marked as incomplete", Snackbar.LENGTH_SHORT).show()
        }.addOnFailureListener { error ->
            Log.e("StepDetailsActivity", "Error marking step as incomplete", error)
            Snackbar.make(binding.root, "Error updating step", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun saveNotes() {
        val newNotes = binding.notesEditText.text.toString()
        val stepRef = db.collection("endUser")
            .document(endUserId!!)
            .collection("dailyRoutines")
            .document(routineId)
            .collection("dailySteps")
            .document(step.id)

        stepRef.update("notes", newNotes)
            .addOnSuccessListener {
                step.notes = newNotes
                Snackbar.make(binding.root, "Notes saved!", Snackbar.LENGTH_SHORT).show()
            }
            .addOnFailureListener { error ->
                Log.e("StepDetailsActivity", "Error saving notes", error)
                Snackbar.make(binding.root, "Error saving notes", Snackbar.LENGTH_SHORT).show()
            }
    }

    private fun getStep(stepId: String) {
        val stepRef = db.collection("endUser")
            .document(endUserId!!)
            .collection("dailyRoutines")
            .document(routineId)
            .collection("dailySteps")
            .document(stepId)

        stepRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val stepFromDB: Step = document.toObject(Step::class.java)!!
                    step = stepFromDB
                    setupUI()
                    updateNavigationButtons()
                    updateCompletionStatus()
                } else {
                    Log.w("TESTING", "No such document")
                    Snackbar.make(binding.root, "Step not found", Snackbar.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { error ->
                Log.w("TESTING", "Error getting step document.", error)
                Snackbar.make(binding.root, "Error getting step", Snackbar.LENGTH_SHORT).show()
            }
    }

    private fun setupUI() {
        binding.textStepTitle.text = step.name
        binding.stepDescriptionText.text = step.description
        binding.notesEditText.setText(step.notes)

        try {
            if (step.image == TaskJoyIcon.CUSTOM.name && step.customIconPath != null) {
                Glide.with(this)
                    .load(File(step.customIconPath))
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