package com.example.taskjoy


import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskjoy.databinding.StepListScreenBinding
import com.example.taskjoy.model.Routine
import com.example.taskjoy.model.Step
import com.example.taskjoy.model.StepAdapter
import com.example.taskjoy.model.StepClickListener
import com.example.taskjoy.model.TaskJoyIcon
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FieldPath

class StepListActivity : AppCompatActivity(), StepClickListener {

    lateinit var binding: StepListScreenBinding
    private lateinit var stepAdapter: StepAdapter
    private var db = Firebase.firestore

    private var stepList: MutableList<Step> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = StepListScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Routine ID used for getting step list from Firebase
        val routineId = intent.getStringExtra("routineId")
        //TODO: Change title to routine name
        supportActionBar!!.setTitle("Step List")

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
    }



    //TODO: show create/edit step screen using plus icon




    // Click listener methods
    override fun onStepClick(step: Step) {
        val intent = Intent(this, StepDetailsActivity::class.java).apply {
            putExtra("stepId", step.id)
        }
        startActivity(intent)
    }

    override fun onEditClick(step: Step) {
        //TODO: Show create/edit screen to edit selected step


        Toast.makeText(this, "Edit Step: ${step.name}", Toast.LENGTH_SHORT).show()
    }

    //get routine from firebase with id passed. THEN get steps stored inside using stored ids
    private fun getRoutineWithSteps(routineId: String) {
        db.collection("routines").document(routineId)
            .get()
            .addOnSuccessListener { routineDoc: DocumentSnapshot ->
                val routineFromDB: Routine = routineDoc.toObject(Routine::class.java)!!

                Log.w("TESTING", "Steps in routine: ${routineFromDB.steps}")

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
                                Log.w("TESTING", "Added step: ${stepFromDB.id}")
                            }
                            stepAdapter.notifyDataSetChanged()
                            Log.w("TESTING", "Total steps added: ${stepList.size}")
                        }
                        .addOnFailureListener { error ->
                            Log.w("TESTING", "Error getting steps.", error)
                            Snackbar.make(binding.root, "Error getting steps", Snackbar.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { error ->
                Log.w("TESTING", "Error getting routine.", error)
                Snackbar.make(binding.root, "Error getting routine", Snackbar.LENGTH_SHORT).show()
            }
    }

}
