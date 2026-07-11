package com.example.nhatky.ui.utils

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun isVideoUri(uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        return mimeType?.startsWith("video") == true || uri.toString().lowercase().let {
            it.endsWith(".mp4") || it.contains("video")
        }
    }

    suspend fun saveMediaToInternalStorage(uri: Uri): Uri? = withContext(Dispatchers.IO) {
        try {
            val isVideo = isVideoUri(uri)
            val extension = if (isVideo) ".mp4" else ".jpg"
            val fileName = "offline_media_${System.currentTimeMillis()}$extension"

            // Lưu file an toàn vào bộ nhớ nội bộ của app
            val file = File(context.filesDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return@withContext Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}