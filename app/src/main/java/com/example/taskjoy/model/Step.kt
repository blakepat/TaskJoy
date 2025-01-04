package com.example.taskjoy.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Step(
    val name: String = "",
    val image: String = "LUNCH",
    var completed: Boolean = false,
    var completedAt: Timestamp? = null,
    var notes: String = "",
    @DocumentId
    val id: String = "",
)

