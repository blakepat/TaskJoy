package com.example.taskjoy.repository

import android.util.Log
import com.example.taskjoy.model.RoutineTemplate
import com.example.taskjoy.model.TaskJoyIcon
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Calendar

/**
 * Firebase implementation of the TemplateRepository interface
 */
class FirebaseTemplateRepository(
    private val db: FirebaseFirestore = Firebase.firestore,
    private val userRepository: UserRepository = FirebaseUserRepository()
) : TemplateRepository {
    private val TAG = "FirebaseTemplateRepo"

    override fun getRoutineTemplate(
        routineId: String,
        endUserId: String,
        currentUserId: String,
        onSuccess: (RoutineTemplate) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("routineTemplates")
            .document(routineId)
            .get()
            .addOnSuccessListener { document ->
                val routine = document.toObject(RoutineTemplate::class.java)
                val createdBy = document.getString("createdBy")

                Log.d(TAG, "Loaded Routine - CreatedBy: $createdBy, EndUserId: $endUserId")

                // If created by current user, allow edit directly
                if (createdBy == currentUserId) {
                    routine?.let { onSuccess(it) }
                    return@addOnSuccessListener
                }

                // Otherwise check parent permissions
                userRepository.checkParentPermission(
                    endUserId = endUserId,
                    currentUserId = currentUserId,
                    onSuccess = { isParent ->
                        if (isParent && routine != null) {
                            onSuccess(routine)
                        } else {
                            onFailure(Exception("Permission denied: Not a parent or routine not found"))
                        }
                    },
                    onFailure = { error ->
                        onFailure(error)
                    }
                )
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error loading routine", error)
                onFailure(error)
            }
    }

    override fun saveRoutine(
        routineId: String?,
        dailyRoutineId: String?,
        endUserId: String,
        name: String,
        icon: TaskJoyIcon,
        currentUserId: String,
        selectedDate: Calendar,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // First check parent permission
        userRepository.checkParentPermission(
            endUserId = endUserId,
            currentUserId = currentUserId,
            onSuccess = { isParent ->
                if (isParent) {
                    performRoutineSave(
                        routineId = routineId,
                        dailyRoutineId = dailyRoutineId,
                        endUserId = endUserId,
                        name = name,
                        icon = icon,
                        currentUserId = currentUserId,
                        selectedDate = selectedDate,
                        onSuccess = onSuccess,
                        onFailure = onFailure
                    )
                } else {
                    onFailure(Exception("Permission denied: Only parents can create/edit routines"))
                }
            },
            onFailure = { error ->
                onFailure(error)
            }
        )
    }

    private fun performRoutineSave(
        routineId: String?,
        dailyRoutineId: String?,
        endUserId: String,
        name: String,
        icon: TaskJoyIcon,
        currentUserId: String,
        selectedDate: Calendar,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val batch = db.batch()

        // Create or update routine template
        val routineTemplatesCollection = db.collection("routineTemplates")
        val routineDoc = if (routineId != null) {
            routineTemplatesCollection.document(routineId)
        } else {
            routineTemplatesCollection.document()
        }

        val routineTemplate = hashMapOf(
            "name" to name,
            "image" to icon.name,
            "steps" to mutableListOf<String>(),
            "createdBy" to currentUserId,
            "createdAt" to Timestamp.now(),
            "endUserId" to endUserId
        )

        batch.set(routineDoc, routineTemplate, SetOptions.merge())

        // Handle daily routine update
        if (dailyRoutineId != null) {
            // If we have a dailyRoutineId, update that specific daily routine
            val dailyRoutineRef = db.collection("endUser")
                .document(endUserId)
                .collection("dailyRoutines")
                .document(dailyRoutineId)

            val dailyRoutine = hashMapOf(
                "name" to name,
                "image" to icon.name,
                "templateId" to routineDoc.id
            )

            batch.update(dailyRoutineRef, dailyRoutine as Map<String, Any>)
        } else {
            // For new routine, create a new daily routine
            val dailyRoutineRef = db.collection("endUser")
                .document(endUserId)
                .collection("dailyRoutines")
                .document()

            val dailyRoutine = hashMapOf(
                "name" to name,
                "date" to Timestamp(selectedDate.time),
                "image" to icon.name,
                "completed" to false,
                "templateId" to routineDoc.id,
                "notes" to ""
            )

            batch.set(dailyRoutineRef, dailyRoutine)

            // Only add template reference for new routines
            val endUserRef = db.collection("endUser").document(endUserId)
            batch.update(endUserRef, "routineTemplates", FieldValue.arrayUnion(routineDoc.id))
        }

        batch.commit()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }
}