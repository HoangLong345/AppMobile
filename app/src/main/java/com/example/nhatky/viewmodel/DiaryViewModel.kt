package com.example.nhatky.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.nhatky.data.model.DiaryEntry
import com.example.nhatky.data.repository.DiaryRepository
import com.example.nhatky.ui.utils.MediaHelper
import com.example.nhatky.worker.SyncDiaryWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

sealed class DiaryUiState {
    object Loading : DiaryUiState()
    data class SuccessGrouped(val groupedDiaries: Map<String, List<DiaryEntry>>) : DiaryUiState()
    data class Error(val message: String) : DiaryUiState()
}

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val repository: DiaryRepository,
    private val mediaHelper: MediaHelper,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow<DiaryUiState>(DiaryUiState.Loading)
    val uiState: StateFlow<DiaryUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun onSearchQueryChange(query: String, userId: String) {
        _searchQuery.value = query
        loadDiaries(userId)
    }

    fun loadDiaries(userId: String) {
        viewModelScope.launch {
            _uiState.value = DiaryUiState.Loading
            launch(Dispatchers.IO) { repository.fetchFromCloudToLocal(userId) }
            try {
                repository.getDiaries(userId, _searchQuery.value)
                    .map { diaries -> diaries.groupBy { dateFormatter.format(Date(it.timestamp)) } }
                    .flowOn(Dispatchers.Default)
                    .catch { e -> _uiState.value = DiaryUiState.Error(e.message ?: "Lỗi tải dữ liệu") }
                    .collect { grouped -> _uiState.value = DiaryUiState.SuccessGrouped(grouped) }
            } catch (e: Exception) {
                _uiState.value = DiaryUiState.Error(e.message ?: "Lỗi hệ thống")
            }
        }
    }

    fun addOrUpdateDiary(
        diaryId: String? = null,
        userId: String,
        title: String,
        content: String,
        mood: String,
        tags: List<String>,
        imageUris: List<Uri>,
        existingMediaUrls: List<String> = emptyList(),
        onComplete: (Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                // Copy ảnh vào máy để Worker truy cập an toàn 100%
                val safeLocalUris = imageUris.mapNotNull { uri ->
                    mediaHelper.saveMediaToInternalStorage(uri)?.toString()
                }

                val totalMediaUrls = existingMediaUrls + safeLocalUris
                val existingDiary = if (diaryId != null) repository.getEntryById(diaryId) else null

                val diary = DiaryEntry(
                    id = diaryId ?: UUID.randomUUID().toString(),
                    userId = userId,
                    title = title,
                    content = content,
                    mood = mood,
                    tags = tags,
                    mediaUrls = totalMediaUrls,
                    timestamp = existingDiary?.timestamp ?: System.currentTimeMillis(),
                    isSynced = false
                )

                val result = if (diaryId == null) repository.addDiaryOffline(diary)
                else repository.updateDiaryOffline(diary)

                if (result.isSuccess) {
                    scheduleSyncWork()
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                Log.e("DiaryViewModel", "Lỗi lưu: ${e.message}")
                onComplete(false)
            }
        }
    }

    private fun scheduleSyncWork() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val syncRequest = OneTimeWorkRequestBuilder<SyncDiaryWorker>()
            .setConstraints(constraints).build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "SyncDiaryWork", ExistingWorkPolicy.REPLACE, syncRequest
        )
    }

    suspend fun getDiaryById(diaryId: String): DiaryEntry? {
        return try { repository.getEntryById(diaryId) } catch (_: Exception) { null }
    }

    fun deleteDiary(diaryId: String) {
        viewModelScope.launch { repository.deleteDiary(diaryId) }
    }
}