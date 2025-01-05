package com.example.taskjoy.screens

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.taskjoy.databinding.ActivityStepDetailsBinding
import com.example.taskjoy.model.Step
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import androidx.core.content.ContextCompat
import com.example.taskjoy.R
import com.example.taskjoy.model.TaskJoyIcon
import com.google.firebase.Timestamp

class StepDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStepDetailsBinding
    private val db = Firebase.firestore
    private lateinit var step: Step

    private var currentPosition: Int = 0
    private lateinit var stepIds: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStepDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val stepId = intent.getStringExtra("stepId")
        currentPosition = intent.getIntExtra("currentPosition", 0)
        stepIds = intent.getStringArrayListExtra("stepIds") ?: arrayListOf()

        setupClickListeners()
        getStep(stepId ?: "")
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
    }

    private fun loadStep(stepId: String, newPosition: Int) {
        currentPosition = newPosition
        getStep(stepId)
        updateNavigationButtons()
    }

    private fun updateNavigationButtons() {
        // Allow navigation in all cases, just update the visual state
        binding.btnPrevStep.isEnabled = currentPosition > 0
        binding.btnNextStep.apply {
            isEnabled = true // Always enabled
//            setImageDrawable(
//                ContextCompat.getDrawable(
//                    this@StepDetailsActivity,
//                    if (currentPosition == stepIds.size - 1) R.drawable.ic_checkmark else R.drawable.ic_forward
//                )
//            )
        }
    }

    private fun updateCompletionStatus() {
        if (step.completed) {
            binding.completionStatusContainer.visibility = View.VISIBLE
            binding.btnResetCompletion.visibility = View.VISIBLE
            binding.completionText.text = "Completed ${step.completedAt?.let { formatTimestamp(it) } ?: ""}"
        } else {
            binding.completionStatusContainer.visibility = View.GONE
            binding.btnResetCompletion.visibility = View.GONE
        }
    }

    private fun formatTimestamp(timestamp: Timestamp): String {
        return android.text.format.DateFormat.format("MMM dd, yyyy", timestamp.toDate()).toString()
    }

    private fun completeAllSteps() {
        val batch = db.batch()
        val currentTime = Timestamp.now()

        stepIds.forEach { stepId ->
            val stepRef = db.collection("steps").document(stepId)
            batch.update(stepRef, mapOf(
                "completed" to true,
                "completedAt" to currentTime
            ))
        }

        batch.commit()
            .addOnSuccessListener {
                Snackbar.make(binding.root, "Routine completed!", Snackbar.LENGTH_SHORT).show()
                // Update UI immediately
                step.completed = true
                step.completedAt = currentTime
                updateCompletionStatus()

                binding.root.postDelayed({
                    finish()
                }, 1500)
            }
            .addOnFailureListener { error ->
                Log.e("StepDetailsActivity", "Error completing steps", error)
                Snackbar.make(binding.root, "Error completing routine", Snackbar.LENGTH_SHORT).show()
            }
    }

    private fun markStepAsComplete(onSuccess: (() -> Unit)? = null) {
        val stepRef = db.collection("steps").document(step.id)
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
        val stepRef = db.collection("steps").document(step.id)

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

    private fun getStep(stepId: String) {
        db.collection("steps")
            .document(stepId)
            .get()
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
        binding.stepImage.setImageResource(TaskJoyIcon.fromString(step.image).getDrawableResource())
    }
}