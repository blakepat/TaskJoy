package com.example.taskjoy

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taskjoy.databinding.ActivityRoutineListBinding
import com.example.taskjoy.model.RoutineAdapter
import com.example.taskjoy.model.RoutineClickListener
import com.example.taskjoy.model.Routine
import com.example.taskjoy.model.TaskJoyIcon
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RoutineListActivity : AppCompatActivity(), RoutineClickListener {

    lateinit var binding: ActivityRoutineListBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var routineAdapter: RoutineAdapter
    private val routineList = mutableListOf<Routine>()
    private var db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoutineListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.setTitle("Routines")

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerViewRoutines)
        recyclerView.layoutManager = LinearLayoutManager(this)
        // Set up the adapter
        routineAdapter = RoutineAdapter(routineList, this, this) // Pass the task list, context, and listener
        recyclerView.adapter = routineAdapter
        // Prepare the task list
//        routineList.addAll(
//            listOf(
//                Routine("Morning Routine", image = 5, steps = TaskJoyIcon.MORNING),
//                Routine("School Preparation", image = 3, steps = TaskJoyIcon.SCHOOL),
//                Routine("Lunch Prep", image = 4, steps = TaskJoyIcon.LUNCH),
//                Routine("Bedtime Routine", image = 2, steps = TaskJoyIcon.BEDTIME),
//                Routine("Custom Task", image = 1, steps = TaskJoyIcon.CUSTOM)
//            )
//        )


        //Get routines from firebase db
        getRoutines()


    }

    //TODO: show create/edit routine screen using plus icon



    // Click listener methods
    override fun onRoutineClick(routine: Routine) {
        val intent = Intent(this, StepListActivity::class.java)
        //PASS ROUTINE ID TO NEXT SCREEN
        intent.putExtra("routineId", routine.id)
        startActivity(intent)
    }

    override fun onEditClick(routine: Routine) {
        //TODO: Show create/edit screen to edit selected routine

        Toast.makeText(this, "Edit Step: ${routine.name}", Toast.LENGTH_SHORT).show()
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun getRoutines() {
        db.collection("routines")
            .get()
            .addOnSuccessListener { results: QuerySnapshot ->
                routineList.clear()

                for (document:QueryDocumentSnapshot in results) {
                    val routineFromDB: Routine = document.toObject(Routine::class.java)
                    routineList.add(routineFromDB)
                }
                routineAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { error ->
                Log.w("TESTING", "Error getting documents.", error)
                Snackbar.make(binding.root, "Error getting routines", Snackbar.LENGTH_SHORT).show()
            }
    }
}



