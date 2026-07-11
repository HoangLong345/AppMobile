package com.example.nhatky

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.nhatky.data.network.GoogleDriveMediaInterceptor
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class NhatKyApplication : Application(), ImageLoaderFactory, Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var googleDriveMediaInterceptor: GoogleDriveMediaInterceptor

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .respectCacheHeaders(false) // Bỏ qua chặn cache từ máy chủ
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor(googleDriveMediaInterceptor)
                    .addNetworkInterceptor { chain ->
                        val response = chain.proceed(chain.request())
                        response.newBuilder()
                            .header("Cache-Control", "public, max-age=31536000") // Cấp phép sống 1 năm
                            .removeHeader("Pragma")
                            .build()
                    }
                    .build()
            }
            .memoryCache {
                MemoryCache.Builder(this).maxSizePercent(0.25).build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.15) // Tăng dung lượng bộ nhớ
                    .build()
            }
            .crossfade(true)
            .build()
    }
}