package com.example.taskjoy.screens

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskjoy.R
import com.example.taskjoy.adapters.RoutineAdapter
import com.example.taskjoy.adapters.RoutineClickListener
import com.example.taskjoy.databinding.ActivityRoutineListBinding
import com.example.taskjoy.model.DailyRoutine
import com.example.taskjoy.model.RoutineTemplate
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
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
        endUserId = intent.getStringExtra("endUser")

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
            putExtra("endUser", endUserId)
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
                if (templateIds != null) {
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
                                    name = templateData.name,
                                    date = Timestamp(selectedDate.time),
                                    image = templateData.image,
                                    steps = templateData.steps.toMutableList(),
                                    completed = false,
                                    notes = ""
                                )

                                batch.set(dailyRoutineRef, dailyRoutine)
                            }

                            batch.commit().addOnSuccessListener {
                                getSpecificEndUserDailyRoutines(endUserId)
                            }
                        }
                }
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