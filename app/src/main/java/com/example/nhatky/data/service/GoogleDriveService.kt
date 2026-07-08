package com.example.nhatky.data.service

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
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
            Log.e(TAG, "No Google account signed in.")
            return null
        }
        val credential = GoogleAccountCredential.usingOAuth2(
            context, Collections.singleton(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account

        Log.d(TAG, "Drive Service initialized for account: ${account.email}")
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("NhatKy").build()
    }

    suspend fun uploadMedia(uri: Uri, isVideo: Boolean): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting upload for uri: $uri, isVideo: $isVideo")
            val driveService = getDriveService() ?: return@withContext null
            
            val folderId = getOrCreateFolder(driveService)
            if (folderId == null) {
                Log.e(TAG, "Failed to get or create folder.")
                return@withContext null
            }
            
            val fileMetadata = File().apply {
                name = "media_${System.currentTimeMillis()}.${if (isVideo) "mp4" else "jpg"}"
                parents = listOf(folderId)
            }
            
            val inputStream = context.contentResolver.openInputStream(uri) ?: run {
                Log.e(TAG, "Could not open input stream for uri: $uri")
                return@withContext null
            }
            val mediaContent = FileContent(if (isVideo) "video/mp4" else "image/jpeg", inputStream.use { 
                val tempFile = java.io.File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}")
                tempFile.outputStream().use { output -> it.copyTo(output) }
                tempFile
            })

            val driveFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()
            
            Log.d(TAG, "Upload successful. File ID: ${driveFile.id}")
            return@withContext "googledrive://${driveFile.id}"
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading media: ${e.message}", e)
            return@withContext null
        }
    }

    private fun getOrCreateFolder(driveService: Drive): String? {
        return try {
            val query = "name = '$folderName' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
            Log.d(TAG, "Searching for folder with query: $query")
            val result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id)")
                .execute()
            
            val folder = result.files.firstOrNull()
            if (folder != null) {
                Log.d(TAG, "Found existing folder. ID: ${folder.id}")
                return folder.id
            }
            
            Log.d(TAG, "Folder not found. Creating new folder: $folderName")
            val folderMetadata = File().apply {
                name = folderName
                mimeType = "application/vnd.google-apps.folder"
            }
            
            val newFolder = driveService.files().create(folderMetadata)
                .setFields("id")
                .execute()
            
            Log.d(TAG, "New folder created. ID: ${newFolder.id}")
            newFolder.id
        } catch (e: Exception) {
            Log.e(TAG, "Error in getOrCreateFolder: ${e.message}", e)
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
