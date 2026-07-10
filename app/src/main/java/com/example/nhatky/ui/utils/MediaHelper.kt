package com.example.nhatky.ui.utils

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Hàm kiểm tra xem URI có phải là video hay không
     */
    fun isVideoUri(uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        return mimeType?.startsWith("video") == true || uri.toString().lowercase().let {
            it.endsWith(".mp4") || it.contains("video")
        }
    }
}