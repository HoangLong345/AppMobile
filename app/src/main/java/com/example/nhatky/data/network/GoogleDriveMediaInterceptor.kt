package com.example.nhatky.data.network

import android.util.Log
import com.example.nhatky.data.service.GoogleDriveService
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveMediaInterceptor @Inject constructor(
    private val googleDriveService: GoogleDriveService
) : Interceptor {
    private val TAG = "DriveInterceptor"

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()

        if (url.contains("googleapis.com/drive/v3/files/") && url.contains("alt=media")) {
            Log.d(TAG, "Đang chặn Coil để cấp quyền cho link: $url")

            val token = googleDriveService.getAccessToken()

            if (!token.isNullOrEmpty()) {
                Log.d(TAG, "Đã có Token, tiến hành gắn vào Header.")
                val newRequest = request.newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                val response = chain.proceed(newRequest)
                Log.d(TAG, "Kết quả tải từ Google - Mã HTTP Code: ${response.code}")
                return response
            } else {
                Log.e(TAG, "LỖI NẶNG: Không lấy được Access Token! Ảnh sẽ bị trắng.")
            }
        }

        return chain.proceed(request)
    }
}