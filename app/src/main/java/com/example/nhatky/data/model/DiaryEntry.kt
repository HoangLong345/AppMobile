package com.example.nhatky.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class DiaryEntry(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
)
