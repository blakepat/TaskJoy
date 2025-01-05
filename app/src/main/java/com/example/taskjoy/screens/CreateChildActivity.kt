package com.example.taskjoy.screens

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.taskjoy.R
import com.example.taskjoy.databinding.CreateChildScreenBinding
import com.example.taskjoy.model.EndUser
import com.example.taskjoy.model.Parent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.auth.User
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
        val id = intent.getStringExtra("endUser")


        binding.btnAddChild.setOnClickListener {
            createEndUser().also { endUserId ->
                addEndUserToParent(endUserId)
            }
        }

        binding.btnUpdateChild.setOnClickListener {
            id?.let { it1 -> updateEndUser(it1) }
        }

        binding.btnAddAnotherParent.setOnClickListener {
            id?.let { endUserId ->
                showAddParentDialog(endUserId)
            } ?: run {
                Snackbar.make(binding.root, "Please create or select a child first", Snackbar.LENGTH_SHORT).show()
            }
        }

        if (id != null) {
            Log.w("TESTING", "id: $id")
            updateUserUI(id)
        }
    }


    private fun updateUserUI(id: String) {
        getEndUser(id)
        binding.btnAddChild.isEnabled = false
        binding.btnAddChild.isVisible = false
        binding.btnAddAnotherParent.isEnabled = true
        binding.btnAddAnotherParent.isVisible = true
        binding.btnUpdateChild.isEnabled = true
        binding.btnUpdateChild.isVisible = true
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

    private fun updateEndUser(endUserId: String) {
        val updates = mapOf(
            "name" to binding.etChildName.text.toString(),
            "age" to binding.etChildAge.text.toString().toInt()
        )

        db.collection("endUser")
            .document(endUserId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "User updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun addEndUserToAnotherParent(endUserId: String, parentEmail: String) {
        // First find the parent by email
        db.collection("parents")
            .whereEqualTo("email", parentEmail)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Snackbar.make(binding.root, "No parent found with that email", Snackbar.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val parentDoc = documents.documents[0]
                val parent = parentDoc.toObject(Parent::class.java)
                val updatedChildren = parent?.children?.toMutableList() ?: mutableListOf()

                // Check if child is already added to this parent
                if (updatedChildren.contains(endUserId)) {
                    Snackbar.make(binding.root, "Child already added to this parent", Snackbar.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                updatedChildren.add(endUserId)

                db.collection("parents")
                    .document(parentDoc.id)
                    .update("children", updatedChildren)
                    .addOnSuccessListener {
                        Snackbar.make(binding.root, "Successfully added child to other parent", Snackbar.LENGTH_SHORT).show()
                    }.addOnFailureListener { error ->
                        Snackbar.make(binding.root, "Failed: $error", Snackbar.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Snackbar.make(binding.root, "Error finding parent: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
    }

    private fun showAddParentDialog(endUserId: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_parent, null)
        val emailEditText = dialogView.findViewById<EditText>(R.id.etParentEmail)

        MaterialAlertDialogBuilder(this)
            .setTitle("Add Another Parent")
            .setView(dialogView)
            .setMessage("Enter the email of the parent you want to add this child to:")
            .setPositiveButton("Add") { dialog, _ ->
                val email = emailEditText.text.toString()
                if (email.isNotEmpty()) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Confirm Addition")
                        .setMessage("Are you sure you want to add this child to parent with email: $email?")
                        .setPositiveButton("Yes") { confirmDialog, _ ->
                            addEndUserToAnotherParent(endUserId, email)
                            confirmDialog.dismiss()
                        }
                        .setNegativeButton("No") { confirmDialog, _ ->
                            confirmDialog.dismiss()
                        }
                        .show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    private fun getEndUser(endUserId: String) {
        db.collection("endUser")
            .document(endUserId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val user: EndUser? = document.toObject(EndUser::class.java)

                    if (user != null) {
                        binding.etChildAge.setText(user.age.toString())
                        binding.etChildName.setText(user.name)
                    }
                } else {
                    Log.w("TESTING", "failure getting endUser document")
                }
            }
            .addOnFailureListener { exception ->
                Log.w("TESTING", "failure getting endUser: $exception")
            }
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