package com.example.taskjoy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.taskjoy.model.StepAdapter
import com.example.taskjoy.databinding.ActivityTaskDetailsBinding
import com.example.taskjoy.model.Task

class TaskDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskDetailsBinding
    private lateinit var stepAdapter: StepAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get task data from intent
        val task = intent.getSerializableExtra("task") as? Task
        task?.let {
            // Set title
            binding.textTaskTitle.text = it.name

            // Prepare steps data
            val steps = (1..it.stepQuantity).map { step -> "Image $step" }

            // Set up RecyclerView
            stepAdapter = StepAdapter(steps)
            binding.recyclerViewSteps.layoutManager = GridLayoutManager(this, 2)
            binding.recyclerViewSteps.adapter = stepAdapter
        }
    }
}
