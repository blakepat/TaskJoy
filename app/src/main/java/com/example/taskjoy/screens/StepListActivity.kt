package com.example.taskjoy.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskjoy.R
import com.example.taskjoy.databinding.StepListScreenBinding
import com.example.taskjoy.model.Routine
import com.example.taskjoy.model.Step
import com.example.taskjoy.adapters.StepAdapter
import com.example.taskjoy.adapters.StepClickListener
import com.example.taskjoy.model.DailyRoutine
import com.example.taskjoy.model.RoutineTemplate
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FieldPath

class StepListActivity : AppCompatActivity(), StepClickListener {

    private lateinit var binding: StepListScreenBinding
    private lateinit var stepAdapter: StepAdapter
    private var db = Firebase.firestore
    private var routineId: String = ""
    private var isEditMode = false
    private var stepList: MutableList<Step> = mutableListOf()
    private var isDaily: Boolean = false
    private var endUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = StepListScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve routineId, isDaily, and endUserId from the intent
        routineId = intent.getStringExtra("routineId").toString()
        isDaily = intent.getBooleanExtra("isDaily", false)
        endUserId = intent.getStringExtra("endUser") // Ensure this is not null or empty

        // Debugging the endUserId to make sure it is properly received
        Log.w("StepListActivity", "End User ID in StepListActivity: $endUserId")

        if (endUserId.isNullOrEmpty()) {
            Log.w("StepListActivity", "Error: endUserId is null or empty.")
            Snackbar.make(binding.root, "Error: Missing User ID", Snackbar.LENGTH_SHORT).show()
            finish() // Close the activity if missing
            return
        } else {

        }

        setupRecyclerView()

        binding.fabAddStep.setOnClickListener {
            val intent = Intent(this, CreateStepActivity::class.java).apply {
                putExtra("userId", endUserId)  // Pass the userId (endUserId)
                putExtra("routineId", routineId)  // Pass the routineId
            }
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.w("StepListActivity", "Fetching routine with ID: $routineId for User: $endUserId")
        getRoutineWithSteps(routineId)
    }

    private fun setupRecyclerView() {
        stepAdapter = StepAdapter(stepList, this)
        binding.recyclerViewSteps.apply {
            adapter = stepAdapter
            layoutManager = LinearLayoutManager(this@StepListActivity)
            addItemDecoration(DividerItemDecoration(this@StepListActivity, DividerItemDecoration.VERTICAL))
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
        }
        startActivity(intent)
    }

    override fun onEditClick(step: Step) {
        val intent = Intent(this, CreateStepActivity::class.java)
        intent.putExtra("routineId", routineId)
        intent.putExtra("stepId", step.id)
        startActivity(intent)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getRoutineWithSteps(routineId: String) {
        val collection = if (isDaily) {
            db.collection("endUser").document(endUserId!!).collection("dailyRoutines")
        } else {
            db.collection("routineTemplates")
        }

        collection.document(routineId)
            .get()
            .addOnSuccessListener { routineDoc: DocumentSnapshot ->
                Log.w("StepListActivity", "Routine document fetched: $routineDoc")
                val routine = if (isDaily) {
                    routineDoc.toObject(DailyRoutine::class.java)
                } else {
                    routineDoc.toObject(RoutineTemplate::class.java)
                }

                if (routine != null) {
                    Log.w("StepListActivity", "Routine fetched: $routine")
                } else {
                    Log.w("StepListActivity", "Routine is null!")
                }

                val routineName = if (isDaily) {
                    (routine as? DailyRoutine)?.name
                } else {
                    (routine as? RoutineTemplate)?.name
                }
                supportActionBar?.title = routineName ?: "Steps"

                val steps = if (isDaily) {
                    (routine as? DailyRoutine)?.steps
                } else {
                    (routine as? RoutineTemplate)?.steps
                }

                if (!steps.isNullOrEmpty()) {
                    stepList.clear()

                    db.collection("steps")
                        .whereIn(FieldPath.documentId(), steps)
                        .get()
                        .addOnSuccessListener { results: QuerySnapshot ->
                            Log.w("StepListActivity", "Steps fetched: ${results.size()} steps")
                            for (document: QueryDocumentSnapshot in results) {
                                val stepFromDB: Step = document.toObject(Step::class.java)
                                stepList.add(stepFromDB)
                            }
                            stepAdapter.notifyDataSetChanged()
                        }
                        .addOnFailureListener { error ->
                            Snackbar.make(binding.root, "Error getting steps $error", Snackbar.LENGTH_SHORT).show()
                            Log.e("StepListActivity", "Error getting steps", error)
                        }
                } else {
                    Log.w("StepListActivity", "No steps found in routine.")
                }
            }
            .addOnFailureListener { error ->
                Snackbar.make(binding.root, "Error getting routine $error", Snackbar.LENGTH_SHORT).show()
                Log.e("StepListActivity", "Error getting routine", error)
            }
    }
}