package com.jarvis.assistant.domain.repository

interface WeatherRepository {
    suspend fun fetchWeatherSummary(city: String, apiKey: String): Result<String>
}
