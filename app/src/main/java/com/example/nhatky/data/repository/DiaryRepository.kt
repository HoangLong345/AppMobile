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

    suspend fun addDiaryOffline(diary: DiaryEntry): Result<Unit> {
        return try {
            val id = if (diary.id.isEmpty()) UUID.randomUUID().toString() else diary.id
            val newDiary = diary.copy(id = id, isSynced = false)
            diaryDao.insertEntry(newDiary)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi addDiaryOffline: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateDiaryOffline(diary: DiaryEntry): Result<Unit> {
        return try {
            diaryDao.updateEntry(diary.copy(isSynced = false))
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi updateDiaryOffline: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteDiary(diaryId: String): Result<Unit> {
        return try {
            val entry = diaryDao.getEntryById(diaryId)
            if (entry != null) {
                diaryDao.deleteEntry(entry)
            }
            try { diaryCollection.document(diaryId).delete().await() } catch (_: Exception) {}
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi deleteDiary: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun uploadMedia(uri: Uri, isVideo: Boolean): String {
        return try {
            googleDriveService.uploadMedia(uri, isVideo) ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi uploadMedia: ${e.message}")
            ""
        }
    }

    suspend fun getUnsyncedEntries(): List<DiaryEntry> {
        return diaryDao.getUnsyncedEntries()
    }

    suspend fun syncSingleEntryToCloud(entry: DiaryEntry) {
        val entrySynced = entry.copy(isSynced = true)
        diaryCollection.document(entry.id).set(entrySynced).await()
        diaryDao.updateEntry(entrySynced)
    }

    suspend fun fetchFromCloudToLocal(userId: String) {
        try {
            val snapshot = diaryCollection.whereEqualTo("userId", userId).get().await()
            val cloudEntries = snapshot.documents.mapNotNull { it.toObject(DiaryEntry::class.java) }
            cloudEntries.forEach { entry ->
                diaryDao.insertEntry(entry.copy(isSynced = true))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi fetchFromCloud: ${e.message}", e)
        }
    }
}