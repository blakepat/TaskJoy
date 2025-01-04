package com.example.taskjoy.screens

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.taskjoy.adapters.IconAdapter
import com.example.taskjoy.databinding.CreateRoutineScreenBinding
import com.example.taskjoy.model.Routine
import com.example.taskjoy.model.TaskJoyIcon
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

class CreateRoutineActivity : AppCompatActivity() {
    private lateinit var binding: CreateRoutineScreenBinding
    private lateinit var db: FirebaseFirestore
    private var routineId: String? = null
    private var selectedIcon: TaskJoyIcon = TaskJoyIcon.CUSTOM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CreateRoutineScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        routineId = intent.getStringExtra("routineId")

        setupIconRecyclerView()
        setupClickListeners()

        routineId?.let { loadRoutineData(it) }
    }

    private fun setupIconRecyclerView() {
        val iconAdapter = IconAdapter(TaskJoyIcon.values(), selectedIcon) { icon ->
            selectedIcon = icon
        }

        binding.rvIcons.apply {
            layoutManager = GridLayoutManager(this@CreateRoutineActivity, 3)
            adapter = iconAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSaveRoutine.setOnClickListener {
            saveRoutine()
        }
    }

    private fun loadRoutineData(routineId: String) {
        db.collection("routines").document(routineId)
            .get()
            .addOnSuccessListener { document ->
                document.toObject(Routine::class.java)?.let { routine ->
                    binding.etRoutineName.setText(routine.name)
                    selectedIcon = TaskJoyIcon.fromString(routine.image)
                    binding.rvIcons.adapter?.notifyDataSetChanged()
                }
            }
    }

    private fun saveRoutine() {
        val name = binding.etRoutineName.text.toString()
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a routine name", Toast.LENGTH_SHORT).show()
            return
        }

        val routine = Routine(
            name = name,
            date = Timestamp.now(),
            image = selectedIcon.name,
            steps = mutableListOf(),
            completed = false,
            id = routineId ?: ""
        )

        val routinesCollection = db.collection("routines")
        val routineDoc = if (routineId != null) {
            routinesCollection.document(routineId!!)
        } else {
            routinesCollection.document()
        }

        routineDoc.set(routine)
            .addOnSuccessListener {
                Toast.makeText(this, "Routine saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving routine: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}