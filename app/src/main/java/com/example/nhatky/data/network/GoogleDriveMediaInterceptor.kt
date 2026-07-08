package com.example.nhatky.data.network

import com.example.nhatky.data.service.GoogleDriveService
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveMediaInterceptor @Inject constructor(
    private val googleDriveService: GoogleDriveService
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()

        if (url.startsWith("googledrive://")) {
            val fileId = url.substring("googledrive://".length)
            val driveUrl = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
            
            val token = googleDriveService.getAccessToken()
            val newRequest = request.newBuilder()
                .url(driveUrl)
                .apply {
                    if (token != null) {
                        addHeader("Authorization", "Bearer $token")
                    }
                }
                .build()
            return chain.proceed(newRequest)
        }

        return chain.proceed(request)
    }
}
