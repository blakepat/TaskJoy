package com.example.taskjoy

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.taskjoy.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set click listener for the button
        binding.buttonToTaskList.setOnClickListener {
            val intent = Intent(this, TaskListActivity::class.java)
            startActivity(intent)
        }
    }
}
