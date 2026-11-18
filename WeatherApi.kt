package com.jarvis.assistant.data.remote

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface WeatherApi {
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): OpenWeatherResponse

    @GET
    suspend fun getOpenMeteoWeather(
        @Url fullUrl: String =
            "https://api.open-meteo.com/v1/forecast?latitude=19.0760&longitude=72.8777&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code"
    ): OpenMeteoResponse
}
