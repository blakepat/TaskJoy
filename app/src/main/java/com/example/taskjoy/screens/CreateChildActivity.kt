package com.example.taskjoy.screens

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.taskjoy.databinding.CreateChildScreenBinding
import com.example.taskjoy.model.EndUser
import com.example.taskjoy.model.Parent
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CreateChildActivity : AppCompatActivity() {

    lateinit var binding: CreateChildScreenBinding
    private var db = Firebase.firestore
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CreateChildScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth


        binding.btnAddChild.setOnClickListener {
            createEndUser().also { endUserId ->
                addEndUserToParent(endUserId)
            }
        }
    }


    private fun createEndUser(): String {
        val endUserRef = db.collection("endUser").document()

        val endUser = EndUser(
            id = endUserRef.id,
            name = binding.etChildName.text.toString(),
            age = binding.etChildAge.text.toString().toInt(),
            parents = mutableListOf(auth.currentUser!!.uid)
        )

        endUserRef.set(endUser)
        return endUserRef.id
    }


    private fun addEndUserToParent(endUserId: String) {
        db.collection("parents").document(auth.currentUser!!.uid).get()
            .addOnSuccessListener { document ->
                val parent = document.toObject(Parent::class.java)
                val updatedChildren = parent?.children?.toMutableList() ?: mutableListOf()
                updatedChildren.add(endUserId)

                db.collection("parents")
                    .document(auth.currentUser!!.uid)
                    .update("children", updatedChildren)
                    .addOnSuccessListener {
                        Snackbar.make(binding.root, "SUCCESS adding endUser to parent", Snackbar.LENGTH_SHORT).show()
                        finish()
                    }.addOnFailureListener { error ->
                        Snackbar.make(binding.root, "FAILED: $error", Snackbar.LENGTH_SHORT).show()
                    }
            }
    }
}