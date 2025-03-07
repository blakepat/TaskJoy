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
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import kotlin.coroutines.cancellation.CancellationException

/**
 * Firebase implementation of the TemplateRepository interface
 */
class FirebaseTemplateRepository(
    private val db: FirebaseFirestore = Firebase.firestore,
    private val userRepository: UserRepository = FirebaseUserRepository()
) : TemplateRepository {
    private val TAG = "FirebaseTemplateRepo"

    override suspend fun getRoutineTemplate(
        routineId: String,
        endUserId: String,
        currentUserId: String
    ): Result<RoutineTemplate> {
        return try {
            val document = db.collection("routineTemplates")
                .document(routineId)
                .get()
                .await()

            val routine = document.toObject(RoutineTemplate::class.java)
            val createdBy = document.getString("createdBy")

            Log.d(TAG, "Loaded Routine - CreatedBy: $createdBy, EndUserId: $endUserId")

            // If created by current user, allow edit directly
            if (createdBy == currentUserId) {
                routine?.let {
                    return Result.success(it)
                } ?: return Result.failure(Exception("Routine not found"))
            }

            // Otherwise check parent permissions
            val permissionResult = userRepository.checkParentPermission(endUserId, currentUserId)

            permissionResult.fold(
                onSuccess = { isParent ->
                    if (isParent && routine != null) {
                        Result.success(routine)
                    } else {
                        Result.failure(Exception("Permission denied: Not a parent or routine not found"))
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading routine", e)
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun saveRoutine(
        routineId: String?,
        dailyRoutineId: String?,
        endUserId: String,
        name: String,
        icon: TaskJoyIcon,
        currentUserId: String,
        selectedDate: Calendar
    ): Result<Unit> {
        return try {
            // First check parent permission
            val permissionResult = userRepository.checkParentPermission(endUserId, currentUserId)

            permissionResult.fold(
                onSuccess = { isParent ->
                    if (isParent) {
                        performRoutineSave(
                            routineId = routineId,
                            dailyRoutineId = dailyRoutineId,
                            endUserId = endUserId,
                            name = name,
                            icon = icon,
                            currentUserId = currentUserId,
                            selectedDate = selectedDate
                        )
                    } else {
                        Result.failure(Exception("Permission denied: Only parents can create/edit routines"))
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error saving routine", e)
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    private suspend fun performRoutineSave(
        routineId: String?,
        dailyRoutineId: String?,
        endUserId: String,
        name: String,
        icon: TaskJoyIcon,
        currentUserId: String,
        selectedDate: Calendar
    ): Result<Unit> {
        return try {
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

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error in performRoutineSave", e)
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}