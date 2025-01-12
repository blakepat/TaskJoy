package com.example.taskjoy.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskjoy.R
import com.example.taskjoy.adapters.StepAdapter
import com.example.taskjoy.adapters.StepClickListener
import com.example.taskjoy.databinding.StepListScreenBinding
import com.example.taskjoy.model.DailyRoutine
import com.example.taskjoy.model.RoutineTemplate
import com.example.taskjoy.model.Step
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestoreException
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


    override fun onDeleteClick(step: Step) {
        // Show confirmation dialog before deletion
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

        // Define document references
        val dailyStepRef = db.collection("endUser")
            .document(endUserId!!)
            .collection("dailyRoutines")
            .document(routineId)
            .collection("dailySteps")
            .document(step.id)

        val dailyRoutineRef = db.collection("endUser")
            .document(endUserId!!)
            .collection("dailyRoutines")
            .document(routineId)

        val templateStepId = step.templateStepId

        if (templateStepId.isEmpty()) {
            // If no template step ID, just delete the daily step
            dailyStepRef.delete()
                .addOnSuccessListener {
                    stepList.remove(step)
                    stepAdapter.notifyDataSetChanged()
                    Snackbar.make(binding.root, "Step deleted successfully", Snackbar.LENGTH_SHORT).show()
                }
                .addOnFailureListener { error ->
                    Log.e("StepListActivity", "Error deleting step", error)
                    Snackbar.make(binding.root, "Error deleting step: ${error.localizedMessage}", Snackbar.LENGTH_LONG).show()
                }
            return
        }

        db.runTransaction { transaction ->
            // 1. READ the daily routine document to get the template ID
            val dailyRoutineDoc = transaction.get(dailyRoutineRef)
            if (!dailyRoutineDoc.exists()) {
                throw FirebaseFirestoreException(
                    "Daily routine document not found",
                    FirebaseFirestoreException.Code.NOT_FOUND
                )
            }

            val dailyRoutine = dailyRoutineDoc.toObject(DailyRoutine::class.java)
            val templateId = dailyRoutine?.templateId

            if (templateId.isNullOrEmpty()) {
                throw FirebaseFirestoreException(
                    "Template ID not found in daily routine",
                    FirebaseFirestoreException.Code.NOT_FOUND
                )
            }

            Log.d("StepListActivity", "Found templateId: $templateId")

            // 2. READ the template document using the correct template ID
            val templateRef = db.collection("routineTemplates").document(templateId)
            val templateDoc = transaction.get(templateRef)

            if (!templateDoc.exists()) {
                throw FirebaseFirestoreException(
                    "Template document not found",
                    FirebaseFirestoreException.Code.NOT_FOUND
                )
            }

            // 3. Get and update the steps array
            val template = templateDoc.toObject(RoutineTemplate::class.java)
            Log.d("StepListActivity", "Current template steps: ${template?.steps}")

            val updatedSteps = template?.steps?.toMutableList() ?: mutableListOf()
            updatedSteps.remove(templateStepId)
            Log.d("StepListActivity", "Updated steps array: $updatedSteps")

            // 4. Define step template reference
            val stepTemplateRef = db.collection("steps").document(templateStepId)

            // 5. Perform all writes
            transaction.update(templateRef, "steps", updatedSteps)
            transaction.delete(dailyStepRef)
            transaction.delete(stepTemplateRef)
        }.addOnSuccessListener {
            Log.d("StepListActivity", "Transaction completed successfully")
            stepList.remove(step)
            stepAdapter.notifyDataSetChanged()
            Snackbar.make(binding.root, "Step deleted successfully", Snackbar.LENGTH_SHORT).show()
        }.addOnFailureListener { error ->
            Log.e("StepListActivity", "Error in deletion transaction", error)
            Snackbar.make(
                binding.root,
                "Error deleting step: ${error.localizedMessage}",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }
}