package com.example.nhatky.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nhatky.data.model.DiaryEntry
import com.example.nhatky.data.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DiaryUiState {
    object Loading : DiaryUiState()
    data class Success(val diaries: List<DiaryEntry>) : DiaryUiState()
    data class Error(val message: String) : DiaryUiState()
}

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val repository: DiaryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<DiaryUiState>(DiaryUiState.Loading)
    val uiState: StateFlow<DiaryUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

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
                        _uiState.value = DiaryUiState.Error(e.message ?: "Unknown error")
                    }
                    .collect { diaries ->
                        _uiState.value = DiaryUiState.Success(diaries)
                    }
            } catch (e: Exception) {
                _uiState.value = DiaryUiState.Error(e.message ?: "Unknown error")
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
                val newMediaUrls = imageUris.map { uri ->
                    // Đơn giản hóa: Coi mọi thứ là ảnh nếu không có phần mở rộng video
                    val isVideo = uri.toString().lowercase().let { 
                        it.endsWith(".mp4") || it.contains("video") 
                    }
                    repository.uploadMedia(uri, isVideo)
                }
                val totalMediaUrls = existingMediaUrls + newMediaUrls
                
                val diary = DiaryEntry(
                    id = diaryId ?: "",
                    userId = userId,
                    title = title,
                    content = content,
                    mood = mood,
                    tags = tags,
                    mediaUrls = totalMediaUrls,
                )
                
                if (diaryId == null) {
                    repository.addDiary(diary)
                } else {
                    repository.updateDiary(diary)
                }
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    suspend fun getDiaryById(diaryId: String): DiaryEntry? {
        return try {
            repository.getDiaryById(diaryId)
        } catch (_: Exception) {
            null
        }
    }

    fun deleteDiary(diaryId: String) {
        viewModelScope.launch {
            try {
                repository.deleteDiary(diaryId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
