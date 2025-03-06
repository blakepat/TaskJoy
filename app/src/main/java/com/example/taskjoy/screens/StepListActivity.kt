package com.example.taskjoy.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskjoy.R
import com.example.taskjoy.adapters.StepAdapter
import com.example.taskjoy.adapters.StepClickListener
import com.example.taskjoy.adapters.StepItemTouchHelperCallback
import com.example.taskjoy.databinding.StepListScreenBinding
import com.example.taskjoy.model.DailyRoutine
import com.example.taskjoy.model.Step
import com.example.taskjoy.repository.RepositoryService
import com.google.android.material.snackbar.Snackbar

class StepListActivity : AppCompatActivity(), StepClickListener {

    private lateinit var binding: StepListScreenBinding
    private lateinit var stepAdapter: StepAdapter
    private lateinit var repositoryService: RepositoryService
    private var routineId: String = ""
    private var isEditMode = false
    private val stepList: MutableList<Step> = mutableListOf()
    private var endUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = StepListScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repositoryService = RepositoryService()

        // Retrieve intent data
        routineId = intent.getStringExtra("routineId").toString()
        endUserId = intent.getStringExtra("endUser")

        if (endUserId.isNullOrEmpty()) {
            Log.e("StepListActivity", "Error: endUserId is missing")
            Snackbar.make(binding.root, "Error: Missing User ID", Snackbar.LENGTH_SHORT).show()
            finish()
            return
        }

        setupRecyclerView()
        setupFab()

        // Fetch routine and steps
        getRoutineWithSteps()
    }

    override fun onResume() {
        super.onResume()
        getRoutineWithSteps()
    }

    private fun setupRecyclerView() {
        stepAdapter = StepAdapter(stepList, this)
        binding.recyclerViewSteps.apply {
            adapter = stepAdapter
            layoutManager = LinearLayoutManager(this@StepListActivity)
        }

        // Setup ItemTouchHelper for drag and drop
        val callback = StepItemTouchHelperCallback(stepAdapter)
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(binding.recyclerViewSteps)
        stepAdapter.setItemTouchHelper(touchHelper)
    }

    private fun setupFab() {
        binding.fabAddStep.setOnClickListener {
            val intent = Intent(this, CreateStepActivity::class.java).apply {
                putExtra("userId", endUserId)
                putExtra("routineId", routineId)
            }
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.step_list_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_toggle_edit -> {
                if (isEditMode) {
                    // Exiting edit mode, save the order
                    saveStepOrder()
                }
                isEditMode = !isEditMode
                item.setIcon(if (isEditMode) R.drawable.ic_checkmark else R.drawable.ic_edit)
                stepAdapter.setEditMode(isEditMode)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveStepOrder() {
        repositoryService.saveStepOrder(
            endUserId = endUserId ?: return,
            routineId = routineId,
            steps = stepList,
            onSuccess = {
                Log.d("StepListActivity", "Successfully saved step order")
                Snackbar.make(binding.root, "Order saved successfully", Snackbar.LENGTH_SHORT).show()
            },
            onError = { error ->
                Log.e("StepListActivity", "Error saving step order", error)
                Snackbar.make(
                    binding.root,
                    "Error saving order: ${error.localizedMessage}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        )
    }

    override fun onStepClick(step: Step) {
        val currentPosition = stepList.indexOf(step)
        val stepIds = stepList.map { it.id }

        val intent = Intent(this, StepDetailsActivity::class.java).apply {
            putExtra("stepId", step.id)
            putExtra("currentPosition", currentPosition)
            putStringArrayListExtra("stepIds", ArrayList(stepIds))
            putExtra("endUser", endUserId)
            putExtra("routineId", routineId)
        }
        startActivity(intent)
    }

    override fun onEditClick(step: Step) {
        val intent = Intent(this, CreateStepActivity::class.java).apply {
            putExtra("userId", endUserId)
            putExtra("routineId", routineId)
            putExtra("stepId", step.id)
            putExtra("templateStepId", step.templateStepId) // Add templateStepId
        }
        startActivity(intent)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getRoutineWithSteps() {
        repositoryService.getRoutineWithSteps(
            endUserId = endUserId ?: return,
            routineId = routineId,
            onSuccess = { routine, steps ->
                Log.d("StepListActivity", "Routine and steps fetched successfully")

                // Update the title
                val routineName = routine.name
                supportActionBar?.title = routineName

                // Update the step list
                stepList.clear()
                stepList.addAll(steps)
                stepAdapter.notifyDataSetChanged()
            },
            onError = { error ->
                Log.e("StepListActivity", "Error getting routine with steps", error)
                Snackbar.make(
                    binding.root,
                    "Error loading steps: ${error.message}",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        )
    }

    override fun onStepOrderChanged(steps: List<Step>) {
        // This is called by the adapter when steps are reordered
        // We don't need to do anything here as the order will be saved when edit mode is exited
        Log.d("StepListActivity", "Step order changed - will be saved on edit mode exit")
    }

    override fun onDeleteClick(step: Step) {
        AlertDialog.Builder(this)
            .setTitle("Delete Step")
            .setMessage("Are you sure you want to delete this step?")
            .setPositiveButton("Delete") { _, _ ->
                deleteStep(step)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteStep(step: Step) {
        Log.d("StepListActivity", "Starting deletion for step: ${step.id}, templateStepId: ${step.templateStepId}")

        repositoryService.deleteStep(
            endUserId = endUserId ?: return,
            routineId = routineId,
            step = step,
            onSuccess = {
                stepList.remove(step)

                // Update remaining steps order
                repositoryService.updateRemainingStepsOrder(
                    endUserId = endUserId ?: return@deleteStep,
                    routineId = routineId,
                    steps = stepList,
                    onSuccess = {
                        stepAdapter.notifyDataSetChanged()
                        Snackbar.make(binding.root, "Step deleted successfully", Snackbar.LENGTH_SHORT).show()
                    },
                    onError = { error ->
                        Log.e("StepListActivity", "Error updating step order after deletion", error)
                        // Still notify data set changed even if ordering fails
                        stepAdapter.notifyDataSetChanged()
                    }
                )
            },
            onError = { error ->
                Log.e("StepListActivity", "Error deleting step", error)
                Snackbar.make(
                    binding.root,
                    "Error deleting step: ${error.localizedMessage}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        )
    }
}