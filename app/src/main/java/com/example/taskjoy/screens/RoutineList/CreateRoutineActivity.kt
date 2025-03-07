package com.example.taskjoy.screens.RoutineList

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.taskjoy.adapters.IconAdapter
import com.example.taskjoy.databinding.CreateRoutineScreenBinding
import com.example.taskjoy.model.TaskJoyIcon
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.Calendar

class CreateRoutineActivity : AppCompatActivity() {
    private lateinit var binding: CreateRoutineScreenBinding
    private lateinit var auth: FirebaseAuth
    private var routineId: String? = null
    private var dailyRoutineId: String? = null
    private var endUserId: String? = null
    private var selectedIcon: TaskJoyIcon = TaskJoyIcon.MORNING
    private var selectedDate: Calendar = Calendar.getInstance()

    // Initialize the ViewModel using the by viewModels() delegate
    private val viewModel: CreateRoutineViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CreateRoutineScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

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
        setupObservers()

        routineId?.let { loadRoutineData(it) }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupObservers() {
        viewModel.routineTemplate.observe(this) { template ->
            binding.etRoutineName.setText(template.name)
            selectedIcon = TaskJoyIcon.fromString(template.image)
            binding.rvIcons.adapter?.notifyDataSetChanged()
        }

        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
                if (it.contains("permission") || it.contains("access")) {
                    finish()
                }
            }
        }

        viewModel.saveSuccess.observe(this) { success ->
            if (success) {
                val message = if (dailyRoutineId != null) "Routine updated successfully" else "Routine saved successfully"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
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

        viewModel.getRoutineTemplate(
            routineId = routineId,
            endUserId = endUserId ?: return,
            currentUserId = currentUserId
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

        // Check permissions first
        viewModel.checkParentPermission(endUserId ?: return, currentUserId).observe(this) { hasPermission ->
            if (hasPermission) {
                viewModel.saveRoutine(
                    routineId = routineId,
                    dailyRoutineId = dailyRoutineId,
                    endUserId = endUserId ?: return@observe,
                    name = name,
                    icon = selectedIcon,
                    currentUserId = currentUserId,
                    selectedDate = selectedDate
                )
            } else {
                Toast.makeText(this, "Permission denied: Only parents can create/edit routines", Toast.LENGTH_SHORT).show()
            }
        }
    }
}