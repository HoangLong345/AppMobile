package com.example.nhatky.data.repository

import android.net.Uri
import com.example.nhatky.data.model.DiaryEntry
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class DiaryRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val diaryCollection = firestore.collection("diaries")

    fun getDiaries(userId: String, searchQuery: String = ""): Flow<List<DiaryEntry>> = callbackFlow {
        val subscription = diaryCollection
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val diaries = snapshot.toObjects(DiaryEntry::class.java)
                    // Lọc theo search query (tạm thời lọc ở client cho linh hoạt)
                    val filteredDiaries = if (searchQuery.isBlank()) {
                        diaries
                    } else {
                        diaries.filter { 
                            it.title.contains(searchQuery, ignoreCase = true) || 
                            it.content.contains(searchQuery, ignoreCase = true) ||
                            it.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
                        }
                    }
                    trySend(filteredDiaries)
                }
            }
        awaitClose { subscription.remove() }
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

    suspend fun uploadImage(uri: Uri): String {
        val fileName = "images/${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child(fileName)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }
}
