package com.example.taskjoy.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskjoy.R
import com.example.taskjoy.adapters.StepAdapter
import com.example.taskjoy.adapters.StepClickListener
import com.example.taskjoy.databinding.StepListScreenBinding
import com.example.taskjoy.model.DailyRoutine
import com.example.taskjoy.model.Step
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class StepListActivity : AppCompatActivity(), StepClickListener {

    private lateinit var binding: StepListScreenBinding
    private lateinit var stepAdapter: StepAdapter
    private val db = Firebase.firestore
    private var routineId: String = ""
    private var isEditMode = false
    private var stepList: MutableList<Step> = mutableListOf()
    private var endUserId: String? = null

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

        // Fetch routine and steps
        getRoutineWithSteps(routineId)
    }

    override fun onResume() {
        super.onResume()
        getRoutineWithSteps(routineId)
    }

    private fun setupRecyclerView() {
        stepAdapter = StepAdapter(stepList, this)
        binding.recyclerViewSteps.apply {
            adapter = stepAdapter
            layoutManager = LinearLayoutManager(this@StepListActivity)
        }
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
                isEditMode = !isEditMode
                item.setIcon(if (isEditMode) R.drawable.ic_checkmark else R.drawable.ic_edit)
                stepAdapter.setEditMode(isEditMode)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
        }
        startActivity(intent)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getRoutineWithSteps(routineId: String) {
        db.collection("endUser").document(endUserId!!).collection("dailyRoutines")
            .document(routineId)
            .get()
            .addOnSuccessListener { routineDoc ->
                Log.d("StepListActivity", "Routine document fetched: ${routineDoc.data}")
                val routine = routineDoc.toObject(DailyRoutine::class.java)
                val routineName = routine?.name ?: "Unknown Routine"

                // Set the title of the action bar
                supportActionBar?.title = routineName

                // Fetch steps from the dailySteps subcollection
                db.collection("endUser").document(endUserId!!).collection("dailyRoutines")
                    .document(routineId).collection("dailySteps")
                    .get()
                    .addOnSuccessListener { stepDocs ->
                        Log.d("StepListActivity", "Steps fetched: ${stepDocs.size()} steps")
                        stepList.clear()
                        for (stepDoc in stepDocs) {
                            val step = stepDoc.toObject(Step::class.java)
                            stepList.add(step)
                        }
                        stepAdapter.notifyDataSetChanged()
                    }
                    .addOnFailureListener { error ->
                        Snackbar.make(binding.root, "Error getting steps: $error", Snackbar.LENGTH_SHORT).show()
                        Log.e("StepListActivity", "Error getting steps", error)
                    }
            }
            .addOnFailureListener { error ->
                Snackbar.make(binding.root, "Error getting routine: $error", Snackbar.LENGTH_SHORT).show()
                Log.e("StepListActivity", "Error getting routine", error)
            }
    }
}