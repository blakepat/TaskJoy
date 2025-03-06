package com.example.taskjoy.screens

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskjoy.R
import com.example.taskjoy.adapters.RoutineAdapter
import com.example.taskjoy.adapters.RoutineClickListener
import com.example.taskjoy.databinding.ActivityRoutineListBinding
import com.example.taskjoy.model.DailyRoutine
import com.example.taskjoy.repository.RepositoryService
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
    private lateinit var repositoryService: RepositoryService
    private var endUserId: String? = null
    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoutineListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setTitle("Routines")

        auth = Firebase.auth
        repositoryService = RepositoryService()

        // Get endUserId from intent if available
        endUserId = intent.getStringExtra("endUser")

        // Get selected date from intent if it exists
        intent.getLongExtra("selectedDate", -1).let { timestamp ->
            if (timestamp != -1L) {
                selectedDate.timeInMillis = timestamp
            }
        }

        setupRecyclerView()
        setupClickListeners()
        setupDateDisplay()
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
            // Check and create daily routines if needed, then get them
            repositoryService.createDailyRoutinesIfNeeded(
                endUserId = id,
                date = selectedDate,
                onSuccess = {
                    fetchRoutines(id)
                },
                onError = { error ->
                    Log.e("RoutineList", "Error checking/creating routines", error)
                    Snackbar.make(binding.root, "Error loading routines: ${error.message}", Snackbar.LENGTH_SHORT).show()
                }
            )
        } ?: run {
            // If no endUserId, get routines for all children of the current user
            auth.currentUser?.uid?.let { parentId ->
                repositoryService.getAllEndUserDailyRoutines(
                    parentId = parentId,
                    date = selectedDate,
                    onSuccess = { allRoutines ->
                        routineList.clear()
                        routineList.addAll(allRoutines)
                        updateUIBasedOnRoutines()
                    },
                    onError = { error ->
                        Log.e("RoutineList", "Error getting routines for all children", error)
                        Snackbar.make(binding.root, "Error loading routines: ${error.message}", Snackbar.LENGTH_SHORT).show()
                    }
                )
            } ?: run {
                Snackbar.make(binding.root, "Error: User not authenticated", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchRoutines(id: String) {
        repositoryService.getDailyRoutines(
            endUserId = id,
            date = selectedDate,
            onSuccess = { routines ->
                routineList.clear()
                routineList.addAll(routines)
                updateUIBasedOnRoutines()
            },
            onError = { error ->
                Log.e("RoutineList", "Error getting routines", error)
                Snackbar.make(binding.root, "Error getting routines: ${error.message}", Snackbar.LENGTH_SHORT).show()
            }
        )
    }

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

        repositoryService.deleteRoutine(
            endUserId = userId,
            routine = routine,
            onSuccess = {
                routineList.remove(routine)
                updateUIBasedOnRoutines()
                Snackbar.make(binding.root, "Routine deleted successfully", Snackbar.LENGTH_SHORT).show()
            },
            onError = { error ->
                Log.e("RoutineList", "Error deleting routine", error)
                Snackbar.make(
                    binding.root,
                    "Error deleting routine: ${error.localizedMessage}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        )
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