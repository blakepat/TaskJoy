package com.example.taskjoy.screens

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskjoy.R
import com.example.taskjoy.adapters.RoutineAdapter
import com.example.taskjoy.adapters.RoutineClickListener
import com.example.taskjoy.databinding.ActivityRoutineListBinding
import com.example.taskjoy.model.DailyRoutine
import com.example.taskjoy.model.RoutineTemplate
import com.example.taskjoy.model.Step
import com.google.android.gms.tasks.Tasks
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RoutineListActivity : AppCompatActivity(), RoutineClickListener {
    private lateinit var binding: ActivityRoutineListBinding
    private lateinit var routineAdapter: RoutineAdapter
    private val routineList = mutableListOf<DailyRoutine>()
    private var isEditMode = false
    private val db = Firebase.firestore
    private lateinit var auth: FirebaseAuth
    private var endUserId: String? = null
    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoutineListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setTitle("Routines")

        auth = Firebase.auth
        // Get endUserId from intent if available
        endUserId = intent.getStringExtra("endUser").toString()



        // Get selected date from intent if it exists
        intent.getLongExtra("selectedDate", -1).let { timestamp ->
            if (timestamp != -1L) {
                selectedDate.timeInMillis = timestamp
            }
        }

        // If endUserId is not found in intent, retrieve it based on the authenticated user
        if (endUserId == null) {
            auth.currentUser?.uid?.let { uid ->
                getAllEndUserDailyRoutines(uid)
            } ?: run {
                Snackbar.make(binding.root, "Error: User not authenticated", Snackbar.LENGTH_SHORT).show()
            }
        }

        setupRecyclerView()
        setupClickListeners()
        setupDateDisplay()
    }

    private fun setupDateDisplay() {
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        binding.textViewDate.text = dateFormat.format(selectedDate.time)
    }

    override fun onResume() {
        super.onResume()
        endUserId?.let { id ->
            createDailyRoutinesIfNeeded(id)
            getSpecificEndUserDailyRoutines(id)
        } ?: run {
            auth.currentUser?.uid?.let { uid ->
                getAllEndUserDailyRoutines(uid)
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
            val intent = Intent(this, CreateRoutineActivity::class.java).apply {
                putExtra("endUser", endUserId)
            }
            startActivity(intent)
        }
    }

    override fun onRoutineClick(routine: DailyRoutine) {
        val intent = Intent(this, StepListActivity::class.java).apply {
            Log.w("TESTING", "ROUTINE CLICK ON ROUTINE LIST SCREEN, ID: ${routine.id}")
            putExtra("routineId", routine.id)
            putExtra("isDaily", true)
            putExtra("endUser", endUserId)
        }
        startActivity(intent)
    }

    override fun onEditClick(routine: DailyRoutine) {
        val intent = Intent(this, CreateRoutineActivity::class.java).apply {
            putExtra("routineId", routine.templateId)
            putExtra("dailyRoutineId", routine.id)  // Add the daily routine ID
            putExtra("endUser", endUserId)
            putExtra("selectedDate", selectedDate.timeInMillis)  // Pass the selected date
        }
        startActivity(intent)
    }

    private fun createDailyRoutinesIfNeeded(endUserId: String) {
        val startOfDay = Calendar.getInstance().apply {
            timeInMillis = selectedDate.timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val endOfDay = Calendar.getInstance().apply {
            timeInMillis = selectedDate.timeInMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time

        db.collection("endUser")
            .document(endUserId)
            .collection("dailyRoutines")
            .whereGreaterThanOrEqualTo("date", Timestamp(startOfDay))
            .whereLessThanOrEqualTo("date", Timestamp(endOfDay))
            .get()
            .addOnSuccessListener { dailyRoutines ->
                if (dailyRoutines.isEmpty) {
                    createDailyRoutinesFromTemplates(endUserId)
                }
            }
    }

    private fun createDailyRoutinesFromTemplates(endUserId: String) {
        db.collection("endUser")
            .document(endUserId)
            .get()
            .addOnSuccessListener { endUserDoc ->
                val templateIds = endUserDoc.get("routineTemplates") as? List<*>
                if (templateIds.isNullOrEmpty()) {
                    Log.w("RoutineList", "No routine templates available")
                    return@addOnSuccessListener
                }

                db.collection("routineTemplates")
                    .whereIn(FieldPath.documentId(), templateIds)
                    .get()
                    .addOnSuccessListener { templates ->
                        val batch = db.batch()

                        templates.forEach { template ->
                            val templateData = template.toObject(RoutineTemplate::class.java)

                            val dailyRoutineRef = db.collection("endUser")
                                .document(endUserId)
                                .collection("dailyRoutines")
                                .document()

                            val dailyRoutine = DailyRoutine(
                                templateId = template.id,
                                name = templateData?.name ?: "",
                                date = Timestamp(selectedDate.time),
                                image = templateData?.image ?: "",
                                completed = false,
                                notes = ""
                            )

                            batch.set(dailyRoutineRef, dailyRoutine)

                            val stepFetches = templateData?.steps?.map { stepId ->
                                db.collection("steps").document(stepId).get()
                            } ?: emptyList()

                            Tasks.whenAllSuccess<DocumentSnapshot>(stepFetches)
                                .addOnSuccessListener { stepDocs ->
                                    stepDocs.forEach { stepDoc ->
                                        val stepData = stepDoc.toObject(Step::class.java)
                                        if (stepData != null) {
                                            val dailyStepRef = dailyRoutineRef.collection("dailySteps").document()
                                            val dailyStep = Step(
                                                name = stepData.name,
                                                description = stepData.description,
                                                image = stepData.image,
                                                completed = false,
                                                notes = "",
                                                templateStepId = stepDoc.id
                                            )
                                            batch.set(dailyStepRef, dailyStep)
                                        }
                                    }
                                    batch.commit()
                                        .addOnSuccessListener {
                                            getSpecificEndUserDailyRoutines(endUserId)
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("RoutineList", "Error committing batch write: ${e.message}")
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("RoutineList", "Error fetching step documents: ${e.message}")
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("RoutineList", "Error fetching routine templates: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("RoutineList", "Error fetching end user document: ${e.message}")
            }
    }


    private fun getSpecificEndUserDailyRoutines(endUserId: String) {
        val startOfDay = Calendar.getInstance().apply {
            timeInMillis = selectedDate.timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val endOfDay = Calendar.getInstance().apply {
            timeInMillis = selectedDate.timeInMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time

        db.collection("endUser")
            .document(endUserId)
            .collection("dailyRoutines")
            .whereGreaterThanOrEqualTo("date", Timestamp(startOfDay))
            .whereLessThanOrEqualTo("date", Timestamp(endOfDay))
            .get()
            .addOnSuccessListener { dailyRoutines ->
                routineList.clear()

                if (dailyRoutines.isEmpty) {
                    binding.recyclerViewRoutines.visibility = View.GONE
                    binding.emptyState.root.visibility = View.VISIBLE
                } else {
                    binding.recyclerViewRoutines.visibility = View.VISIBLE
                    binding.emptyState.root.visibility = View.GONE

                    for (routineDoc in dailyRoutines) {
                        val routine = routineDoc.toObject(DailyRoutine::class.java).apply {
                            id = routineDoc.id
                        }
                        routineList.add(routine)
                    }
                }
                routineAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { error ->
                Snackbar.make(binding.root,
                    "Error getting routines: ${error.message}",
                    Snackbar.LENGTH_SHORT).show()
            }
    }

    private fun getAllEndUserDailyRoutines(parentId: String) {
        db.collection("parents").document(parentId)
            .get()
            .addOnSuccessListener { parentDoc ->
                val childrenIds = parentDoc.get("children") as? List<String>

                if (!childrenIds.isNullOrEmpty()) {
                    routineList.clear()
                    childrenIds.forEach { childId ->
                        getSpecificEndUserDailyRoutines(childId)
                    }
                } else {
                    binding.recyclerViewRoutines.visibility = View.GONE
                    binding.emptyState.root.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { error ->
                Snackbar.make(binding.root,
                    "Error getting parent data: ${error.message}",
                    Snackbar.LENGTH_SHORT).show()
            }
    }


    override fun onDeleteClick(routine: DailyRoutine) {
        // Show confirmation dialog before deletion
        AlertDialog.Builder(this)
            .setTitle("Delete Routine")
            .setMessage("Are you sure you want to delete this routine?")
            .setPositiveButton("Delete") { _, _ ->
                deleteRoutine(routine)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteRoutine(routine: DailyRoutine) {
        Log.d("RoutineListActivity", "Starting deletion for routine: ${routine.id}, templateId: ${routine.templateId}")

        // Reference to all needed documents
        val dailyRoutineRef = db.collection("endUser")
            .document(endUserId!!)
            .collection("dailyRoutines")
            .document(routine.id)

        val routineTemplateRef = db.collection("routineTemplates")
            .document(routine.templateId)

        // First get all the dailySteps
        dailyRoutineRef.collection("dailySteps")
            .get()
            .addOnSuccessListener { dailyStepsSnapshot ->
                // Now start the transaction with the dailySteps data
                db.runTransaction { transaction ->
                    // 1. READS FIRST
                    // Get endUser document
                    val endUserRef = db.collection("endUser").document(endUserId!!)
                    val endUserDoc = transaction.get(endUserRef)

                    // Get routine template to check its steps
                    val templateDoc = transaction.get(routineTemplateRef)

                    // 2. PROCESS DATA
                    // Update templateIds array
                    val templateIds = if (endUserDoc.exists()) {
                        (endUserDoc.get("routineTemplates") as? MutableList<String> ?: mutableListOf()).apply {
                            remove(routine.templateId)
                        }
                    } else {
                        mutableListOf()
                    }

                    // Get template steps to delete
                    val templateSteps = if (templateDoc.exists()) {
                        val template = templateDoc.toObject(RoutineTemplate::class.java)
                        template?.steps ?: listOf()
                    } else {
                        listOf()
                    }

                    // 3. WRITES SECOND
                    // Delete all dailySteps
                    dailyStepsSnapshot.documents.forEach { stepDoc ->
                        val stepRef = dailyRoutineRef.collection("dailySteps").document(stepDoc.id)
                        transaction.delete(stepRef)
                    }

                    // Delete all template steps
                    templateSteps.forEach { stepId ->
                        val stepRef = db.collection("steps").document(stepId)
                        transaction.delete(stepRef)
                    }

                    // Delete the daily routine document
                    transaction.delete(dailyRoutineRef)

                    // Delete the routine template
                    transaction.delete(routineTemplateRef)

                    // Update the endUser document if it exists
                    if (endUserDoc.exists()) {
                        transaction.update(endUserRef, "routineTemplates", templateIds)
                    }
                }.addOnSuccessListener {
                    Log.d("RoutineListActivity", "Successfully deleted routine and all related documents")
                    routineList.remove(routine)
                    routineAdapter.notifyDataSetChanged()

                    if (routineList.isEmpty()) {
                        binding.recyclerViewRoutines.visibility = View.GONE
                        binding.emptyState.root.visibility = View.VISIBLE
                    }

                    Snackbar.make(binding.root, "Routine deleted successfully", Snackbar.LENGTH_SHORT).show()
                }.addOnFailureListener { error ->
                    Log.e("RoutineListActivity", "Error in deletion transaction", error)
                    Snackbar.make(
                        binding.root,
                        "Error deleting routine: ${error.localizedMessage}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener { error ->
                Log.e("RoutineListActivity", "Error getting dailySteps", error)
                Snackbar.make(
                    binding.root,
                    "Error getting dailySteps: ${error.localizedMessage}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
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
}