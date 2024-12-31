package com.example.taskjoy.model

import com.google.firebase.firestore.DocumentId

data class Step(
    val name: String = "",
    val image: String = "LUNCH",
    val completed: Boolean = false,
    var notes: String = "",
    @DocumentId
    val id: String = "",
)

