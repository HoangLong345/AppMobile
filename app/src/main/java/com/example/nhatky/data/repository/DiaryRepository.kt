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
    suspend fun getDiaryById(diaryId: String): DiaryEntry? {
        val snapshot = diaryCollection.document(diaryId).get().await()
        return snapshot.toObject(DiaryEntry::class.java)
    }

    suspend fun addDiary(diary: DiaryEntry) {
        val docRef = diaryCollection.document()
        val newDiary = diary.copy(id = docRef.id)
        docRef.set(newDiary).await()
    }

    suspend fun updateDiary(diary: DiaryEntry) {
        diaryCollection.document(diary.id).set(diary).await()
    }

    suspend fun deleteDiary(diaryId: String) {
        diaryCollection.document(diaryId).delete().await()
    }

    suspend fun uploadMedia(uri: Uri, isVideo: Boolean): String {
        val extension = if (isVideo) "mp4" else "jpg"
        val folder = if (isVideo) "videos" else "images"
        val fileName = "$folder/${UUID.randomUUID()}.$extension"

        val ref = storage.reference.child(fileName)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }
}
