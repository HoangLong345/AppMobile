package com.example.nhatky.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val timestamp: Long,
    val images: List<String>,
    val videoUrl: String?,
    val mood: String,
    val isSynced: Boolean = false
)
