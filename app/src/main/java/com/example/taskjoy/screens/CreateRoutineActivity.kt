package com.example.taskjoy.screens

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.taskjoy.adapters.IconAdapter
import com.example.taskjoy.databinding.CreateRoutineScreenBinding
import com.example.taskjoy.model.RoutineTemplate
import com.example.taskjoy.model.TaskJoyIcon
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import java.util.Calendar

class CreateRoutineActivity : AppCompatActivity() {
    private lateinit var binding: CreateRoutineScreenBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var routineId: String? = null
    private var dailyRoutineId: String? = null
    private var endUserId: String? = null
    private var selectedIcon: TaskJoyIcon = TaskJoyIcon.MORNING
    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CreateRoutineScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = Firebase.auth

        // Get routineId, endUser and selectedDate from intent
        routineId = intent.getStringExtra("routineId")
        dailyRoutineId = intent.getStringExtra("dailyRoutineId")
        endUserId = intent.getStringExtra("endUser")
        intent.getLongExtra("selectedDate", -1).let { timestamp ->
            if (timestamp != -1L) {
                selectedDate.timeInMillis = timestamp
            }
        }

        Log.d("CreateRoutine", "Received IDs - RoutineId: $routineId, DailyRoutineId: $dailyRoutineId, EndUserId: $endUserId")

        if (endUserId == null) {
            Toast.makeText(this, "Error: No end user specified", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupIconRecyclerView()
        setupClickListeners()

        routineId?.let { loadRoutineData(it) }
    }

    private fun setupIconRecyclerView() {
        val iconAdapter = IconAdapter.createBasic(
            context = this,
            icons = TaskJoyIcon.values(),
            selectedIcon = selectedIcon
        ) { icon ->
            selectedIcon = icon
        }

        binding.rvIcons.apply {
            layoutManager = GridLayoutManager(this@CreateRoutineActivity, 3)
            adapter = iconAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSaveRoutine.setOnClickListener {
            saveRoutine()
        }
    }

    private fun loadRoutineData(routineId: String) {
        // First get the endUserId from the intent
        val intentEndUserId = intent.getStringExtra("endUser")

        db.collection("routineTemplates")
            .document(routineId)
            .get()
            .addOnSuccessListener { document ->
                val routine = document.toObject(RoutineTemplate::class.java)
                val createdBy = document.getString("createdBy")

                Log.d("CreateRoutine", "Loaded Routine - CreatedBy: $createdBy, EndUserId: $intentEndUserId")
                Log.d("CreateRoutine", "Current User ID: ${auth.currentUser?.uid}")

                // Use the endUserId from intent instead of document
                if (intentEndUserId == null) {
                    Log.e("CreateRoutine", "No end user ID available to check permissions")
                    Toast.makeText(this, "Error: Unable to verify routine access", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                // If created by current user, allow edit directly
                if (createdBy == auth.currentUser?.uid) {
                    routine?.let {
                        binding.etRoutineName.setText(it.name)
                        selectedIcon = TaskJoyIcon.fromString(it.image)
                        binding.rvIcons.adapter?.notifyDataSetChanged()
                    }
                    return@addOnSuccessListener
                }

                // Check parent permissions using intentEndUserId
                db.collection("endUser")
                    .document(intentEndUserId)
                    .get()
                    .addOnSuccessListener { userDoc ->
                        val parents = userDoc.get("parents") as? List<*>
                        val isParent = parents?.contains(auth.currentUser?.uid) == true

                        if (isParent) {
                            routine?.let {
                                binding.etRoutineName.setText(it.name)
                                selectedIcon = TaskJoyIcon.fromString(it.image)
                                binding.rvIcons.adapter?.notifyDataSetChanged()
                            }
                        } else {
                            Log.e("CreateRoutine", "Access denied - Not a parent")
                            Toast.makeText(this, "You don't have access to edit this routine", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("CreateRoutine", "Error checking user permissions", e)
                        Toast.makeText(this, "Error checking permissions: ${e.message}", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("CreateRoutine", "Error loading routine", e)
                Toast.makeText(this, "Error loading routine: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun saveRoutine() {
        val name = binding.etRoutineName.text.toString()
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a routine name", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        // Add debug logs
        Log.d("CreateRoutine", "Current User ID: $currentUserId")
        Log.d("CreateRoutine", "EndUser ID: $endUserId")

        // First verify the user has permission (is a parent of this endUser)
        db.collection("endUser")
            .document(endUserId!!)
            .get()
            .addOnSuccessListener { document ->
                val parents = document.get("parents") as? List<*>
                Log.d("CreateRoutine", "Parents list: $parents")

                if (parents?.contains(currentUserId) == true) {
                    // User is a parent, proceed with saving
                    saveRoutineTemplateAndDaily(name, currentUserId)
                } else {
                    Toast.makeText(
                        this,
                        "Permission denied: Only parents can create routines",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                Log.d("CreateRoutine", "Error checking permissions: ${e.message}")
                Toast.makeText(this, "Error checking permissions: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun saveRoutineTemplateAndDaily(name: String, currentUserId: String) {
        val batch = db.batch()

        // Create or update routine template
        val routineTemplatesCollection = db.collection("routineTemplates")
        val routineDoc = if (routineId != null) {
            routineTemplatesCollection.document(routineId!!)
        } else {
            routineTemplatesCollection.document()
        }

        val routineTemplate = hashMapOf(
            "name" to name,
            "image" to selectedIcon.name,
            "steps" to mutableListOf<String>(),
            "createdBy" to currentUserId,
            "createdAt" to Timestamp.now(),
            "endUserId" to endUserId
        )

        batch.set(routineDoc, routineTemplate, com.google.firebase.firestore.SetOptions.merge())

        // Handle daily routine update
        if (dailyRoutineId != null) {
            // If we have a dailyRoutineId, update that specific daily routine
            val dailyRoutineRef = db.collection("endUser")
                .document(endUserId!!)
                .collection("dailyRoutines")
                .document(dailyRoutineId!!)

            val dailyRoutine = hashMapOf(
                "name" to name,
                "image" to selectedIcon.name,
                "templateId" to routineDoc.id
            )

            batch.update(dailyRoutineRef, dailyRoutine as Map<String, Any>)
        } else {
            // For new routine, create a new daily routine
            val dailyRoutineRef = db.collection("endUser")
                .document(endUserId!!)
                .collection("dailyRoutines")
                .document()

            val dailyRoutine = hashMapOf(
                "name" to name,
                "date" to Timestamp(selectedDate.time),
                "image" to selectedIcon.name,
                "completed" to false,
                "templateId" to routineDoc.id,
                "notes" to ""
            )

            batch.set(dailyRoutineRef, dailyRoutine)

            // Only add template reference for new routines
            val endUserRef = db.collection("endUser").document(endUserId!!)
            batch.update(endUserRef, "routineTemplates", FieldValue.arrayUnion(routineDoc.id))
        }

        batch.commit()
            .addOnSuccessListener {
                val message = if (dailyRoutineId != null) "Routine updated successfully" else "Routine saved successfully"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving routine: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}