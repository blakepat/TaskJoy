package com.example.taskjoy.model
import com.google.firebase.firestore.DocumentId

data class Parent(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    var children: List<String> = listOf()
    )
