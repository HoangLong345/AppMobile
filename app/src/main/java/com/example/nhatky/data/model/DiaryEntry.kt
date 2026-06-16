package com.example.nhatky.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class DiaryEntry(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val mood: String = "Normal", // Vui, Buồn, Bình thường, v.v.
    val tags: List<String> = emptyList(),
    val mediaUrls: List<String> = emptyList(),
    @ServerTimestamp
    val timestamp: Date? = null
)
