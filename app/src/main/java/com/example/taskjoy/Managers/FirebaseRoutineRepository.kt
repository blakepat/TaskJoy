package com.example.taskjoy.repository

import android.util.Log
import com.example.taskjoy.model.DailyRoutine
import com.example.taskjoy.model.RoutineTemplate
import com.example.taskjoy.model.Step
import com.example.taskjoy.repository.utils.FirebaseUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import kotlin.coroutines.cancellation.CancellationException


class FirebaseRoutineRepository(
    private val db: FirebaseFirestore = Firebase.firestore
) : RoutineRepository {
    private val TAG = "FirebaseRoutineRepo"

    override suspend fun createDailyRoutinesIfNeeded(
        endUserId: String,
        date: Calendar
    ): Result<Unit> {
        return try {
            val (startTimestamp, endTimestamp) = FirebaseUtils.getDayTimestamps(date)

            val dailyRoutines = db.collection("endUser")
                .document(endUserId)
                .collection("dailyRoutines")
                .whereGreaterThanOrEqualTo("date", startTimestamp)
                .whereLessThanOrEqualTo("date", endTimestamp)
                .get()
                .await()

            if (dailyRoutines.isEmpty) {
                createDailyRoutinesFromTemplates(endUserId, date)
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking daily routines", e)
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    private suspend fun createDailyRoutinesFromTemplates(
        endUserId: String,
        date: Calendar
    ): Result<Unit> {
        return try {
            val endUserDoc = db.collection("endUser")
                .document(endUserId)
                .get()
                .await()

            val templateIds = endUserDoc.get("routineTemplates") as? List<*>
            if (templateIds.isNullOrEmpty()) {
                Log.w(TAG, "No routine templates available")
                return Result.success(Unit) // No templates to create routines from
            }

            val templates = db.collection("routineTemplates")
                .whereIn(FieldPath.documentId(), templateIds)
                .get()
                .await()

            val batch = db.batch()

            for (template in templates) {
                val templateData = template.toObject(RoutineTemplate::class.java)

                val dailyRoutineRef = db.collection("endUser")
                    .document(endUserId)
                    .collection("dailyRoutines")
                    .document()

                val dailyRoutine = DailyRoutine(
                    templateId = template.id,
                    name = templateData.name,
                    date = Timestamp(date.time),
                    image = templateData.image,
                    completed = false,
                    notes = ""
                )

                batch.set(dailyRoutineRef, dailyRoutine)

                // If there are no steps, continue to the next template
                if (templateData.steps.isEmpty()) {
                    continue
                }

                // Fetch all steps for this template
                val stepDocs = templateData.steps.map { stepId ->
                    db.collection("steps").document(stepId).get().await()
                }

                // Add all steps to the batch
                stepDocs.forEachIndexed { index, stepDoc ->
                    val stepData = stepDoc.toObject<Step>()
                    if (stepData != null) {
                        val dailyStepRef = dailyRoutineRef
                            .collection("dailySteps")
                            .document()

                        val dailyStep = Step(
                            name = stepData.name,
                            description = stepData.description,
                            image = stepData.image,
                            completed = false,
                            notes = "",
                            templateStepId = stepDoc.id,
                            order = index
                        )
                        batch.set(dailyStepRef, dailyStep)
                    }
                }
            }

            // Commit the batch
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating daily routines from templates", e)
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun getDailyRoutines(
        endUserId: String,
        date: Calendar
    ): Result<List<DailyRoutine>> {
        return try {
            val (startTimestamp, endTimestamp) = FirebaseUtils.getDayTimestamps(date)

            val dailyRoutines = db.collection("endUser")
                .document(endUserId)
                .collection("dailyRoutines")
                .whereGreaterThanOrEqualTo("date", startTimestamp)
                .whereLessThanOrEqualTo("date", endTimestamp)
                .get()
                .await()

            val routineList = dailyRoutines.documents.map { routineDoc ->
                routineDoc.toObject(DailyRoutine::class.java)?.apply {
                    id = routineDoc.id
                } ?: throw Exception("Failed to parse DailyRoutine document")
            }

            Result.success(routineList)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting daily routines", e)
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun getAllEndUserDailyRoutines(
        parentId: String,
        date: Calendar
    ): Result<List<DailyRoutine>> {
        return try {
            val parentDoc = db.collection("parents")
                .document(parentId)
                .get()
                .await()

            val childrenIds = parentDoc.get("children") as? List<String>
            if (childrenIds.isNullOrEmpty()) {
                return Result.success(emptyList())
            }

            val allRoutines = mutableListOf<DailyRoutine>()
            for (childId in childrenIds) {
                val childRoutinesResult = getDailyRoutines(childId, date)

                // If successful, add the routines to our list
                childRoutinesResult.fold(
                    onSuccess = { routines ->
                        allRoutines.addAll(routines)
                    },
                    onFailure = { error ->
                        // Log the error but continue with other children
                        Log.e(TAG, "Error getting routines for child $childId", error)
                    }
                )
            }

            Result.success(allRoutines)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all end user daily routines", e)
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun deleteRoutine(
        endUserId: String,
        routine: DailyRoutine
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Starting deletion for routine: ${routine.id}, templateId: ${routine.templateId}")

            // Reference to all needed documents
            val dailyRoutineRef = db.collection("endUser")
                .document(endUserId)
                .collection("dailyRoutines")
                .document(routine.id)

            val routineTemplateRef = db.collection("routineTemplates")
                .document(routine.templateId)

            // get all dailySteps
            val dailyStepsSnapshot = dailyRoutineRef.collection("dailySteps")
                .get()
                .await()

            db.runTransaction { transaction ->
                // Get endUser document
                val endUserRef = db.collection("endUser").document(endUserId)
                val endUserDoc = transaction.get(endUserRef)

                // Get routine template to check its steps
                val templateDoc = transaction.get(routineTemplateRef)

                // Update templateIds array
                val templateIds = if (endUserDoc.exists()) {
                    (endUserDoc.get("routineTemplates") as? MutableList<String> ?: mutableListOf()).apply {
                        remove(routine.templateId)
                    }
                } else {
                    mutableListOf()
                }

                // Get template steps to delete
                val templateSteps = if (templateDoc.exists()) {
                    val template = templateDoc.toObject(RoutineTemplate::class.java)
                    template?.steps ?: listOf()
                } else {
                    listOf()
                }

                // Delete all dailySteps
                dailyStepsSnapshot.documents.forEach { stepDoc ->
                    val stepRef = dailyRoutineRef.collection("dailySteps").document(stepDoc.id)
                    transaction.delete(stepRef)
                }

                // Delete all template steps
                templateSteps.forEach { stepId ->
                    val stepRef = db.collection("steps").document(stepId)
                    transaction.delete(stepRef)
                }

                // Delete the daily routine document
                transaction.delete(dailyRoutineRef)

                // Delete the routine template
                transaction.delete(routineTemplateRef)

                // Update the endUser document if it exists
                if (endUserDoc.exists()) {
                    transaction.update(endUserRef, "routineTemplates", templateIds)
                }
            }.await()

            Log.d(TAG, "Successfully deleted routine and all related documents")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting routine", e)
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}