package com.example.taskjoy.repository.utils

import com.google.firebase.Timestamp
import java.util.Calendar

object FirebaseUtils {
//    private const val TAG = "FirebaseUtils"

    fun getDayTimestamps(date: Calendar): Pair<Timestamp, Timestamp> {
        val startOfDay = Calendar.getInstance().apply {
            timeInMillis = date.timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val endOfDay = Calendar.getInstance().apply {
            timeInMillis = date.timeInMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time

        return Pair(Timestamp(startOfDay), Timestamp(endOfDay))
    }
//
//    fun commitBatch(
//        batch: WriteBatch,
//        onSuccess: () -> Unit,
//        onError: (Exception) -> Unit
//    ) {
//        batch.commit()
//            .addOnSuccessListener {
//                onSuccess()
//            }
//            .addOnFailureListener { e ->
//                Log.e(TAG, "Error committing batch write: ${e.message}")
//                onError(e)
//            }
//    }
}