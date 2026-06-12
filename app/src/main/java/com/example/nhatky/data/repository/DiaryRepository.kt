package com.example.nhatky.data.repository

import com.example.nhatky.data.model.DiaryEntry
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class DiaryRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val diaryCollection = firestore.collection("diaries")

    fun getDiaries(userId: String): Flow<List<DiaryEntry>> = callbackFlow {
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
                    trySend(diaries)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun addDiary(diary: DiaryEntry) {
        val docRef = diaryCollection.document()
        val newDiary = diary.copy(id = docRef.id)
        docRef.set(newDiary).await()
    }

    suspend fun deleteDiary(diaryId: String) {
        diaryCollection.document(diaryId).delete().await()
    }
}
