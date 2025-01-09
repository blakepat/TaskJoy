package com.example.taskjoy.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class RoutineTemplate(
    val name: String = "",
    val image: String = "MORNING",
    var steps: MutableList<String> = mutableListOf(),
    val createdBy: String = "",
    val endUserId: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    @DocumentId
    var id: String = ""
)