package com.example.taskjoy.screens

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.taskjoy.adapters.IconAdapter
import com.example.taskjoy.databinding.CreateRoutineScreenBinding
import com.example.taskjoy.model.TaskJoyIcon
import com.example.taskjoy.repository.RepositoryService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.Calendar

class CreateRoutineActivity : AppCompatActivity() {
    private lateinit var binding: CreateRoutineScreenBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var repositoryService: RepositoryService
    private var routineId: String? = null
    private var dailyRoutineId: String? = null
    private var endUserId: String? = null
    private var selectedIcon: TaskJoyIcon = TaskJoyIcon.MORNING
    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CreateRoutineScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        repositoryService = RepositoryService()

        // Get routineId, endUser and selectedDate from intent
        routineId = intent.getStringExtra("routineId")
        dailyRoutineId = intent.getStringExtra("dailyRoutineId")
        endUserId = intent.getStringExtra("endUser")
        intent.getLongExtra("selectedDate", -1).let { timestamp ->
            if (timestamp != -1L) {
                selectedDate.timeInMillis = timestamp
            }
        }

        Log.d("CreateRoutine", "Received IDs - RoutineId: $routineId, DailyRoutineId: $dailyRoutineId, EndUserId: $endUserId")

        if (endUserId == null) {
            Toast.makeText(this, "Error: No end user specified", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupIconRecyclerView()
        setupClickListeners()

        routineId?.let { loadRoutineData(it) }
    }

    private fun setupIconRecyclerView() {
        val iconAdapter = IconAdapter.createBasic(
            context = this,
            icons = TaskJoyIcon.values(),
            selectedIcon = selectedIcon
        ) { icon ->
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
        val currentUserId = auth.currentUser?.uid ?: return

        repositoryService.getRoutineTemplate(
            routineId = routineId,
            endUserId = endUserId ?: return,
            currentUserId = currentUserId,
            onSuccess = { routine ->
                binding.etRoutineName.setText(routine.name)
                selectedIcon = TaskJoyIcon.fromString(routine.image)
                binding.rvIcons.adapter?.notifyDataSetChanged()
            },
            onFailure = { error ->
                Log.e("CreateRoutine", "Error loading routine", error)
                Toast.makeText(this, "Error loading routine: ${error.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        )
    }

    private fun saveRoutine() {
        val name = binding.etRoutineName.text.toString()
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a routine name", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        // Add debug logs
        Log.d("CreateRoutine", "Current User ID: $currentUserId")
        Log.d("CreateRoutine", "EndUser ID: $endUserId")

        repositoryService.saveRoutine(
            routineId = routineId,
            dailyRoutineId = dailyRoutineId,
            endUserId = endUserId ?: return,
            name = name,
            icon = selectedIcon,
            currentUserId = currentUserId,
            selectedDate = selectedDate,
            onSuccess = {
                val message = if (dailyRoutineId != null) "Routine updated successfully" else "Routine saved successfully"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                finish()
            },
            onFailure = { error ->
                Log.e("CreateRoutine", "Error saving routine", error)
                Toast.makeText(this, "Error saving routine: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}