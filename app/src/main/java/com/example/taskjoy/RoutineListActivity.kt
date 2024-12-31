package com.example.taskjoy


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
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FieldPath

class RoutineListActivity : AppCompatActivity(), RoutineClickListener {

    lateinit var binding: ActivityRoutineListBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var routineAdapter: RoutineAdapter
    private val routineList = mutableListOf<Routine>()
    private var db = Firebase.firestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoutineListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.setTitle("Routines")

        auth = Firebase.auth

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
        getEndUserRoutines(auth.currentUser!!.uid)


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




    private fun getEndUserRoutines(parentId: String) {
        // First get the parent document to access their children list
        Log.w("TESTING", "parentId: $parentId")
        db.collection("parents").document(parentId)
            .get()
            .addOnSuccessListener { parentDoc: DocumentSnapshot ->
                // Get the list of child (endUser) IDs
                val childrenIds = parentDoc.get("children") as? List<String>
                Log.w("TESTING", "childIDs: $childrenIds")
                if (!childrenIds.isNullOrEmpty()) {
                    // Query endUsers collection for documents with matching IDs
                    db.collection("endUser")
                        .whereIn(FieldPath.documentId(), childrenIds)
                        .get()
                        .addOnSuccessListener { endUserResults: QuerySnapshot ->
                            val allRoutineIds = mutableListOf<String>()
                            Log.w("TESTING", "routine: $allRoutineIds")
                            // Collect all routine IDs from each endUser
                            for (endUserDoc: QueryDocumentSnapshot in endUserResults) {
                                val routineIds = endUserDoc.get("routines") as? List<String>
                                if (!routineIds.isNullOrEmpty()) {
                                    allRoutineIds.addAll(routineIds)
                                }
                            }

                            if (allRoutineIds.isNotEmpty()) {
                                routineList.clear() // Clear existing routines

                                // Finally, query the routines collection
                                db.collection("routines")
                                    .whereIn(FieldPath.documentId(), allRoutineIds)
                                    .get()
                                    .addOnSuccessListener { routineResults: QuerySnapshot ->
                                        for (routineDoc: QueryDocumentSnapshot in routineResults) {
                                            val routineFromDB = routineDoc.toObject(Routine::class.java)
                                            routineList.add(routineFromDB)
                                            Log.w("TESTING", "Added routine: ${routineFromDB.id}")
                                        }
                                        routineAdapter.notifyDataSetChanged()
                                        Log.w("TESTING", "Total routines added: ${routineList.size}")
                                    }
                                    .addOnFailureListener { error ->
                                        Log.w("TESTING", "Error getting routines.", error)
                                        Snackbar.make(binding.root, "Error getting routines",
                                            Snackbar.LENGTH_SHORT).show()
                                    }
                            }
                        }
                        .addOnFailureListener { error ->
                            Log.w("TESTING", "Error getting endUsers.", error)
                            Snackbar.make(binding.root, "Error getting end users",
                                Snackbar.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { error ->
                Log.w("TESTING", "Error getting parent.", error)
                Snackbar.make(binding.root, "Error getting parent data",
                    Snackbar.LENGTH_SHORT).show()
            }
    }
}



