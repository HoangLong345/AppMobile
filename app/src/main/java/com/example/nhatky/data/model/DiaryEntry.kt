package com.example.nhatky.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val mood: String = "Bình thường",
    val tags: List<String> = emptyList(),
    val mediaUrls: List<String> = emptyList(),
    val isSynced: Boolean = false
)
