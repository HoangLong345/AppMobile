package com.example.nhatky.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nhatky.data.model.DiaryEntry
import com.example.nhatky.data.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

sealed class DiaryUiState {
    object Loading : DiaryUiState()
    data class Success(val diaries: List<DiaryEntry>) : DiaryUiState()
    data class SuccessGrouped(val groupedDiaries: Map<String, List<DiaryEntry>>) : DiaryUiState()
    data class Error(val message: String) : DiaryUiState()
}

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val repository: DiaryRepository,
    @ApplicationContext private val context: Context // Tiêm Context để lấy ContentResolver
) : ViewModel() {
    private val TAG = "DiaryViewModel"
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
            try {
                repository.getDiaries(userId, _searchQuery.value)
                    .catch { e ->
                        _uiState.value = DiaryUiState.Error(e.message ?: "Lỗi không xác định")
                    }
                    .collect { diaries ->
                        val grouped = diaries.groupBy { dateFormatter.format(Date(it.timestamp)) }
                        _uiState.value = DiaryUiState.SuccessGrouped(grouped)
                    }
            } catch (e: Exception) {
                _uiState.value = DiaryUiState.Error(e.message ?: "Lỗi không xác định")
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
                Log.d(TAG, "addOrUpdateDiary: Bắt đầu. Số lượng file: ${imageUris.size}")

                val newMediaUrls = mutableListOf<String>()
                var isUploadFailed = false

                for (uri in imageUris) {
                    // Dùng ContentResolver để hỏi Hệ điều hành Android chính xác định dạng file
                    val mimeType = context.contentResolver.getType(uri)
                    val isVideo = mimeType?.startsWith("video") == true || uri.toString().lowercase().let {
                        it.endsWith(".mp4") || it.contains("video")
                    }

                    Log.d(TAG, "addOrUpdateDiary: Đang upload: $uri, isVideo: $isVideo")

                    val result = repository.uploadMedia(uri, isVideo)

                    if (result.isNullOrEmpty()) {
                        Log.e(TAG, "Upload thất bại cho uri: $uri")
                        isUploadFailed = true
                        break
                    } else {
                        Log.d(TAG, "Upload thành công: $result")
                        newMediaUrls.add(result)
                    }
                }

                if (isUploadFailed) {
                    onComplete(false)
                    return@launch
                }

                val totalMediaUrls = existingMediaUrls + newMediaUrls
                val existingDiary = if (diaryId != null) repository.getEntryById(diaryId) else null

                val diary = DiaryEntry(
                    id = diaryId ?: UUID.randomUUID().toString(),
                    userId = userId,
                    title = title,
                    content = content,
                    mood = mood,
                    tags = tags,
                    mediaUrls = totalMediaUrls,
                    timestamp = existingDiary?.timestamp ?: System.currentTimeMillis()
                )

                if (diaryId == null) {
                    repository.addDiary(diary)
                } else {
                    repository.updateDiary(diary)
                }
                onComplete(true)

            } catch (e: Exception) {
                Log.e(TAG, "Lỗi Catch trong addOrUpdateDiary: ${e.message}", e)
                onComplete(false)
            }
        }
    }

    suspend fun getDiaryById(diaryId: String): DiaryEntry? {
        return try { repository.getEntryById(diaryId) } catch (_: Exception) { null }
    }

    fun deleteDiary(diaryId: String) {
        viewModelScope.launch {
            try { repository.deleteDiary(diaryId) } catch (e: Exception) { e.printStackTrace() }
        }
    }
}