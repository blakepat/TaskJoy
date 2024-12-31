package com.example.taskjoy.screens

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.taskjoy.databinding.ActivityStepDetailsBinding
import com.example.taskjoy.model.Step
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import androidx.core.content.ContextCompat
import com.example.taskjoy.R

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

        // Set up navigation button click listeners
        binding.btnPrevStep.setOnClickListener {
            if (currentPosition > 0) {
                loadStep(stepIds[currentPosition - 1], currentPosition - 1)
            }
        }

        binding.btnNextStep.setOnClickListener {
            if (currentPosition < stepIds.size - 1) {
                loadStep(stepIds[currentPosition + 1], currentPosition + 1)
            } else {
                // This is the last step - you might want to show completion UI or return to list
                finish()
            }
        }

        getStep(stepId ?: "")
    }

    private fun loadStep(stepId: String, newPosition: Int) {
        currentPosition = newPosition
        getStep(stepId)
        updateNavigationButtons()
    }

    private fun updateNavigationButtons() {
        // Update button states based on position
        binding.btnPrevStep.isEnabled = currentPosition > 0
        binding.btnNextStep.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                if (currentPosition == stepIds.size - 1) R.drawable.ic_checkmark else R.drawable.ic_forward
            )
        )
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
        // Add other UI setup code here
    }
}