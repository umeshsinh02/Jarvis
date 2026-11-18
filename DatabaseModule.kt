package com.jarvis.assistant.di

import android.content.Context
import androidx.room.Room
import com.jarvis.assistant.data.local.ChatDao
import com.jarvis.assistant.data.local.JarvisDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideJarvisDatabase(@ApplicationContext context: Context): JarvisDatabase {
        return Room.databaseBuilder(
            context,
            JarvisDatabase::class.java,
            "jarvis_assistant.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideChatDao(database: JarvisDatabase): ChatDao = database.chatDao()
}
