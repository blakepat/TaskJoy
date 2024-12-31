package com.example.taskjoy.screens

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.taskjoy.R
import com.example.taskjoy.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    var db = Firebase.firestore
    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        setupClickListeners()
    }



    private fun setupClickListeners() {
        binding.buttonToRoutineList.setOnClickListener {
            val intent = Intent(this, RoutineListActivity::class.java)
            startActivity(intent)
        }
        binding.buttonChildOneRoutines.setOnClickListener {
            val intent = Intent(this, RoutineListActivity::class.java)
            intent.putExtra("endUser", "VVtTNqZPPUTBZu5GTRAQ")
            startActivity(intent)
        }
        binding.buttonChildTwoRoutines.setOnClickListener {
            val intent = Intent(this, RoutineListActivity::class.java)
            intent.putExtra("endUser", "kFFPl1Vz4aBNfkjGrFIh")
            startActivity(intent)
        }
        binding.buttonCreateChild.setOnClickListener {
            //TODO: CREATE CHILD SCREEN
//            val intent = Intent(this, RoutineListActivity::class.java)
//            startActivity(intent)
        }
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
                return true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}
