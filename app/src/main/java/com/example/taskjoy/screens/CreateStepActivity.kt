package com.example.taskjoy.screens

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.taskjoy.adapters.IconAdapter
import com.example.taskjoy.databinding.CreateStepScreenBinding
import com.example.taskjoy.model.CustomIcon
import com.example.taskjoy.model.Step
import com.example.taskjoy.model.TaskJoyIcon
import com.example.taskjoy.CustomIconManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class CreateStepActivity : AppCompatActivity() {
    private lateinit var binding: CreateStepScreenBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var customIconManager: CustomIconManager
    private var userId: String = "" // User ID
    private var routineId: String = "" // Routine ID
    private var stepId: String? = null // Step ID (null for create, non-null for edit)
    private var selectedIcon: TaskJoyIcon = TaskJoyIcon.BRUSHTEETH // Default icon
    private var selectedCustomIcon: CustomIcon? = null

    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            customIconManager.saveIcon(it)?.let { customIcon ->
                updateIconAdapter()
                // Auto-select the newly added custom icon
                selectedIcon = TaskJoyIcon.CUSTOM
                selectedCustomIcon = customIcon
            } ?: run {
                Toast.makeText(this, "Failed to save custom icon", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CreateStepScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        setupCustomIconManager()

        // Retrieve userId and routineId from intent
        userId = intent.getStringExtra("userId") ?: run {
            Log.w("TESTING", "Error: User ID not provided.")
            Toast.makeText(this, "Error: User ID not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        routineId = intent.getStringExtra("routineId") ?: run {
            Log.w("TESTING", "Error: Routine ID not provided.")
            Toast.makeText(this, "Error: Routine ID not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Get stepId if we're editing
        stepId = intent.getStringExtra("stepId")

        setupIconRecyclerView()
        setupClickListeners()

        // If we're editing, load the existing step data
        stepId?.let { loadExistingStep(it) }
    }

    private fun setupCustomIconManager() {
        customIconManager = CustomIconManager(this)
    }

    private fun loadExistingStep(stepId: String) {
        db.collection("steps").document(stepId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val step = document.toObject(Step::class.java)
                    step?.let {
                        // Populate the fields
                        binding.etStepName.setText(it.name)
                        binding.etStepNotes.setText(it.notes)

                        // Handle both regular and custom icons
                        if (it.customIconPath != null) {
                            selectedIcon = TaskJoyIcon.CUSTOM
                            selectedCustomIcon = CustomIcon(
                                id = System.currentTimeMillis(),
                                filepath = it.customIconPath
                            )
                        } else {
                            try {
                                selectedIcon = TaskJoyIcon.valueOf(it.image)
                            } catch (e: IllegalArgumentException) {
                                Log.w("ERROR", "Invalid icon name: ${it.image}")
                                selectedIcon = TaskJoyIcon.BRUSHTEETH
                            }
                        }
                        // Refresh the icon adapter with the new selection
                        setupIconRecyclerView()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w("ERROR", "Error loading step: ${e.message}")
                Toast.makeText(this, "Error loading step data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupIconRecyclerView() {
        // Filter out CUSTOM from icons array if there are no custom icons
        val filteredIcons = TaskJoyIcon.values().filter { icon ->
            if (customIconManager.getAllIcons().isEmpty()) {
                icon != TaskJoyIcon.CUSTOM
            } else {
                true
            }
        }.toTypedArray()

        val iconAdapter = IconAdapter.createWithCustom(
            context = this,
            icons = filteredIcons,  // Use filtered icons instead
            customIcons = customIconManager.getAllIcons(),
            selectedIcon = selectedIcon,
            selectedCustomIcon = selectedCustomIcon,
            onIconSelected = { icon, customIcon ->
                selectedIcon = icon
                selectedCustomIcon = customIcon
            },
            onAddCustomIcon = {
                selectImageLauncher.launch("image/*")
            },
            onDeleteCustomIcon = { customIcon ->
                showDeleteConfirmationDialog(customIcon)
            }
        )

        binding.rvIcons.apply {
            layoutManager = GridLayoutManager(this@CreateStepActivity, 3)
            adapter = iconAdapter
        }
    }

    private fun showDeleteConfirmationDialog(customIcon: CustomIcon) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Custom Icon")
            .setMessage("Are you sure you want to delete this custom icon?")
            .setPositiveButton("Delete") { _, _ ->
                customIconManager.deleteIcon(customIcon)
                if (selectedCustomIcon == customIcon) {
                    selectedIcon = TaskJoyIcon.BRUSHTEETH
                    selectedCustomIcon = null
                }
                updateIconAdapter()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateIconAdapter() {
        val customIcons = customIconManager.getAllIcons()
        val filteredIcons = TaskJoyIcon.values().filter { icon ->
            if (customIcons.isEmpty()) {
                icon != TaskJoyIcon.CUSTOM
            } else {
                true
            }
        }.toTypedArray()

        (binding.rvIcons.adapter as? IconAdapter)?.updateData(
            filteredIcons,  // Use filtered icons
            customIcons,
            selectedIcon,
            selectedCustomIcon
        )
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSaveStep.setOnClickListener { saveStep() }
    }

    private fun saveStep() {
        val name = binding.etStepName.text.toString()
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a step name", Toast.LENGTH_SHORT).show()
            return
        }

        if (stepId != null) {
            updateExistingStep(name)
        } else {
            createNewStep(name)
        }
    }

    private fun updateExistingStep(name: String) {
        val stepRef = db.collection("steps").document(stepId!!)

        val updates = mutableMapOf<String, Any?>(  // Changed to allow null values
            "name" to name,
            "notes" to binding.etStepNotes.text.toString()
        )

        // Handle icon updates
        if (selectedIcon == TaskJoyIcon.CUSTOM && selectedCustomIcon != null) {
            updates["customIconPath"] = selectedCustomIcon!!.filepath
            updates["image"] = TaskJoyIcon.CUSTOM.name
        } else {
            updates["customIconPath"] = null as Any?  // Explicitly cast null as Any?
            updates["image"] = selectedIcon.name
        }

        stepRef.update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Step updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating step: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createNewStep(name: String) {
        val stepRef = db.collection("steps").document()

        val step = Step(
            name = name,
            image = selectedIcon.name,
            notes = binding.etStepNotes.text.toString(),
            completed = false,
            id = stepRef.id,
            customIconPath = if (selectedIcon == TaskJoyIcon.CUSTOM) selectedCustomIcon?.filepath else null
        )

        stepRef.set(step)
            .addOnSuccessListener {
                // After creating the step, update both the routine template and daily routine
                db.collection("endUser")
                    .document(userId)
                    .collection("dailyRoutines")
                    .document(routineId)
                    .get()
                    .addOnSuccessListener { routineDoc ->
                        val templateId = routineDoc.getString("templateId")
                        templateId?.let {
                            // Update routine template first
                            updateRoutineTemplate(it, stepRef.id)

                            // Now update the dailyRoutine by adding the new step
                            addStepToDailyRoutine(stepRef.id)
                        } ?: Log.w("ERROR", "Template ID not found.")
                    }
                    .addOnFailureListener { e ->
                        Log.w("ERROR", "Error retrieving dailyRoutine: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error creating step: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addStepToDailyRoutine(stepId: String) {
        db.collection("endUser")
            .document(userId)
            .collection("dailyRoutines")
            .document(routineId)
            .update("steps", FieldValue.arrayUnion(stepId))
            .addOnSuccessListener {
                Toast.makeText(this, "Step added to daily routine.", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Log.w("ERROR", "Error updating daily routine: ${e.message}")
                Toast.makeText(this, "Error updating daily routine: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateRoutineTemplate(templateId: String, stepId: String) {
        db.collection("routineTemplates")
            .document(templateId)
            .update("steps", FieldValue.arrayUnion(stepId))
            .addOnSuccessListener {
                Toast.makeText(this, "Step created and added successfully.", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Log.w("ERROR", "Error updating routineTemplate: ${e.message}")
            }
    }
}