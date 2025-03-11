package com.example.taskjoy.screens.RoutineList

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskjoy.R
import com.example.taskjoy.adapters.RoutineAdapter
import com.example.taskjoy.adapters.RoutineClickListener
import com.example.taskjoy.databinding.ActivityRoutineListBinding
import com.example.taskjoy.model.DailyRoutine
import com.example.taskjoy.screens.StepList.StepListActivity
import com.example.taskjoy.viewmodels.RoutineListViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RoutineListActivity : AppCompatActivity(), RoutineClickListener {
    private lateinit var binding: ActivityRoutineListBinding
    private lateinit var routineAdapter: RoutineAdapter
    private val routineList = mutableListOf<DailyRoutine>()
    private var isEditMode = false
    private lateinit var auth: FirebaseAuth
    private var endUserId: String? = null
    private var selectedDate: Calendar = Calendar.getInstance()


    private val viewModel: RoutineListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoutineListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setTitle("Routines")

        auth = Firebase.auth

        endUserId = intent.getStringExtra("endUser")

        intent.getLongExtra("selectedDate", -1).let { timestamp ->
            if (timestamp != -1L) {
                selectedDate.timeInMillis = timestamp
            }
        }

        setupRecyclerView()
        setupClickListeners()
        setupDateDisplay()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.routines.observe(this) { routines ->
            routineList.clear()
            routineList.addAll(routines)
            updateUIBasedOnRoutines()
        }


        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun setupDateDisplay() {
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        binding.textViewDate.text = dateFormat.format(selectedDate.time)
    }

    override fun onResume() {
        super.onResume()
        loadRoutines()
    }

    private fun loadRoutines() {
        endUserId?.let { id ->
            // Check and create routines if needed
            viewModel.createDailyRoutinesIfNeeded(id, selectedDate)
        } ?: run {
            // If no endUserId, get routines for all children of the current user
            auth.currentUser?.uid?.let { parentId ->
                viewModel.getAllEndUserDailyRoutines(parentId, selectedDate)
            } ?: run {
                Snackbar.make(binding.root, "Error: User not authenticated", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateUIBasedOnRoutines() {
        if (routineList.isEmpty()) {
            binding.recyclerViewRoutines.visibility = View.GONE
            binding.emptyState.root.visibility = View.VISIBLE
        } else {
            binding.recyclerViewRoutines.visibility = View.VISIBLE
            binding.emptyState.root.visibility = View.GONE
        }
        routineAdapter.notifyDataSetChanged()
    }

    private fun setupRecyclerView() {
        routineAdapter = RoutineAdapter(routineList, this, this)
        binding.recyclerViewRoutines.apply {
            layoutManager = LinearLayoutManager(this@RoutineListActivity)
            adapter = routineAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAddRoutine.setOnClickListener {
            val intent = Intent(this, CreateRoutineActivity::class.java).apply {
                putExtra("endUser", endUserId)
            }
            startActivity(intent)
        }
    }

    override fun onRoutineClick(routine: DailyRoutine) {
        val intent = Intent(this, StepListActivity::class.java).apply {
            Log.w("TESTING", "ROUTINE CLICK ON ROUTINE LIST SCREEN, ID: ${routine.id}")
            putExtra("routineId", routine.id)
            putExtra("isDaily", true)
            putExtra("endUser", endUserId)
        }
        startActivity(intent)
    }

    override fun onEditClick(routine: DailyRoutine) {
        val intent = Intent(this, CreateRoutineActivity::class.java).apply {
            putExtra("routineId", routine.templateId)
            putExtra("dailyRoutineId", routine.id)  // Add the daily routine ID
            putExtra("endUser", endUserId)
            putExtra("selectedDate", selectedDate.timeInMillis)  // Pass the selected date
        }
        startActivity(intent)
    }

    override fun onDeleteClick(routine: DailyRoutine) {
        // Show confirmation dialog before deletion
        AlertDialog.Builder(this)
            .setTitle("Delete Routine")
            .setMessage("Are you sure you want to delete this routine?")
            .setPositiveButton("Delete") { _, _ ->
                deleteRoutine(routine)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteRoutine(routine: DailyRoutine) {
        val userId = endUserId ?: return
        viewModel.deleteRoutine(userId, routine)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.routine_list_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_toggle_edit -> {
                isEditMode = !isEditMode
                item.setIcon(if (isEditMode) R.drawable.ic_checkmark else R.drawable.ic_edit)
                routineAdapter.setEditMode(isEditMode)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}