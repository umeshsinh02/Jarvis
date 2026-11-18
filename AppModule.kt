package com.jarvis.assistant.di

import com.jarvis.assistant.data.repository.ChatRepositoryImpl
import com.jarvis.assistant.data.repository.WeatherRepositoryImpl
import com.jarvis.assistant.domain.repository.ChatRepository
import com.jarvis.assistant.domain.repository.WeatherRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindWeatherRepository(impl: WeatherRepositoryImpl): WeatherRepository
}
