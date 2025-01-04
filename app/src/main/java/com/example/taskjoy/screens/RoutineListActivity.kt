package com.example.taskjoy.screens

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
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

    private lateinit var binding: ActivityRoutineListBinding
    private lateinit var routineAdapter: RoutineAdapter
    private val routineList = mutableListOf<Routine>()
    private var isEditMode = false
    private val db = Firebase.firestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoutineListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setTitle("Routines")  // Changed !! to ?

        val endUserId = intent.getStringExtra("endUser")
        auth = Firebase.auth

        setupRecyclerView()
        setupClickListeners()

        // Get Routines
        if (endUserId != null) {
            getSpecificEndUserRoutines(endUserId)
        } else {
            auth.currentUser?.uid?.let { uid ->  // Safe call added
                getAllEndUserRoutines(uid)
            } ?: run {
                Snackbar.make(binding.root, "Error: User not authenticated", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        routineAdapter = RoutineAdapter(routineList, this, this)
        binding.recyclerViewRoutines.apply {
            layoutManager = LinearLayoutManager(this@RoutineListActivity)
            adapter = routineAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAddRoutine.setOnClickListener {
            val intent = Intent(this, CreateRoutineActivity::class.java)
            startActivity(intent)
        }
    }

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.routine_list_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_toggle_edit -> {
                isEditMode = !isEditMode
                item.setIcon(if (isEditMode) R.drawable.ic_checkmark else R.drawable.ic_edit)
                routineAdapter.setEditMode(isEditMode)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getSpecificEndUserRoutines(endUserId: String) {
        db.collection("endUser").document(endUserId)
            .get()
            .addOnSuccessListener { endUserDoc: DocumentSnapshot ->
                val routineIds = endUserDoc.get("routines") as? List<String>

                if (!routineIds.isNullOrEmpty()) {
                    routineList.clear()

                    db.collection("routines")
                        .whereIn(FieldPath.documentId(), routineIds)
                        .get()
                        .addOnSuccessListener { routineResults: QuerySnapshot ->
                            for (routineDoc: QueryDocumentSnapshot in routineResults) {
                                routineDoc.toObject(Routine::class.java).let { routine ->
                                    routine.id = routineDoc.id  // Ensure ID is set
                                    routineList.add(routine)
                                }
                            }
                            routineAdapter.notifyDataSetChanged()
                        }
                        .addOnFailureListener { error ->
                            Snackbar.make(binding.root, "Error getting routines: ${error.message}",
                                Snackbar.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { error ->
                Snackbar.make(binding.root, "Error getting end user data: ${error.message}",
                    Snackbar.LENGTH_SHORT).show()
            }
    }

    private fun getAllEndUserRoutines(parentId: String) {
        db.collection("parents").document(parentId)
            .get()
            .addOnSuccessListener { parentDoc: DocumentSnapshot ->
                val childrenIds = parentDoc.get("children") as? List<String>

                if (!childrenIds.isNullOrEmpty()) {
                    db.collection("endUser")
                        .whereIn(FieldPath.documentId(), childrenIds)
                        .get()
                        .addOnSuccessListener { endUserResults: QuerySnapshot ->
                            val allRoutineIds = mutableListOf<String>()

                            for (endUserDoc: QueryDocumentSnapshot in endUserResults) {
                                (endUserDoc.get("routines") as? List<String>)?.let { routineIds ->
                                    allRoutineIds.addAll(routineIds)
                                }
                            }

                            if (allRoutineIds.isNotEmpty()) {
                                routineList.clear()

                                db.collection("routines")
                                    .whereIn(FieldPath.documentId(), allRoutineIds)
                                    .get()
                                    .addOnSuccessListener { routineResults: QuerySnapshot ->
                                        for (routineDoc: QueryDocumentSnapshot in routineResults) {
                                            routineDoc.toObject(Routine::class.java).let { routine ->
                                                routine.id = routineDoc.id  // Ensure ID is set
                                                routineList.add(routine)
                                            }
                                        }
                                        routineAdapter.notifyDataSetChanged()
                                    }
                                    .addOnFailureListener { error ->
                                        Snackbar.make(binding.root, "Error getting routines: ${error.message}",
                                            Snackbar.LENGTH_SHORT).show()
                                    }
                            }
                        }
                        .addOnFailureListener { error ->
                            Snackbar.make(binding.root, "Error getting end users: ${error.message}",
                                Snackbar.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { error ->
                Snackbar.make(binding.root, "Error getting parent data: ${error.message}",
                    Snackbar.LENGTH_SHORT).show()
            }
    }
}