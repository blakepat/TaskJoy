package com.example.taskjoy.model

import com.google.firebase.firestore.DocumentId

data class EndUser(
    val name: String = "",
    val age: Int = 0,
    var notes: MutableList<String> = mutableListOf(),
    var parents: MutableList<String> = mutableListOf(),
    var routines: MutableList<String> = mutableListOf(),
    @DocumentId
    val id: String = ""
)