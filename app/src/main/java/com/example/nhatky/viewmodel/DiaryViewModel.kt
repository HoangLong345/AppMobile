package com.example.nhatky.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nhatky.data.model.DiaryEntry
import com.example.nhatky.data.repository.DiaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DiaryViewModel(private val repository: DiaryRepository = DiaryRepository()) : ViewModel() {
    private val _diaries = MutableStateFlow<List<DiaryEntry>>(emptyList())
    val diaries: StateFlow<List<DiaryEntry>> = _diaries

    fun loadDiaries(userId: String) {
        viewModelScope.launch {
            try {
                repository.getDiaries(userId).collectLatest {
                    _diaries.value = it
                }
            } catch (e: Exception) {
                // Handle error (e.g., show a toast or log it)
                e.printStackTrace()
            }
        }
    }

    fun addDiary(userId: String, title: String, content: String) {
        viewModelScope.launch {
            val diary = DiaryEntry(userId = userId, title = title, content = content)
            repository.addDiary(diary)
        }
    }

    fun deleteDiary(diaryId: String) {
        viewModelScope.launch {
            repository.deleteDiary(diaryId)
        }
    }
}
