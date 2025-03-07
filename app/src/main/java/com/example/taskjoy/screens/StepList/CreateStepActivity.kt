package com.example.taskjoy.screens.StepList

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.taskjoy.Managers.CustomIconManager
import com.example.taskjoy.adapters.IconAdapter
import com.example.taskjoy.databinding.CreateStepScreenBinding
import com.example.taskjoy.model.CustomIcon
import com.example.taskjoy.model.TaskJoyIcon
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CreateStepActivity : AppCompatActivity() {
    private lateinit var binding: CreateStepScreenBinding
    private lateinit var customIconManager: CustomIconManager
    private var userId: String = "" // User ID
    private var routineId: String = "" // Routine ID
    private var stepId: String? = null // Step ID (null for create, non-null for edit)
    private var templateStepId: String? = null // Template Step ID
    private var selectedIcon: TaskJoyIcon = TaskJoyIcon.BRUSHTEETH // Default icon
    private var selectedCustomIcon: CustomIcon? = null

    // Initialize the ViewModel using the by viewModels() delegate
    private val viewModel: CreateStepViewModel by viewModels()

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

        setupCustomIconManager()

        // Retrieve userId and routineId from intent
        userId = intent.getStringExtra("userId") ?: run {
            Log.w("CreateStep", "Error: User ID not provided.")
            Toast.makeText(this, "Error: User ID not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        routineId = intent.getStringExtra("routineId") ?: run {
            Log.w("CreateStep", "Error: Routine ID not provided.")
            Toast.makeText(this, "Error: Routine ID not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Get stepId if we're editing
        stepId = intent.getStringExtra("stepId")
        templateStepId = intent.getStringExtra("templateStepId")

        setupIconRecyclerView()
        setupClickListeners()
        setupObservers()

        // If we're editing, load the existing step data
        stepId?.let { loadExistingStep(it) }
    }

    private fun setupObservers() {
        viewModel.step.observe(this) { step ->
            // Populate the fields
            binding.etStepName.setText(step.name)
            binding.etStepNotes.setText(step.description)

            // Handle both regular and custom icons
            if (step.customIconPath != null) {
                selectedIcon = TaskJoyIcon.CUSTOM
                selectedCustomIcon = CustomIcon(
                    id = System.currentTimeMillis(),
                    filepath = step.customIconPath
                )
            } else {
                try {
                    selectedIcon = TaskJoyIcon.valueOf(step.image)
                } catch (e: IllegalArgumentException) {
                    Log.w("CreateStep", "Invalid icon name: ${step.image}")
                    selectedIcon = TaskJoyIcon.BRUSHTEETH
                }
            }
            // Refresh the icon adapter with the new selection
            setupIconRecyclerView()
        }


        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        viewModel.saveSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, if (stepId != null) "Step updated successfully" else "Step created successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupCustomIconManager() {
        customIconManager = CustomIconManager(this)
    }

    private fun loadExistingStep(stepId: String) {
        viewModel.getStep(
            endUserId = userId,
            routineId = routineId,
            stepId = stepId
        )
    }

    private fun setupIconRecyclerView() {
        // Filter out CUSTOM from icons array if there are no custom icons
        val filteredIcons = TaskJoyIcon.entries.filter { icon ->
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

        val description = binding.etStepNotes.text.toString()
        val customIconPath = if (selectedIcon == TaskJoyIcon.CUSTOM) selectedCustomIcon?.filepath else null

        if (stepId != null) {
            // Update existing step
            viewModel.updateStep(
                endUserId = userId,
                routineId = routineId,
                stepId = stepId!!,
                templateStepId = templateStepId,
                name = name,
                description = description,
                icon = selectedIcon,
                customIconPath = customIconPath
            )
        } else {
            // Create new step
            viewModel.createStep(
                endUserId = userId,
                routineId = routineId,
                name = name,
                description = description,
                icon = selectedIcon,
                customIconPath = customIconPath
            )
        }
    }
}