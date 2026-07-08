package com.example.nhatky.data.service

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.InputStreamContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "GoogleDriveService"
    private val folderName = "NhatKy_Media"

    private fun getDriveService(): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            Log.e(TAG, "Không tìm thấy tài khoản Google nào đã đăng nhập.")
            return null
        }
        val credential = GoogleAccountCredential.usingOAuth2(
            context, Collections.singleton(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("NhatKy").build()
    }

    suspend fun uploadMedia(uri: Uri, isVideo: Boolean): String? = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService() ?: return@withContext null

            val folderId = getOrCreateFolder(driveService)
            if (folderId == null) {
                return@withContext null
            }

            val fileMetadata = File().apply {
                name = "media_${System.currentTimeMillis()}.${if (isVideo) "mp4" else "jpg"}"
                parents = listOf(folderId)
            }

            val mimeType = if (isVideo) "video/mp4" else "image/jpeg"

            return@withContext context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val mediaContent = InputStreamContent(mimeType, inputStream)

                val driveFile = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()

                // Gắn thêm đuôi file vào URL trả về
                val extension = if (isVideo) ".mp4" else ".jpg"

                Log.d(TAG, "Upload thành công. File ID: ${driveFile.id}$extension")
                "googledrive://${driveFile.id}$extension"
            } ?: run {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi upload media: ${e.message}", e)
            return@withContext null
        }
    }

    private fun getOrCreateFolder(driveService: Drive): String? {
        return try {
            val query = "name = '$folderName' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
            val result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id)")
                .execute()

            val folder = result.files.firstOrNull()
            if (folder != null) return folder.id

            val folderMetadata = File().apply {
                name = folderName
                mimeType = "application/vnd.google-apps.folder"
            }

            val newFolder = driveService.files().create(folderMetadata)
                .setFields("id")
                .execute()

            newFolder.id
        } catch (e: Exception) {
            null
        }
    }

    fun getAccessToken(): String? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(
            context, Collections.singleton(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account

        return try {
            credential.token
        } catch (e: Exception) {
            null
        }
    }
}