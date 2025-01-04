package com.example.taskjoy.screens


import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
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

    lateinit var binding: StepListScreenBinding
    private lateinit var stepAdapter: StepAdapter
    private var db = Firebase.firestore
    private var routineId: String = ""

    private var stepList: MutableList<Step> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = StepListScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Routine ID used for getting step list from Firebase
        routineId = intent.getStringExtra("routineId").toString()

        //Setup Recycler view
        stepAdapter = StepAdapter(stepList, this)
        binding.recyclerViewSteps.adapter = stepAdapter
        binding.recyclerViewSteps.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewSteps.addItemDecoration(
            DividerItemDecoration(
                this,
                LinearLayoutManager.VERTICAL
            )
        )


        getRoutineWithSteps(routineId ?: "")


        binding.fabAddStep.setOnClickListener {
            val intent = Intent(this, CreateStepActivity::class.java)
            intent.putExtra("routineId", routineId) // Required
            startActivity(intent)
        }
    }








    // Click listener methods
//    override fun onStepClick(step: Step) {
//        val intent = Intent(this, StepDetailsActivity::class.java).apply {
//            putExtra("stepId", step.id)
//        }
//        startActivity(intent)
//    }
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

    //get routine from firebase with id passed. THEN get steps stored inside using stored ids
    @SuppressLint("NotifyDataSetChanged")
    private fun getRoutineWithSteps(routineId: String) {
        db.collection("routines").document(routineId)
            .get()
            .addOnSuccessListener { routineDoc: DocumentSnapshot ->
                val routineFromDB: Routine = routineDoc.toObject(Routine::class.java)!!

                supportActionBar!!.setTitle(routineFromDB.name)

                if (routineFromDB.steps.isNotEmpty()) {
                    stepList.clear() // Clear existing steps

                    // Query steps collection
                    db.collection("steps")
                        .whereIn(FieldPath.documentId(), routineFromDB.steps) // Use documentId() instead of "id"
                        .get()
                        .addOnSuccessListener { results: QuerySnapshot ->
                            for (document: QueryDocumentSnapshot in results) {
                                val stepFromDB: Step = document.toObject(Step::class.java)
                                stepList.add(stepFromDB)
                            }

                            if (stepList.isEmpty()) {
                                //TODO: CREATE AN EMPTY LIST VIEW
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
