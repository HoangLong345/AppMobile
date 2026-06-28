package com.example.nhatky.data.repository

import com.example.nhatky.data.dao.DiaryDao
import com.example.nhatky.data.model.DiaryEntry
import kotlinx.coroutines.flow.Flow

class DiaryRepository(private val diaryDao: DiaryDao) {

    val allEntries: Flow<List<DiaryEntry>> = diaryDao.getAllEntries()

    suspend fun insert(entry: DiaryEntry) {
        diaryDao.insertEntry(entry)
        // TODO: Sync with Firebase Firestore
    }

    suspend fun update(entry: DiaryEntry) {
        diaryDao.updateEntry(entry)
        // TODO: Sync with Firebase Firestore
    }

    suspend fun delete(entry: DiaryEntry) {
        diaryDao.deleteEntry(entry)
        // TODO: Sync with Firebase Firestore
    }

    suspend fun getEntryById(id: Long): DiaryEntry? {
        return diaryDao.getEntryById(id)
    }

    suspend fun syncWithCloud() {
        val unsynced = diaryDao.getUnsyncedEntries()
        unsynced.forEach { entry ->
            try {
                // TODO: Upload to Firestore
                // if successful:
                // diaryDao.updateEntry(entry.copy(isSynced = true))
            } catch (e: Exception) {
                // Log error
            }
        }
    }
}
