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

class CreateRoutineActivity : AppCompatActivity() {
    private lateinit var binding: CreateRoutineScreenBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var routineId: String? = null
    private var endUserId: String? = null
    private var selectedIcon: TaskJoyIcon = TaskJoyIcon.CUSTOM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CreateRoutineScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = Firebase.auth

        // Get both routineId and endUserId from intent
        routineId = intent.getStringExtra("routineId")
        endUserId = intent.getStringExtra("endUser")

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
        val iconAdapter = IconAdapter(TaskJoyIcon.values(), selectedIcon) { icon ->
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
        // First verify this user has access to this routine
        db.collection("routineTemplates")
            .document(routineId)
            .get()
            .addOnSuccessListener { document ->
                val routine = document.toObject(RoutineTemplate::class.java)
                val createdBy = document.getString("createdBy")

                // Verify the user has access to this routine
                if (createdBy == auth.currentUser?.uid) {
                    // User created this routine, load it
                    routine?.let {
                        binding.etRoutineName.setText(it.name)
                        selectedIcon = TaskJoyIcon.fromString(it.image)
                        binding.rvIcons.adapter?.notifyDataSetChanged()
                    }
                } else {
                    // User doesn't have access
                    Toast.makeText(this, "You don't have access to this routine", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
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
                Log.d("CreateRoutine", "Parents list: $parents") // Debug log

                if (parents?.contains(currentUserId) == true) {
                    // User is a parent, proceed with saving
                    saveRoutineTemplate(name, currentUserId)
                } else {
                    Toast.makeText(this, "Permission denied: Only parents can create routines", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.d("CreateRoutine", "Error checking permissions: ${e.message}") // Debug log
                Toast.makeText(this, "Error checking permissions: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveRoutineTemplate(name: String, currentUserId: String) {
        val routineTemplate = hashMapOf(
            "name" to name,
            "image" to selectedIcon.name,
            "steps" to mutableListOf<String>(),
            "createdBy" to currentUserId,
            "createdAt" to Timestamp.now(),
            "endUserId" to endUserId
        )

        val batch = db.batch()
        val routineTemplatesCollection = db.collection("routineTemplates")
        val routineDoc = if (routineId != null) {
            routineTemplatesCollection.document(routineId!!)
        } else {
            routineTemplatesCollection.document()
        }

        batch.set(routineDoc, routineTemplate)

        if (routineId == null) {
            val endUserRef = db.collection("endUser").document(endUserId!!)
            batch.update(endUserRef, "routineTemplates", FieldValue.arrayUnion(routineDoc.id))
        }

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(this, "Routine template saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving routine: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}