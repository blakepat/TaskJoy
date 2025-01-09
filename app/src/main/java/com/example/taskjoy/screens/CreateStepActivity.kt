package com.example.taskjoy.screens

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.taskjoy.adapters.IconAdapter
import com.example.taskjoy.databinding.CreateStepScreenBinding
import com.example.taskjoy.model.Step
import com.example.taskjoy.model.TaskJoyIcon
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CreateStepActivity : AppCompatActivity() {
    private lateinit var binding: CreateStepScreenBinding
    private lateinit var db: FirebaseFirestore
    private var userId: String = "" // User ID
    private var routineId: String = "" // Routine ID
    private var stepId: String? = null // Step ID (null for create, non-null for edit)
    private var selectedIcon: TaskJoyIcon = TaskJoyIcon.BRUSHTEETH // Default icon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CreateStepScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        // Retrieve userId and routineId from intent
        userId = intent.getStringExtra("userId") ?: run {
            Log.w("TESTING", "Error: User ID not provided.")
            Toast.makeText(this, "Error: User ID not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        routineId = intent.getStringExtra("routineId") ?: run {
            Log.w("TESTING", "Error: Routine ID not provided.")
            Toast.makeText(this, "Error: Routine ID not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Get stepId if we're editing
        stepId = intent.getStringExtra("stepId")

        setupIconRecyclerView()
        setupClickListeners()

        // If we're editing, load the existing step data
        stepId?.let { loadExistingStep(it) }
    }

    private fun loadExistingStep(stepId: String) {
        db.collection("steps").document(stepId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val step = document.toObject(Step::class.java)
                    step?.let {
                        // Populate the fields
                        binding.etStepName.setText(it.name)
                        binding.etStepNotes.setText(it.notes)

                        // Update selected icon
                        try {
                            selectedIcon = TaskJoyIcon.valueOf(it.image)
                            // Refresh the icon adapter with the new selection
                            setupIconRecyclerView()
                        } catch (e: IllegalArgumentException) {
                            Log.w("ERROR", "Invalid icon name: ${it.image}")
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w("ERROR", "Error loading step: ${e.message}")
                Toast.makeText(this, "Error loading step data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupIconRecyclerView() {
        val iconAdapter = IconAdapter(TaskJoyIcon.values(), selectedIcon) { icon ->
            selectedIcon = icon
        }

        binding.rvIcons.apply {
            layoutManager = GridLayoutManager(this@CreateStepActivity, 3)
            adapter = iconAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSaveStep.setOnClickListener { saveStep() }
    }

    private fun saveStep() {
        val name = binding.etStepName.text.toString()
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a step name", Toast.LENGTH_SHORT).show()
            return
        }

        if (stepId != null) {
            updateExistingStep(name)
        } else {
            createNewStep(name)
        }
    }

    private fun updateExistingStep(name: String) {
        val stepRef = db.collection("steps").document(stepId!!)

        val updates = mapOf(
            "name" to name,
            "image" to selectedIcon.name,
            "notes" to binding.etStepNotes.text.toString()
        )

        stepRef.update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Step updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating step: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createNewStep(name: String) {
        val stepRef = db.collection("steps").document()

        val step = Step(
            name = name,
            image = selectedIcon.name,
            notes = binding.etStepNotes.text.toString(),
            completed = false,
            id = stepRef.id
        )

        stepRef.set(step)
            .addOnSuccessListener {
                // After creating the step, update both the routine template and daily routine
                db.collection("endUser")
                    .document(userId)
                    .collection("dailyRoutines")
                    .document(routineId)
                    .get()
                    .addOnSuccessListener { routineDoc ->
                        val templateId = routineDoc.getString("templateId")
                        templateId?.let {
                            // Update routine template first
                            updateRoutineTemplate(it, stepRef.id)

                            // Now update the dailyRoutine by adding the new step
                            addStepToDailyRoutine(stepRef.id)
                        } ?: Log.w("ERROR", "Template ID not found.")
                    }
                    .addOnFailureListener { e ->
                        Log.w("ERROR", "Error retrieving dailyRoutine: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error creating step: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addStepToDailyRoutine(stepId: String) {
        db.collection("endUser")
            .document(userId)
            .collection("dailyRoutines")
            .document(routineId)
            .update("steps", FieldValue.arrayUnion(stepId))
            .addOnSuccessListener {
                Toast.makeText(this, "Step added to daily routine.", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Log.w("ERROR", "Error updating daily routine: ${e.message}")
                Toast.makeText(this, "Error updating daily routine: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateRoutineTemplate(templateId: String, stepId: String) {
        db.collection("routineTemplates")
            .document(templateId)
            .update("steps", FieldValue.arrayUnion(stepId))
            .addOnSuccessListener {
                Toast.makeText(this, "Step created and added successfully.", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Log.w("ERROR", "Error updating routineTemplate: ${e.message}")
            }
    }
}