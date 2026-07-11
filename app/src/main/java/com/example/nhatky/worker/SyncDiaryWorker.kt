package com.example.nhatky.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.nhatky.data.repository.DiaryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class SyncDiaryWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: DiaryRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("SyncDiaryWorker", "Bắt đầu tiến trình đồng bộ ngầm!")

            val unsyncedEntries = repository.getUnsyncedEntries()
            if (unsyncedEntries.isEmpty()) return@withContext Result.success()

            for (entry in unsyncedEntries) {
                val finalMediaUrls = mutableListOf<String>()

                for (url in entry.mediaUrls) {
                    if (url.startsWith("file://") || url.startsWith("content://")) {
                        Log.d("SyncDiaryWorker", "Đang upload file cục bộ: $url")

                        val isVideo = appContext.contentResolver.getType(Uri.parse(url))?.startsWith("video") == true
                                || url.endsWith(".mp4")

                        val driveUrl = repository.uploadMedia(Uri.parse(url), isVideo)

                        if (driveUrl.isNotEmpty()) finalMediaUrls.add(driveUrl)
                        else finalMediaUrls.add(url)
                    } else {
                        finalMediaUrls.add(url)
                    }
                }

                val hasPendingLocalFiles = finalMediaUrls.any { it.startsWith("file://") || it.startsWith("content://") }

                if (!hasPendingLocalFiles) {
                    val entryToSync = entry.copy(mediaUrls = finalMediaUrls)
                    repository.syncSingleEntryToCloud(entryToSync)
                    Log.d("SyncDiaryWorker", "Đồng bộ thành công: ${entry.title}")
                } else {
                    Log.d("SyncDiaryWorker", "Upload lỗi, sẽ thử lại sau.")
                    return@withContext Result.retry()
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncDiaryWorker", "Lỗi đồng bộ: ${e.message}")
            Result.retry()
        }
    }
}