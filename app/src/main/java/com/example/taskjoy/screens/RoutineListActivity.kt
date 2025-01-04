package com.example.taskjoy.screens


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taskjoy.R
import com.example.taskjoy.databinding.ActivityRoutineListBinding
import com.example.taskjoy.adapters.RoutineAdapter
import com.example.taskjoy.adapters.RoutineClickListener
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

        val endUserId = intent.getStringExtra("endUser")
        auth = Firebase.auth

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerViewRoutines)
        recyclerView.layoutManager = LinearLayoutManager(this)
        // Set up the adapter
        routineAdapter = RoutineAdapter(routineList, this, this) // Pass the task list, context, and listener
        recyclerView.adapter = routineAdapter

        //Get Routines
        if (endUserId != null) {
            getSpecificEndUserRoutines(endUserId)
        } else {
            //Get routines from firebase db
            getAllEndUserRoutines(auth.currentUser!!.uid)
        }


        binding.fabAddRoutine.setOnClickListener {
            val intent = Intent(this, CreateRoutineActivity::class.java)
            startActivity(intent)
        }
    }



    // Click listener methods
    override fun onRoutineClick(routine: Routine) {
        val intent = Intent(this, StepListActivity::class.java)
        intent.putExtra("routineId", routine.id)
        startActivity(intent)
    }

    override fun onEditClick(routine: Routine) {
        val intent = Intent(this, CreateRoutineActivity::class.java)
        intent.putExtra("routineId", routine.id)
        startActivity(intent)
    }



    private fun getSpecificEndUserRoutines(endUserId: String) {
        db.collection("endUser").document(endUserId)
            .get()
            .addOnSuccessListener { endUserDoc: DocumentSnapshot ->
                val routineIds = endUserDoc.get("routines") as? List<String>
                Log.w("TESTING", "routine IDs: $routineIds")

                if (!routineIds.isNullOrEmpty()) {
                    routineList.clear() // Clear existing routines

                    db.collection("routines")
                        .whereIn(FieldPath.documentId(), routineIds)
                        .get()
                        .addOnSuccessListener { routineResults: QuerySnapshot ->
                            for (routineDoc: QueryDocumentSnapshot in routineResults) {
                                val routineFromDB = routineDoc.toObject(Routine::class.java)
                                routineList.add(routineFromDB)
                                Log.w("TESTING", "Added routine: ${routineFromDB.id}")
                            }

                            if (routineList.isEmpty()) {
                                //TODO: CREATE AN EMPTY LIST VIEW
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
                Log.w("TESTING", "Error getting end user.", error)
                Snackbar.make(binding.root, "Error getting end user data",
                    Snackbar.LENGTH_SHORT).show()
            }
    }



    private fun getAllEndUserRoutines(parentId: String) {
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
                                } else {
                                    //TODO: CREATE AN EMPTY LIST VIEW
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

                                        if (routineList.isEmpty()) {
                                            //TODO: CREATE AN EMPTY LIST VIEW
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



