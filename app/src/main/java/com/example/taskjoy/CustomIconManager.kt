package com.example.taskjoy

import android.content.Context
import android.net.Uri
import com.example.taskjoy.model.CustomIcon
import java.io.File
import java.io.FileOutputStream

class CustomIconManager(private val context: Context) {
    private val customIconsDir = File(context.filesDir, "custom_icons")

    init {
        customIconsDir.mkdirs()
    }

    fun saveIcon(uri: Uri): CustomIcon? {
        try {
            val timestamp = System.currentTimeMillis()
            val filename = "icon_$timestamp.jpg"
            val file = File(customIconsDir, filename)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }

            return CustomIcon(timestamp, file.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun deleteIcon(customIcon: CustomIcon) {
        File(customIcon.filepath).delete()
    }

    fun getAllIcons(): List<CustomIcon> {
        return customIconsDir.listFiles()?.map { file ->
            CustomIcon(
                file.nameWithoutExtension.substringAfter("icon_").toLong(),
                file.absolutePath
            )
        }?.sortedByDescending { it.timestamp } ?: emptyList()
    }
}