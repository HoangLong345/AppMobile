package com.example.nhatky.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nhatky.data.model.DiaryEntry
import com.example.nhatky.data.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val repository: DiaryRepository
) : ViewModel() {
    private val _diaries = MutableStateFlow<List<DiaryEntry>>(emptyList())
    val diaries: StateFlow<List<DiaryEntry>> = _diaries

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    fun onSearchQueryChange(query: String, userId: String) {
        _searchQuery.value = query
        loadDiaries(userId)
    }

    fun loadDiaries(userId: String) {
        viewModelScope.launch {
            try {
                repository.getDiaries(userId, _searchQuery.value).collectLatest {
                    _diaries.value = it
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addDiaryWithMedia(
        userId: String,
        title: String,
        content: String,
        mood: String,
        tags: List<String>,
        mediaItems: List<Pair<Uri, Boolean>> // Thay vì một Uri đơn lẻ
    ) {
        viewModelScope.launch {
            try {
                val mediaUrls = mediaItems.map { (uri, isVideo) ->
                    repository.uploadMedia(uri, isVideo)
                }

                val diary = DiaryEntry(
                    userId = userId,
                    title = title,
                    content = content,
                    mood = mood,
                    tags = tags,
                    mediaUrls = mediaUrls
                )
                repository.addDiary(diary)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteDiary(diaryId: String) {
        viewModelScope.launch {
            repository.deleteDiary(diaryId)
        }
    }
}
