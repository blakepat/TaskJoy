package com.example.taskjoy.screens.HomePage

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskjoy.R
import com.example.taskjoy.adapters.ChildAdapter
import com.example.taskjoy.adapters.ChildClickListener
import com.example.taskjoy.databinding.ActivityMainBinding
import com.example.taskjoy.model.EndUser
import com.example.taskjoy.screens.RoutineList.RoutineListActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), ChildClickListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private val childList = mutableListOf<EndUser>()
    private lateinit var childAdapter: ChildAdapter
    private lateinit var selectedDate: Calendar

    // Initialize the ViewModel using the by viewModels() delegate
    private val viewModel: MainViewModel by viewModels()

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
        setupObservers()

        checkChildLockMode()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupObservers() {
        viewModel.children.observe(this) { children ->
            childList.clear()
            childList.addAll(children)
            childAdapter.notifyDataSetChanged()
        }


        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun checkChildLockMode() {
        val prefs = getSharedPreferences("TaskJoyPrefs", Context.MODE_PRIVATE)
        val wasChildLocked = prefs.getBoolean("childLockEnabled", false)

        if (wasChildLocked) {
            prefs.edit().putBoolean("childLockEnabled", false).apply()

            Firebase.auth.signOut()
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    private fun setupRecyclerView() {
        val currentUserId = getCurrentUserId() ?: return

        childAdapter = ChildAdapter(childList, this, currentUserId)
        binding.childRecyclerView.apply {
            adapter = childAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
            addItemDecoration(DividerItemDecoration(this.context, LinearLayoutManager.VERTICAL))
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
        binding.buttonCreateChild.setOnClickListener {
            val intent = Intent(this, CreateChildActivity::class.java)
            startActivity(intent)
        }

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
            putExtra("endUser", id)
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
        val currentUserId = getCurrentUserId()
        if (currentUserId != null) {
            lifecycleScope.launch {
                viewModel.deleteEndUser(currentUserId, id)
            }
        } else {
            Snackbar.make(binding.root, "User not authenticated", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun getChildren() {
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            Log.w("MainActivity", "Cannot get children: User not authenticated")
            return
        }

        viewModel.getChildren(currentUserId)
    }
}