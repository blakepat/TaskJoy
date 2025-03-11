package com.example.taskjoy.repository

import android.util.Log
import com.example.taskjoy.model.DailyRoutine
import com.example.taskjoy.model.RoutineTemplate
import com.example.taskjoy.model.Step
import com.example.taskjoy.model.TaskJoyIcon
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException


class FirebaseStepRepository(
    private val db: FirebaseFirestore = Firebase.firestore
) : StepRepository {
    private val TAG = "FirebaseStepRepository"

    override suspend fun getRoutineWithSteps(
        endUserId: String,
        routineId: String
    ): Result<Pair<DailyRoutine, List<Step>>> {
        return try {
            val routineDoc = db.collection("endUser").document(endUserId).collection("dailyRoutines")
                .document(routineId)
                .get()
                .await()

            Log.d(TAG, "Routine document fetched: ${routineDoc.data}")

            val routineData = routineDoc.toObject(DailyRoutine::class.java) ?: DailyRoutine()
            val routine = routineData.copy(id = routineId)

            //GET STEPS
            val stepDocs = db.collection("endUser").document(endUserId).collection("dailyRoutines")
                .document(routineId).collection("dailySteps")
                .orderBy("order", Query.Direction.ASCENDING)
                .get()
                .await()

            Log.d(TAG, "Steps fetched: ${stepDocs.size()} steps")
            val steps = stepDocs.documents.map { stepDoc ->
                // Get the step data and create a new Step with the ID properly set
                val stepData = stepDoc.toObject(Step::class.java) ?: Step()
                // Assuming Step is a data class
                stepData.copy(id = stepDoc.id)
            }

            Result.success(Pair(routine, steps))
        } catch (e: Exception) {
            Log.e(TAG, "Error getting routine with steps", e)
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun saveStepOrder(
        endUserId: String,
        routineId: String,
        steps: List<Step>
    ): Result<Unit> {
        return try {
            val dailyRoutineRef = db.collection("endUser")
                .document(endUserId)
                .collection("dailyRoutines")
                .document(routineId)

            db.runTransaction { transaction ->
                // 1. Get the daily routine to fetch the template ID
                val dailyRoutineDoc = transaction.get(dailyRoutineRef)
                if (!dailyRoutineDoc.exists()) {
                    throw FirebaseFirestoreException(
                        "Daily routine document not found",
                        FirebaseFirestoreException.Code.NOT_FOUND
                    )
                }

                val dailyRoutine = dailyRoutineDoc.toObject(DailyRoutine::class.java)
                val templateId = dailyRoutine?.templateId

                if (!templateId.isNullOrEmpty()) {
                    // 2. Get and update template steps order
                    val templateRef = db.collection("routineTemplates").document(templateId)
                    val templateDoc = transaction.get(templateRef)

                    if (templateDoc.exists()) {
                        val orderedTemplateStepIds = steps
                            .filter { it.templateStepId.isNotEmpty() }
                            .map { it.templateStepId }
                        transaction.update(templateRef, "steps", orderedTemplateStepIds)
                    }
                }

                // 3. Update order in dailySteps subcollection
                steps.forEachIndexed { index, step ->
                    val stepRef = dailyRoutineRef
                        .collection("dailySteps")
                        .document(step.id)
                    transaction.update(stepRef, "order", index)
                }
            }.await()

            Log.d(TAG, "Successfully saved step order")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving step order", e)
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun updateRemainingStepsOrder(
        endUserId: String,
        routineId: String,
        steps: List<Step>
    ): Result<Unit> {
        return try {
            val batch = db.batch()

            steps.forEachIndexed { index, step ->
                val stepRef = db.collection("endUser")
                    .document(endUserId)
                    .collection("dailyRoutines")
                    .document(routineId)
                    .collection("dailySteps")
                    .document(step.id)

                batch.update(stepRef, "order", index)
            }

            batch.commit().await()
            Log.d(TAG, "Successfully updated step order")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating step order", e)
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun deleteStep(
        endUserId: String,
        routineId: String,
        step: Step
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Starting deletion for step: ${step.id}, templateStepId: ${step.templateStepId}")

            val dailyStepRef = db.collection("endUser")
                .document(endUserId)
                .collection("dailyRoutines")
                .document(routineId)
                .collection("dailySteps")
                .document(step.id)

            val dailyRoutineRef = db.collection("endUser")
                .document(endUserId)
                .collection("dailyRoutines")
                .document(routineId)

            val templateStepId = step.templateStepId

            if (templateStepId.isEmpty()) {
                // If no template step id, delete the daily step
                dailyStepRef.delete().await()
                return Result.success(Unit)
            }

            db.runTransaction { transaction ->
                val dailyRoutineDoc = transaction.get(dailyRoutineRef)
                if (!dailyRoutineDoc.exists()) {
                    throw FirebaseFirestoreException(
                        "Daily routine document not found",
                        FirebaseFirestoreException.Code.NOT_FOUND
                    )
                }

                val dailyRoutine = dailyRoutineDoc.toObject(DailyRoutine::class.java)
                val templateId = dailyRoutine?.templateId

                if (templateId.isNullOrEmpty()) {
                    throw FirebaseFirestoreException(
                        "Template ID not found in daily routine",
                        FirebaseFirestoreException.Code.NOT_FOUND
                    )
                }

                val templateRef = db.collection("routineTemplates").document(templateId)
                val templateDoc = transaction.get(templateRef)

                if (!templateDoc.exists()) {
                    throw FirebaseFirestoreException(
                        "Template document not found",
                        FirebaseFirestoreException.Code.NOT_FOUND
                    )
                }

                val template = templateDoc.toObject(RoutineTemplate::class.java)
                val updatedSteps = template?.steps?.toMutableList() ?: mutableListOf()
                updatedSteps.remove(templateStepId)

                val stepTemplateRef = db.collection("steps").document(templateStepId)

                transaction.update(templateRef, "steps", updatedSteps)
                transaction.delete(dailyStepRef)
                transaction.delete(stepTemplateRef)
            }.await()

            Log.d(TAG, "Transaction completed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error in deletion transaction", e)
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun getStep(
        endUserId: String,
        routineId: String,
        stepId: String
    ): Result<Step> {
        return try {
            val document = db.collection("endUser")
                .document(endUserId)
                .collection("dailyRoutines")
                .document(routineId)
                .collection("dailySteps")
                .document(stepId)
                .get()
                .await()

            if (document != null && document.exists()) {
                val stepData = document.toObject(Step::class.java)

                stepData?.let {
                    val stepWithId = it.copy(id = stepId)
                    Result.success(stepWithId)
                } ?: Result.failure(Exception("Failed to parse step data"))
            } else {
                Result.failure(Exception("Step not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading step", e)
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun createStep(
        endUserId: String,
        routineId: String,
        name: String,
        description: String,
        icon: TaskJoyIcon,
        customIconPath: String?
    ): Result<Unit> {
        return try {
            val stepRef = db.collection("steps").document()

            val step = Step(
                name = name,
                image = icon.name,
                description = description,
                completed = false,
                id = stepRef.id,
                customIconPath = if (icon == TaskJoyIcon.CUSTOM) customIconPath else null
            )

            stepRef.set(step).await()

            val dailyStep = step.copy(
                notes = "",
                completed = false,
                completedAt = null,
                id = stepRef.id
            )

            val dailyStepsRef = db.collection("endUser")
                .document(endUserId)
                .collection("dailyRoutines")
                .document(routineId)
                .collection("dailySteps")

            dailyStepsRef.document(dailyStep.id).set(dailyStep).await()

            val dailyRoutineDoc = db.collection("endUser")
                .document(endUserId)
                .collection("dailyRoutines")
                .document(routineId)
                .get()
                .await()

            val dailyRoutine = dailyRoutineDoc.toObject(DailyRoutine::class.java)
            if (dailyRoutine != null) {
                try {
                    db.collection("routineTemplates")
                        .document(dailyRoutine.templateId)
                        .update("steps", FieldValue.arrayUnion(stepRef.id))
                        .await()
                } catch (e: Exception) {
                    Log.w(TAG, "Error updating routineTemplate", e)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating step", e)
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun updateStep(
        endUserId: String,
        routineId: String,
        stepId: String,
        templateStepId: String?,
        name: String,
        description: String,
        icon: TaskJoyIcon,
        customIconPath: String?
    ): Result<Unit> {
        return try {
            val batch = db.batch()

            // Update the template-step
            if (!templateStepId.isNullOrEmpty()) {
                val templateStepRef = db.collection("steps").document(templateStepId)
                val templateStepUpdates = hashMapOf(
                    "name" to name,
                    "description" to description,
                    "image" to icon.name,
                    "customIconPath" to if (icon == TaskJoyIcon.CUSTOM) customIconPath else null
                )
                batch.set(templateStepRef, templateStepUpdates, SetOptions.merge())
            }

            // Update daily-step
            val dailyStepRef = db.collection("endUser")
                .document(endUserId)
                .collection("dailyRoutines")
                .document(routineId)
                .collection("dailySteps")
                .document(stepId)

            val dailyStepUpdates = hashMapOf(
                "name" to name,
                "description" to description,
                "image" to icon.name,
                "customIconPath" to if (icon == TaskJoyIcon.CUSTOM) customIconPath else null
            )
            batch.set(dailyStepRef, dailyStepUpdates, SetOptions.merge())

            // Commit all updates in a batch
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating step", e)
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun markStepAsComplete(
        endUserId: String,
        routineId: String,
        stepId: String
    ): Result<Timestamp> {
        return try {
            val stepRef = db.collection("endUser")
                .document(endUserId)
                .collection("dailyRoutines")
                .document(routineId)
                .collection("dailySteps")
                .document(stepId)

            val currentTime = Timestamp.now()

            stepRef.update(mapOf(
                "completed" to true,
                "completedAt" to currentTime
            )).await()

            Result.success(currentTime)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking step as complete", e)
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun markStepAsIncomplete(
        endUserId: String,
        routineId: String,
        stepId: String
    ): Result<Unit> {
        return try {
            val stepRef = db.collection("endUser")
                .document(endUserId)
                .collection("dailyRoutines")
                .document(routineId)
                .collection("dailySteps")
                .document(stepId)

            stepRef.update(mapOf(
                "completed" to false,
                "completedAt" to null
            )).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking step as incomplete", e)
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun saveStepNotes(
        endUserId: String,
        routineId: String,
        stepId: String,
        notes: String
    ): Result<Unit> {
        return try {
            val stepRef = db.collection("endUser")
                .document(endUserId)
                .collection("dailyRoutines")
                .document(routineId)
                .collection("dailySteps")
                .document(stepId)

            stepRef.update("notes", notes).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving notes", e)
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun completeAllSteps(
        endUserId: String,
        routineId: String
    ): Result<Timestamp> {
        return try {
            val dailyStepsRef = db.collection("endUser")
                .document(endUserId)
                .collection("dailyRoutines")
                .document(routineId)
                .collection("dailySteps")

            val stepsSnapshot = dailyStepsRef.get().await()
            val batch = db.batch()
            val currentTime = Timestamp.now()

            stepsSnapshot.documents.forEach { stepDoc ->
                batch.update(stepDoc.reference, mapOf(
                    "completed" to true,
                    "completedAt" to currentTime
                ))
            }

            batch.commit().await()
            Result.success(currentTime)
        } catch (e: Exception) {
            Log.e(TAG, "Error completing all steps", e)
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}