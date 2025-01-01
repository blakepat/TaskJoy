package com.example.taskjoy.screens

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskjoy.R
import com.example.taskjoy.adapters.ChildAdapter
import com.example.taskjoy.adapters.ChildClickListener
import com.example.taskjoy.adapters.StepAdapter
import com.example.taskjoy.databinding.ActivityMainBinding
import com.example.taskjoy.model.EndUser
import com.example.taskjoy.model.Parent
import com.example.taskjoy.model.Routine
import com.example.taskjoy.model.Step
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity(), ChildClickListener {

    private lateinit var binding: ActivityMainBinding

    var db = Firebase.firestore
    private lateinit var auth: FirebaseAuth
    private var childList = mutableListOf<EndUser>()
    private lateinit var childAdapter: ChildAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        setupClickListeners()



        //Setup Recycler view
        childAdapter = ChildAdapter(childList, this)
        binding.childRecyclerView.adapter = childAdapter
        binding.childRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.childRecyclerView.addItemDecoration(
            DividerItemDecoration(
                this,
                LinearLayoutManager.VERTICAL
            )
        )
    }


    override fun onResume() {
        super.onResume()
        getChildren()
    }

    private fun setupClickListeners() {
        binding.buttonToRoutineList.setOnClickListener {
            val intent = Intent(this, RoutineListActivity::class.java)
            startActivity(intent)
        }
        binding.buttonCreateChild.setOnClickListener {
            val intent = Intent(this, CreateChildActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_options, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.mi_logout -> {
                auth.signOut()
                finish()
                return true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onChildClick(id: String) {
        val intent = Intent(this, RoutineListActivity::class.java)
        intent.putExtra("endUser", id)
        startActivity(intent)
    }

    override fun onDeleteClick(id: String) {
        removeEndUserFromParent(auth.currentUser!!.uid, id)
    }


    private fun removeEndUserFromParent(parentId: String, endUserId: String) {
        db.collection("parents").document(parentId).get()
            .addOnSuccessListener { document ->
                val parent = document.toObject(Parent::class.java)
                val updatedChildren = parent?.children?.toMutableList() ?: mutableListOf()
                updatedChildren.remove(endUserId)

                db.runBatch { batch ->
                    // Remove child from parent
                    batch.update(
                        db.collection("parents").document(parentId),
                        "children", updatedChildren
                    )

                    // Delete EndUser document
                    batch.delete(db.collection("endUser").document(endUserId))

                    // Delete from children subcollection
                    batch.delete(
                        db.collection("parents").document(parentId)
                            .collection("children").document(endUserId)
                    )
                }
            }
        getChildren()
    }


    private fun getChildren() {
        // First get the parent document to access their children list
        Log.w("TESTING", "parentId: ${auth.currentUser!!.uid}")
        db.collection("parents")
            .document(auth.currentUser!!.uid)
            .get()
            .addOnSuccessListener { parentDoc: DocumentSnapshot ->
                // Get the list of child (endUser) IDs
                val childrenIds = parentDoc.get("children") as? List<String>
                Log.w("TESTING", "childIDs: $childrenIds")
                if (!childrenIds.isNullOrEmpty()) {
                    // Query endUsers collection for documents with matching IDs
                    db.collection("endUser")
                        .whereIn(FieldPath.documentId(), childrenIds)
                        .get()
                        .addOnSuccessListener { endUserResults: QuerySnapshot ->
                            childList.clear()
                            for (endUserDoc: QueryDocumentSnapshot in endUserResults) {
                                val childFromDB: EndUser = endUserDoc.toObject(EndUser::class.java)
                                childList.add(childFromDB)
                                if (childList.isEmpty()) {
                                    //TODO: CREATE AN EMPTY LIST VIEW
                                }
                            }
                            childAdapter.notifyDataSetChanged()
                        }
                        .addOnFailureListener { error ->
                            Log.w("TESTING", "Error getting endUsers.", error)
                            Snackbar.make(binding.root, "Error getting end users",
                                Snackbar.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { error ->
                Log.w("TESTING", "Error getting parent.", error)
                Snackbar.make(binding.root, "Error getting parent data",
                    Snackbar.LENGTH_SHORT).show()
            }
    }
}
