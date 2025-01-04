package com.example.taskjoy.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Routine(
    val name: String = "",
    val date: Timestamp = Timestamp.now(),
    val image: String = "MORNING",
    var steps: MutableList<String> = mutableListOf(),
    var completed: Boolean = false,
    @DocumentId
    var id: String = "",
)