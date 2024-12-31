package com.example.taskjoy

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.taskjoy.model.RoutineAdapter
import com.example.taskjoy.databinding.ActivityStepDetailsBinding
import com.example.taskjoy.model.Routine
import com.example.taskjoy.model.Step
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class StepDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStepDetailsBinding
    val db = Firebase.firestore

    lateinit var step: Step

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStepDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val stepId = intent.getStringExtra("stepId")

        //TODO: CREATE CARD WITH NAME AND ICON.



        //TODO: MAKE RIGHT BUTTON COMPLETE ACTIVITY AND GO TO NEXT



        //TODO: MAKE LEFT BUTTON GO BACK TO PREVIOUS ACTIVITY (IF ANY)


        //gets step from firebase using ID passed from step list activity
        getStep(stepId ?: "")
    }

    private fun getStep(stepId: String) {
        db.collection("steps")
            .document(stepId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val stepFromDB: Step = document.toObject(Step::class.java)!!
                    step = stepFromDB

                    setupUI()
                } else {
                    Log.w("TESTING", "No such document")
                    Snackbar.make(binding.root, "Step not found", Snackbar.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { error ->
                Log.w("TESTING", "Error getting step document.", error)
                Snackbar.make(binding.root, "Error getting step", Snackbar.LENGTH_SHORT).show()
            }
    }


    //TODO: SETUP UI WITH STEP FROM DB
    private fun setupUI() {
        binding.textStepTitle.text = step.name
    }
}
