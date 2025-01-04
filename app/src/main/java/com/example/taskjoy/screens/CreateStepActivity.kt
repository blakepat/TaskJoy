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
    private var routineId: String = ""
    private var stepId: String? = null
    private var selectedIcon: TaskJoyIcon = TaskJoyIcon.LUNCH

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CreateStepScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        routineId = intent.getStringExtra("routineId") ?: run {
            Toast.makeText(this, "Error: Routine ID not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        stepId = intent.getStringExtra("stepId")

        setupIconRecyclerView()
        setupClickListeners()

        stepId?.let { loadStepData(it) }
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
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSaveStep.setOnClickListener {
            saveStep()
        }
    }

    private fun loadStepData(stepId: String) {
        Log.w("TESTING", "Starting loadStepData with stepId: $stepId and routineId: $routineId")

        // Get step directly from steps collection
        db.collection("steps")
            .document(stepId)
            .get()
            .addOnSuccessListener { stepDoc ->
                Log.w("TESTING", "Raw step data: ${stepDoc.data}")

                if (stepDoc.exists()) {
                    Log.w("TESTING", "Step document exists, loading data")
                    val step = stepDoc.toObject(Step::class.java)
                    step?.let {
                        Log.w("TESTING", "Setting UI with - Name: ${it.name}, Notes: ${it.notes}, Image: ${it.image}")
                        binding.etStepName.setText(it.name)
                        binding.etStepNotes.setText(it.notes)
                        selectedIcon = TaskJoyIcon.fromString(it.image)
                        binding.rvIcons.adapter?.notifyDataSetChanged()
                    }
                }
            }
            .addOnFailureListener { error ->
                Log.w("TESTING", "Error loading step: $error")
            }
    }

    private fun saveStep() {
        val name = binding.etStepName.text.toString()
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a step name", Toast.LENGTH_SHORT).show()
            return
        }

        // If editing an existing step
        if (stepId != null) {
            updateExistingStep(stepId!!, name)
        } else {
            createNewStep(name)
        }
    }

    private fun updateExistingStep(stepId: String, name: String) {
        val step = Step(
            name = name,
            image = selectedIcon.name,
            notes = binding.etStepNotes.text.toString(),
            completed = false,
            id = stepId
        )

        db.collection("steps")
            .document(stepId)
            .set(step)
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
                // After creating the step, update the routine's steps array
                val routineRef = db.collection("routines").document(routineId)
                routineRef.update("steps", FieldValue.arrayUnion(stepRef.id))
                    .addOnSuccessListener {
                        Toast.makeText(this, "Step created successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error updating routine: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error creating step: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}