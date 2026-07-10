package com.example.nhatky.di

import android.content.Context
import com.example.nhatky.data.dao.DiaryDao
import com.example.nhatky.data.database.AppDatabase
import com.example.nhatky.data.network.GoogleDriveMediaInterceptor
import com.example.nhatky.data.preferences.SettingsManager
import com.example.nhatky.data.repository.DiaryRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideOkHttpClient(interceptor: GoogleDriveMediaInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideDiaryDao(database: AppDatabase): DiaryDao {
        return database.diaryDao()
    }

    @Provides
    @Singleton
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager(context)
    }

    @Provides
    @Singleton
    fun provideDiaryRepository(
        diaryDao: DiaryDao,
        firestore: FirebaseFirestore,
        googleDriveService: com.example.nhatky.data.service.GoogleDriveService
    ): DiaryRepository {
        return DiaryRepository(diaryDao, firestore, googleDriveService)
    }
}
