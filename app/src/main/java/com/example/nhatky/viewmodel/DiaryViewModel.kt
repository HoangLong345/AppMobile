package com.example.nhatky.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nhatky.data.model.DiaryEntry
import com.example.nhatky.data.repository.DiaryRepository
import com.example.nhatky.ui.utils.MediaHelper // Hãy thay bằng đường dẫn đúng tới thư mục của MediaHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
    private val mediaHelper: MediaHelper // Tiêm MediaHelper thay vì tiêm Context
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
                    .map { diaries ->
                        // Việc groupBy được xử lý tách biệt với collect
                        diaries.groupBy { dateFormatter.format(Date(it.timestamp)) }
                    }
                    .flowOn(Dispatchers.Default) // Chạy tác vụ biến đổi (groupBy) trên Background Thread
                    .catch { e ->
                        _uiState.value = DiaryUiState.Error(e.message ?: "Lỗi tải dữ liệu")
                    }
                    .collect { grouped ->
                        // Collect chạy trên UI Thread (Main)
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

                // Khởi tạo tiến trình Upload song song (Concurrent Upload)
                val uploadDeferreds = imageUris.map { uri ->
                    async {
                        val isVideo = mediaHelper.isVideoUri(uri)
                        Log.d(TAG, "Đang upload: $uri, isVideo: $isVideo")
                        repository.uploadMedia(uri, isVideo)
                    }
                }

                // Chờ tất cả file upload xong cùng lúc
                val uploadResults = uploadDeferreds.awaitAll()

                // Kiểm tra xem có file nào bị lỗi (trả về rỗng) không
                if (uploadResults.any { it.isEmpty() }) {
                    Log.e(TAG, "Có ít nhất một file upload thất bại.")
                    onComplete(false)
                    return@launch
                }

                val newMediaUrls = uploadResults.filter { it.isNotEmpty() }
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

                // Gọi repository và xử lý lỗi thông qua Result
                val result = if (diaryId == null) {
                    repository.addDiary(diary)
                } else {
                    repository.updateDiary(diary)
                }

                if (result.isSuccess) {
                    onComplete(true)
                } else {
                    Log.e(TAG, "Lưu thất bại do lỗi phía Database hoặc Mạng")
                    onComplete(false) // Có thể tuỳ biến để bắn thông báo Toast trên UI
                }

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
            val result = repository.deleteDiary(diaryId)
            if (result.isFailure) {
                // Tuỳ thuộc vào yêu cầu, bạn có thể truyền lỗi xuống UI state ở đây
                Log.e(TAG, "Lỗi không thể xoá nhật ký: ${result.exceptionOrNull()?.message}")
            }
        }
    }
}