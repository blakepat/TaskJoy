package com.example.taskjoy.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Step(
    val name: String = "",
    val image: String = "LUNCH",
    val description: String = "", // Template instructions
    var completed: Boolean = false,
    var completedAt: Timestamp? = null,
    var notes: String = "",      // Daily specific notes
    val customIconPath: String? = null,
    @DocumentId
    val id: String = "",
    val templateStepId: String = "" // Reference to original template step
)
