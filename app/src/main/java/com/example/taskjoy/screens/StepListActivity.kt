package com.example.taskjoy.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = StepListScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        routineId = intent.getStringExtra("routineId").toString()

        setupRecyclerView()


        binding.fabAddStep.setOnClickListener {
            val intent = Intent(this, CreateStepActivity::class.java)
            intent.putExtra("routineId", routineId)
            startActivity(intent)
        }
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
        db.collection("routines").document(routineId)
            .get()
            .addOnSuccessListener { routineDoc: DocumentSnapshot ->
                val routineFromDB: Routine = routineDoc.toObject(Routine::class.java)!!
                supportActionBar?.title = routineFromDB.name

                if (routineFromDB.steps.isNotEmpty()) {
                    stepList.clear()

                    db.collection("steps")
                        .whereIn(FieldPath.documentId(), routineFromDB.steps)
                        .get()
                        .addOnSuccessListener { results: QuerySnapshot ->
                            for (document: QueryDocumentSnapshot in results) {
                                val stepFromDB: Step = document.toObject(Step::class.java)
                                stepList.add(stepFromDB)
                            }
                            stepAdapter.notifyDataSetChanged()
                        }
                        .addOnFailureListener { error ->
                            Snackbar.make(binding.root, "Error getting steps $error", Snackbar.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { error ->
                Snackbar.make(binding.root, "Error getting routine $error", Snackbar.LENGTH_SHORT).show()
            }
    }
}