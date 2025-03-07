package com.example.taskjoy.repository

import android.util.Log
import com.example.taskjoy.model.DailyRoutine
import com.example.taskjoy.model.RoutineTemplate
import com.example.taskjoy.model.Step
import com.example.taskjoy.repository.utils.FirebaseUtils
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Calendar

/**
 * Firebase implementation of the RoutineRepository interface
 */
class FirebaseRoutineRepository(
    private val db: FirebaseFirestore = Firebase.firestore
) : RoutineRepository {
    private val TAG = "FirebaseRoutineRepo"

    override fun createDailyRoutinesIfNeeded(
        endUserId: String,
        date: Calendar,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val (startTimestamp, endTimestamp) = FirebaseUtils.getDayTimestamps(date)

        db.collection("endUser")
            .document(endUserId)
            .collection("dailyRoutines")
            .whereGreaterThanOrEqualTo("date", startTimestamp)
            .whereLessThanOrEqualTo("date", endTimestamp)
            .get()
            .addOnSuccessListener { dailyRoutines ->
                if (dailyRoutines.isEmpty) {
                    createDailyRoutinesFromTemplates(endUserId, date, onSuccess, onError)
                } else {
                    onSuccess() // Routines already exist
                }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error checking daily routines", error)
                onError(error)
            }
    }

    private fun createDailyRoutinesFromTemplates(
        endUserId: String,
        date: Calendar,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("endUser")
            .document(endUserId)
            .get()
            .addOnSuccessListener { endUserDoc ->
                val templateIds = endUserDoc.get("routineTemplates") as? List<*>
                if (templateIds.isNullOrEmpty()) {
                    Log.w(TAG, "No routine templates available")
                    onSuccess() // No templates to create routines from
                    return@addOnSuccessListener
                }

                db.collection("routineTemplates")
                    .whereIn(FieldPath.documentId(), templateIds)
                    .get()
                    .addOnSuccessListener { templates ->
                        // Create a single batch for all operations
                        val batch = db.batch()

                        // Use a counter to track when all steps are processed
                        var templatesProcessed = 0
                        val totalTemplates = templates.size()

                        templates.forEach { template ->
                            val templateData = template.toObject(RoutineTemplate::class.java)

                            // Create daily routine document reference
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

                            // If there are no steps, increment counter
                            if (templateData.steps.isEmpty()) {
                                templatesProcessed++
                                if (templatesProcessed == totalTemplates) {
                                    // Commit batch when all templates are processed
                                    FirebaseUtils.commitBatch(batch, onSuccess, onError)
                                }
                                return@forEach
                            }

                            // Fetch all steps for this template
                            val stepFetches = templateData.steps.map { stepId ->
                                db.collection("steps").document(stepId).get()
                            }

                            Tasks.whenAllSuccess<DocumentSnapshot>(stepFetches)
                                .addOnSuccessListener { stepDocs ->
                                    // Add all steps to the batch
                                    stepDocs.forEachIndexed { index, stepDoc ->
                                        val stepData = stepDoc.toObject(Step::class.java)
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

                                    templatesProcessed++
                                    if (templatesProcessed == totalTemplates) {
                                        // Commit batch when all templates are processed
                                        FirebaseUtils.commitBatch(batch, onSuccess, onError)
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error fetching step documents: ${e.message}")
                                    onError(e)
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error fetching routine templates: ${e.message}")
                        onError(e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching end user document: ${e.message}")
                onError(e)
            }
    }

    override fun getDailyRoutines(
        endUserId: String,
        date: Calendar,
        onSuccess: (List<DailyRoutine>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val (startTimestamp, endTimestamp) = FirebaseUtils.getDayTimestamps(date)

        db.collection("endUser")
            .document(endUserId)
            .collection("dailyRoutines")
            .whereGreaterThanOrEqualTo("date", startTimestamp)
            .whereLessThanOrEqualTo("date", endTimestamp)
            .get()
            .addOnSuccessListener { dailyRoutines ->
                val routineList = mutableListOf<DailyRoutine>()

                for (routineDoc in dailyRoutines) {
                    val routine = routineDoc.toObject(DailyRoutine::class.java).apply {
                        id = routineDoc.id
                    }
                    routineList.add(routine)
                }

                onSuccess(routineList)
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error getting daily routines", error)
                onError(error)
            }
    }

    override fun getAllEndUserDailyRoutines(
        parentId: String,
        date: Calendar,
        onSuccess: (List<DailyRoutine>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("parents").document(parentId)
            .get()
            .addOnSuccessListener { parentDoc ->
                val childrenIds = parentDoc.get("children") as? List<String>

                if (childrenIds.isNullOrEmpty()) {
                    onSuccess(emptyList())
                    return@addOnSuccessListener
                }

                // Keep track of how many children we've processed
                var processedCount = 0
                val allRoutines = mutableListOf<DailyRoutine>()

                childrenIds.forEach { childId ->
                    getDailyRoutines(
                        childId,
                        date,
                        onSuccess = { childRoutines ->
                            allRoutines.addAll(childRoutines)
                            processedCount++

                            // When all children are processed, return the combined list
                            if (processedCount == childrenIds.size) {
                                onSuccess(allRoutines)
                            }
                        },
                        onError = { error ->
                            // Continue with other children even if one fails
                            Log.e(TAG, "Error getting routines for child $childId", error)
                            processedCount++

                            // When all children are processed, return what we have
                            if (processedCount == childrenIds.size) {
                                onSuccess(allRoutines)
                            }
                        }
                    )
                }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error getting parent data", error)
                onError(error)
            }
    }

    override fun deleteRoutine(
        endUserId: String,
        routine: DailyRoutine,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        Log.d(TAG, "Starting deletion for routine: ${routine.id}, templateId: ${routine.templateId}")

        // Reference to all needed documents
        val dailyRoutineRef = db.collection("endUser")
            .document(endUserId)
            .collection("dailyRoutines")
            .document(routine.id)

        val routineTemplateRef = db.collection("routineTemplates")
            .document(routine.templateId)

        // First get all the dailySteps
        dailyRoutineRef.collection("dailySteps")
            .get()
            .addOnSuccessListener { dailyStepsSnapshot ->
                // Now start the transaction with the dailySteps data
                db.runTransaction { transaction ->
                    // 1. READS FIRST
                    // Get endUser document
                    val endUserRef = db.collection("endUser").document(endUserId)
                    val endUserDoc = transaction.get(endUserRef)

                    // Get routine template to check its steps
                    val templateDoc = transaction.get(routineTemplateRef)

                    // 2. PROCESS DATA
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

                    // 3. WRITES SECOND
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
                }.addOnSuccessListener {
                    Log.d(TAG, "Successfully deleted routine and all related documents")
                    onSuccess()
                }.addOnFailureListener { error ->
                    Log.e(TAG, "Error in deletion transaction", error)
                    onError(error)
                }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error getting dailySteps", error)
                onError(error)
            }
    }
}