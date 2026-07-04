package com.example.nhatky.data.dao

import androidx.room.*
import com.example.nhatky.data.model.DiaryEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_entries WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllEntries(userId: String): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE userId = :userId AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') ORDER BY timestamp DESC")
    fun searchEntries(userId: String, query: String): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE id = :id")
    suspend fun getEntryById(id: String): DiaryEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: DiaryEntry)

    @Update
    suspend fun updateEntry(entry: DiaryEntry)

    @Delete
    suspend fun deleteEntry(entry: DiaryEntry)

    @Query("SELECT * FROM diary_entries WHERE isSynced = 0")
    suspend fun getUnsyncedEntries(): List<DiaryEntry>
}
