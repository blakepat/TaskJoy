package com.example.taskjoy.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.Timestamp

// Daily Routine (instance of a routine for a specific date)
data class DailyRoutine(
    val templateId: String = "", // Reference to the template routine
    val name: String = "",
    val date: Timestamp = Timestamp.now(),
    val image: String = "MORNING",
    var steps: MutableList<Step> = mutableListOf(), // Store embedded Step objects
    var completed: Boolean = false,
    var notes: String = "", // Routine-level notes
    @DocumentId
    var id: String = ""
)