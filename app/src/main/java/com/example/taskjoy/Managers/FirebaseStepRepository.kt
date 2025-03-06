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
import com.google.firebase.ktx.Firebase

/**
 * Firebase implementation of the StepRepository interface
 */
class FirebaseStepRepository(
    private val db: FirebaseFirestore = Firebase.firestore
) : StepRepository {
    private val TAG = "FirebaseStepRepository"

    override fun getRoutineWithSteps(
        endUserId: String,
        routineId: String,
        onSuccess: (DailyRoutine, List<Step>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("endUser").document(endUserId).collection("dailyRoutines")
            .document(routineId)
            .get()
            .addOnSuccessListener { routineDoc ->
                Log.d(TAG, "Routine document fetched: ${routineDoc.data}")
                val routine = routineDoc.toObject(DailyRoutine::class.java) ?: DailyRoutine()

                // Fetch and sort steps by order
                db.collection("endUser").document(endUserId).collection("dailyRoutines")
                    .document(routineId).collection("dailySteps")
                    .orderBy("order", Query.Direction.ASCENDING)
                    .get()
                    .addOnSuccessListener { stepDocs ->
                        Log.d(TAG, "Steps fetched: ${stepDocs.size()} steps")
                        val steps = mutableListOf<Step>()
                        for (stepDoc in stepDocs) {
                            val step = stepDoc.toObject(Step::class.java)
                            steps.add(step)
                        }
                        onSuccess(routine, steps)
                    }
                    .addOnFailureListener { error ->
                        Log.e(TAG, "Error getting steps", error)
                        onError(error)
                    }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error getting routine", error)
                onError(error)
            }
    }

    override fun saveStepOrder(
        endUserId: String,
        routineId: String,
        steps: List<Step>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
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
        }.addOnSuccessListener {
            Log.d(TAG, "Successfully saved step order")
            onSuccess()
        }.addOnFailureListener { error ->
            Log.e(TAG, "Error saving step order", error)
            onError(error)
        }
    }

    override fun updateRemainingStepsOrder(
        endUserId: String,
        routineId: String,
        steps: List<Step>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
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

        batch.commit()
            .addOnSuccessListener {
                Log.d(TAG, "Successfully updated step order")
                onSuccess()
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error updating step order", error)
                onError(error)
            }
    }

    override fun deleteStep(
        endUserId: String,
        routineId: String,
        step: Step,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
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
            // If no template step ID, just delete the daily step
            dailyStepRef.delete()
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { error ->
                    Log.e(TAG, "Error deleting step", error)
                    onError(error)
                }
            return
        }

        db.runTransaction { transaction ->
            // Handle template step deletion
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

        }.addOnSuccessListener {
            Log.d(TAG, "Transaction completed successfully")
            onSuccess()
        }.addOnFailureListener { error ->
            Log.e(TAG, "Error in deletion transaction", error)
            onError(error)
        }
    }

    override fun getStep(
        endUserId: String,
        routineId: String,
        stepId: String,
        onSuccess: (Step) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("endUser")
            .document(endUserId)
            .collection("dailyRoutines")
            .document(routineId)
            .collection("dailySteps")
            .document(stepId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val step = document.toObject(Step::class.java)
                    step?.let {
                        onSuccess(it)
                    } ?: run {
                        onError(Exception("Failed to parse step data"))
                    }
                } else {
                    onError(Exception("Step not found"))
                }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error loading step", error)
                onError(error)
            }
    }

    override fun createStep(
        endUserId: String,
        routineId: String,
        name: String,
        description: String,
        icon: TaskJoyIcon,
        customIconPath: String?,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val stepRef = db.collection("steps").document()

        val step = Step(
            name = name,
            image = icon.name,
            description = description,
            completed = false,
            id = stepRef.id,
            customIconPath = if (icon == TaskJoyIcon.CUSTOM) customIconPath else null
        )

        stepRef.set(step)
            .addOnSuccessListener {
                // Add to the dailySteps subcollection with a unique ID
                val dailyStep = step.copy(
                    notes = "", // Notes are specific to daily steps
                    completed = false,
                    completedAt = null,
                    id = stepRef.id // Reuse step ID for simplicity
                )

                val dailyStepsRef = db.collection("endUser")
                    .document(endUserId)
                    .collection("dailyRoutines")
                    .document(routineId)
                    .collection("dailySteps")

                dailyStepsRef.document(dailyStep.id).set(dailyStep)
                    .addOnSuccessListener {
                        // Update the routineTemplate with the reference to the new step
                        db.collection("endUser")
                            .document(endUserId)
                            .collection("dailyRoutines")
                            .document(routineId)
                            .get()
                            .addOnSuccessListener { dailyRoutineDoc ->
                                val dailyRoutine = dailyRoutineDoc.toObject(DailyRoutine::class.java)
                                if (dailyRoutine != null) {
                                    db.collection("routineTemplates")
                                        .document(dailyRoutine.templateId)
                                        .update("steps", FieldValue.arrayUnion(stepRef.id))
                                        .addOnSuccessListener {
                                            onSuccess()
                                        }
                                        .addOnFailureListener { error ->
                                            Log.w(TAG, "Error updating routineTemplate", error)
                                            onSuccess() // Still consider it a success since daily step is created
                                        }
                                } else {
                                    Log.w(TAG, "Failed to retrieve DailyRoutine document")
                                    onSuccess() // Still consider it a success since daily step is created
                                }
                            }
                            .addOnFailureListener { error ->
                                Log.w(TAG, "Error getting DailyRoutine document", error)
                                onSuccess() // Still consider it a success since daily step is created
                            }
                    }
                    .addOnFailureListener { error ->
                        Log.e(TAG, "Error adding step to dailySteps", error)
                        onError(error)
                    }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error creating step", error)
                onError(error)
            }
    }

    override fun updateStep(
        endUserId: String,
        routineId: String,
        stepId: String,
        templateStepId: String?,
        name: String,
        description: String,
        icon: TaskJoyIcon,
        customIconPath: String?,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val batch = db.batch()

        // Update the template step if it exists
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

        // Update the daily step
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
        batch.commit()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error updating step", error)
                onError(error)
            }
    }

    override fun markStepAsComplete(
        endUserId: String,
        routineId: String,
        stepId: String,
        onSuccess: (Timestamp) -> Unit,
        onError: (Exception) -> Unit
    ) {
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
        )).addOnSuccessListener {
            onSuccess(currentTime)
        }.addOnFailureListener { error ->
            Log.e(TAG, "Error marking step as complete", error)
            onError(error)
        }
    }

    override fun markStepAsIncomplete(
        endUserId: String,
        routineId: String,
        stepId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val stepRef = db.collection("endUser")
            .document(endUserId)
            .collection("dailyRoutines")
            .document(routineId)
            .collection("dailySteps")
            .document(stepId)

        stepRef.update(mapOf(
            "completed" to false,
            "completedAt" to null
        )).addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { error ->
            Log.e(TAG, "Error marking step as incomplete", error)
            onError(error)
        }
    }

    override fun saveStepNotes(
        endUserId: String,
        routineId: String,
        stepId: String,
        notes: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val stepRef = db.collection("endUser")
            .document(endUserId)
            .collection("dailyRoutines")
            .document(routineId)
            .collection("dailySteps")
            .document(stepId)

        stepRef.update("notes", notes)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error saving notes", error)
                onError(error)
            }
    }

    override fun completeAllSteps(
        endUserId: String,
        routineId: String,
        onSuccess: (Timestamp) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val dailyStepsRef = db.collection("endUser")
            .document(endUserId)
            .collection("dailyRoutines")
            .document(routineId)
            .collection("dailySteps")

        dailyStepsRef.get()
            .addOnSuccessListener { stepsSnapshot ->
                val batch = db.batch()
                val currentTime = Timestamp.now()

                stepsSnapshot.documents.forEach { stepDoc ->
                    batch.update(stepDoc.reference, mapOf(
                        "completed" to true,
                        "completedAt" to currentTime
                    ))
                }

                batch.commit()
                    .addOnSuccessListener {
                        onSuccess(currentTime)
                    }
                    .addOnFailureListener { error ->
                        Log.e(TAG, "Error completing steps", error)
                        onError(error)
                    }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error fetching steps to complete", error)
                onError(error)
            }
    }
}