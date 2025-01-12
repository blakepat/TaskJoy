package com.example.taskjoy.screens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskjoy.R
import com.example.taskjoy.adapters.ChildAdapter
import com.example.taskjoy.adapters.ChildClickListener
import com.example.taskjoy.databinding.ActivityMainBinding
import com.example.taskjoy.model.DailyRoutine
import com.example.taskjoy.model.EndUser
import com.example.taskjoy.model.Parent
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), ChildClickListener {

    private lateinit var binding: ActivityMainBinding
    private var db = Firebase.firestore
    private lateinit var auth: FirebaseAuth
    private var childList = mutableListOf<EndUser>()
    private lateinit var childAdapter: ChildAdapter
    private lateinit var selectedDate: Calendar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        selectedDate = Calendar.getInstance()

        setupRecyclerView()
        setupCalendar()
        setupClickListeners()
        updateCurrentDateDisplay()


        // Check if app was closed in child lock mode
        val prefs = getSharedPreferences("TaskJoyPrefs", Context.MODE_PRIVATE)
        val wasChildLocked = prefs.getBoolean("closedInChildLock", false)

        if (wasChildLocked) {
            // Clear the flag
            prefs.edit().putBoolean("closedInChildLock", false).apply()
            prefs.edit().putBoolean("childLockEnabled", false).apply()

            // Force logout
            Firebase.auth.signOut()

            // Show message
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()

            // Navigate to login screen
            // Replace with your login activity
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

    }

    private fun setupRecyclerView() {
        childAdapter = ChildAdapter(childList, this, auth.currentUser!!.uid)
        binding.childRecyclerView.apply {
            adapter = childAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun setupCalendar() {
        binding.calendarView.apply {
            date = System.currentTimeMillis()

            setOnDateChangeListener { _, year, month, dayOfMonth ->
                selectedDate.set(year, month, dayOfMonth)
                updateCurrentDateDisplay()
            }
        }
    }

    private fun setupClickListeners() {
//        binding.buttonToRoutineList.setOnClickListener {
//            val intent = Intent(this, RoutineListActivity::class.java).apply {
//                putExtra("selectedDate", Calendar.getInstance().timeInMillis)
//            }
//            startActivity(intent)
//        }

        binding.buttonCreateChild.setOnClickListener {
            val intent = Intent(this, CreateChildActivity::class.java)
            startActivity(intent)
        }

//        binding.buttonViewDateRoutines.setOnClickListener {
//            val intent = Intent(this, RoutineListActivity::class.java).apply {
//                putExtra("selectedDate", selectedDate.timeInMillis)
//            }
//            startActivity(intent)
//        }

        binding.buttonToGames.setOnClickListener {
            try {
                Log.d("EmotionGame", "Attempting to start EmotionMemoryActivity")
                val intent = Intent(this@MainActivity, EmotionMemoryActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("EmotionGame", "Failed to start EmotionMemoryActivity", e)
                Toast.makeText(this, "Unable to start game: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateCurrentDateDisplay() {
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        binding.currentDateDisplay.text = dateFormat.format(selectedDate.time)
    }

    override fun onResume() {
        super.onResume()
        getChildren()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_options, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.mi_logout -> {
                auth.signOut()
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onChildClick(id: String) {
        val intent = Intent(this, RoutineListActivity::class.java).apply {
            putExtra("endUser", id)  // Passing the child endUser ID to RoutineListActivity
            putExtra("selectedDate", selectedDate.timeInMillis)
        }
        startActivity(intent)
    }

    override fun onEditClick(id: String) {
        val intent = Intent(this, CreateChildActivity::class.java)
        intent.putExtra("endUser", id)
        startActivity(intent)
    }

    override fun onDeleteClick(id: String) {
        removeEndUserFromParent(auth.currentUser!!.uid, id)
    }

    private fun removeEndUserFromParent(parentId: String, endUserId: String) {
        db.collection("parents").document(parentId).get()
            .addOnSuccessListener { document ->
                val parent = document.toObject(Parent::class.java)
                val updatedChildren = parent?.children?.toMutableList() ?: mutableListOf()
                updatedChildren.remove(endUserId)

                db.runBatch { batch ->
                    // Remove child from parent
                    batch.update(
                        db.collection("parents").document(parentId),
                        "children", updatedChildren
                    )

                    // Delete EndUser document
                    batch.delete(db.collection("endUser").document(endUserId))

                    // Delete from children subcollection
                    batch.delete(
                        db.collection("parents").document(parentId)
                            .collection("children").document(endUserId)
                    )

                    // Delete all daily routines for this end user
                    db.collection("endUser")
                        .document(endUserId)
                        .collection("dailyRoutines")
                        .get()
                        .addOnSuccessListener { routines ->
                            val routineBatch = db.batch()
                            routines.forEach { routine ->
                                routineBatch.delete(routine.reference)
                            }
                            routineBatch.commit()
                        }

                    // Delete routine templates created for this end user
                    db.collection("routineTemplates")
                        .whereEqualTo("endUserId", endUserId)
                        .get()
                        .addOnSuccessListener { templates ->
                            val templateBatch = db.batch()
                            templates.forEach { template ->
                                templateBatch.delete(template.reference)
                            }
                            templateBatch.commit()
                        }

                }.addOnSuccessListener {
                    getChildren() // Refresh the list after successful deletion
                }.addOnFailureListener { e ->
                    Snackbar.make(binding.root, "Error removing child: ${e.message}",
                        Snackbar.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Snackbar.make(binding.root, "Error accessing parent data: ${e.message}",
                    Snackbar.LENGTH_SHORT).show()
            }
    }

    private fun getChildren() {
        Log.w("TESTING", "parentId: ${auth.currentUser!!.uid}")
        db.collection("parents")
            .document(auth.currentUser!!.uid)
            .get()
            .addOnSuccessListener { parentDoc: DocumentSnapshot ->
                val childrenIds = parentDoc.get("children") as? List<String>
                Log.w("TESTING", "childIDs: $childrenIds")
                if (!childrenIds.isNullOrEmpty()) {
                    db.collection("endUser")
                        .whereIn(FieldPath.documentId(), childrenIds)
                        .get()
                        .addOnSuccessListener { endUserResults: QuerySnapshot ->
                            childList.clear()
                            for (endUserDoc: QueryDocumentSnapshot in endUserResults) {
                                val childFromDB: EndUser = endUserDoc.toObject(EndUser::class.java)
                                childList.add(childFromDB)
                            }
                            childAdapter.notifyDataSetChanged()
                        }
                        .addOnFailureListener { error ->
                            Log.w("TESTING", "Error getting endUsers.", error)
                            Snackbar.make(binding.root, "Error getting end users",
                                Snackbar.LENGTH_SHORT).show()
                        }
                } else {
                    childList.clear()
                    childAdapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { error ->
                Log.w("TESTING", "Error getting parent.", error)
                Snackbar.make(binding.root, "Error getting parent data",
                    Snackbar.LENGTH_SHORT).show()
            }
    }
}