package com.example.taskjoy.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.Timestamp

data class DailyRoutine(
    val templateId: String = "",
    val name: String = "",
    val date: Timestamp = Timestamp.now(),
    val image: String = "MORNING",
    var steps: MutableList<Step> = mutableListOf(),
    var completed: Boolean = false,
    var notes: String = "",
    @DocumentId
    var id: String = ""
)