package com.example.taskjoy.screens.StepList

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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskjoy.R
import com.example.taskjoy.adapters.StepAdapter
import com.example.taskjoy.adapters.StepClickListener
import com.example.taskjoy.adapters.StepItemTouchHelperCallback
import com.example.taskjoy.databinding.StepListScreenBinding
import com.example.taskjoy.model.Step
import com.example.taskjoy.screens.StepDetails.StepDetailsActivity
import com.example.taskjoy.viewmodels.StepListViewModel
import com.google.android.material.snackbar.Snackbar

class StepListActivity : AppCompatActivity(), StepClickListener {

    private lateinit var binding: StepListScreenBinding
    private lateinit var stepAdapter: StepAdapter
    private val stepList: MutableList<Step> = mutableListOf()
    private var isEditMode = false
    private var routineId: String = ""
    private var endUserId: String? = null

    // Initialize the ViewModel using the by viewModels() delegate
    private val viewModel: StepListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = StepListScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        setupObservers()

        // Fetch routine and steps
        getRoutineWithSteps()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupObservers() {
        viewModel.routine.observe(this) { routine ->
            // Update activity title with routine name
            supportActionBar?.title = routine.name
        }

        viewModel.steps.observe(this) { steps ->
            stepList.clear()
            stepList.addAll(steps)
            stepAdapter.notifyDataSetChanged()
            updateEmptyState(steps.isEmpty())
        }

        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        viewModel.saveOrderSuccess.observe(this) { success ->
            if (success) {
                Snackbar.make(binding.root, "Order saved successfully", Snackbar.LENGTH_SHORT).show()
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            // Handle loading state if needed
        }
    }

    /**
     * Updates the visibility of empty state and recycler view based on whether steps list is empty
     */
    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.recyclerViewSteps.visibility = View.GONE
            binding.emptyStateView.visibility = View.VISIBLE
            setupEmptyStateAnimation()
        } else {
            binding.recyclerViewSteps.visibility = View.VISIBLE
            binding.emptyStateView.visibility = View.GONE
        }
    }

    private fun setupEmptyStateAnimation() {
        val image = binding.emptyStateImage
        val title = binding.emptyStateTitle
        val description = binding.emptyStateDescription

        // Initially set alpha to 0 (invisible)
        image.alpha = 0f
        title.alpha = 0f
        description.alpha = 0f

        // Create animation sequences
        image.animate()
            .alpha(1f)
            .setDuration(400)
            .withEndAction {
                // After image appears, animate the title
                title.animate()
                    .alpha(1f)
                    .setDuration(400)
                    .withEndAction {
                        // After title appears, animate the description
                        description.animate()
                            .alpha(1f)
                            .setDuration(400)
                            .start()
                    }
                    .start()
            }
            .start()

        // Add a subtle pulse animation to draw attention to the FAB
        binding.fabAddStep.apply {
            animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(600)
                .withEndAction {
                    animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(600)
                        .start()
                }
                .start()
        }
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
        // Set up FAB button click
        binding.fabAddStep.setOnClickListener {
            navigateToCreateStep()
        }

        // Set up empty state button click
        binding.emptyStateButton.setOnClickListener {
            navigateToCreateStep()
        }
    }

    private fun navigateToCreateStep() {
        val intent = Intent(this, CreateStepActivity::class.java).apply {
            putExtra("userId", endUserId)
            putExtra("routineId", routineId)
        }
        startActivity(intent)
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
        viewModel.saveStepOrder(
            endUserId = endUserId ?: return,
            routineId = routineId,
            steps = stepList
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

    private fun getRoutineWithSteps() {
        viewModel.getRoutineWithSteps(
            endUserId = endUserId ?: return,
            routineId = routineId
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
        viewModel.deleteStep(
            endUserId = endUserId ?: return,
            routineId = routineId,
            step = step
        )

        // After deletion, update the order of remaining steps
        viewModel.updateRemainingStepsOrder(
            endUserId = endUserId ?: return,
            routineId = routineId,
            steps = stepList.filter { it.id != step.id }
        )
    }
}