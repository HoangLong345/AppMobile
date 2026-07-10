package com.example.nhatky.data.repository

import android.net.Uri
import android.util.Log
import com.example.nhatky.data.dao.DiaryDao
import com.example.nhatky.data.model.DiaryEntry
import com.example.nhatky.data.service.GoogleDriveService
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiaryRepository @Inject constructor(
    private val diaryDao: DiaryDao,
    private val firestore: FirebaseFirestore,
    private val googleDriveService: GoogleDriveService
) {
    private val TAG = "DiaryRepository"
    private val diaryCollection = firestore.collection("diaries")

    fun getDiaries(userId: String, query: String = ""): Flow<List<DiaryEntry>> {
        return if (query.isEmpty()) {
            diaryDao.getAllEntries(userId)
        } else {
            diaryDao.searchEntries(userId, query)
        }
    }

    suspend fun getEntryById(id: String): DiaryEntry? {
        return diaryDao.getEntryById(id)
    }

    // Trả về Result để ViewModel bắt được lỗi
    suspend fun addDiary(diary: DiaryEntry): Result<Unit> {
        return try {
            val id = if (diary.id.isEmpty()) UUID.randomUUID().toString() else diary.id
            val newDiary = diary.copy(id = id)

            // Save locally first
            diaryDao.insertEntry(newDiary)

            // Try to sync with Firestore
            diaryCollection.document(id).set(newDiary.copy(isSynced = true)).await()
            diaryDao.updateEntry(newDiary.copy(isSynced = true))

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi addDiary: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Trả về Result
    suspend fun updateDiary(diary: DiaryEntry): Result<Unit> {
        return try {
            diaryDao.updateEntry(diary.copy(isSynced = false))

            diaryCollection.document(diary.id).set(diary.copy(isSynced = true)).await()
            diaryDao.updateEntry(diary.copy(isSynced = true))

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi updateDiary: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Trả về Result
    suspend fun deleteDiary(diaryId: String): Result<Unit> {
        return try {
            val entry = diaryDao.getEntryById(diaryId)
            if (entry != null) {
                diaryDao.deleteEntry(entry)
            }

            diaryCollection.document(diaryId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi deleteDiary: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun uploadMedia(uri: Uri, isVideo: Boolean): String {
        Log.d(TAG, "uploadMedia: Requesting GoogleDriveService for $uri")
        val result = googleDriveService.uploadMedia(uri, isVideo) ?: ""
        Log.d(TAG, "uploadMedia: GoogleDriveService returned $result")
        return result
    }

    suspend fun syncWithCloud() {
        val unsynced = diaryDao.getUnsyncedEntries()
        unsynced.forEach { entry ->
            try {
                diaryCollection.document(entry.id).set(entry.copy(isSynced = true)).await()
                diaryDao.updateEntry(entry.copy(isSynced = true))
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi syncWithCloud cho mục ${entry.id}: ${e.message}")
            }
        }
    }
}