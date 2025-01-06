package com.example.taskjoy.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.Timestamp

// Daily Routine (instance of a routine for a specific date)
data class DailyRoutine(
    val templateId: String = "", // Reference to the template routine
    val name: String = "",
    val date: Timestamp = Timestamp.now(),
    val image: String = "MORNING",
    var steps: MutableList<String> = mutableListOf(),
    var completed: Boolean = false,
    var notes: String = "", // Add notes field
    @DocumentId
    var id: String = ""
)