package com.jarvis.assistant.data.remote

import com.google.gson.annotations.SerializedName

data class OpenWeatherResponse(
    val name: String? = null,
    val main: MainMetrics? = null,
    val weather: List<WeatherCondition>? = null,
    val wind: WindInfo? = null
)

data class MainMetrics(
    val temp: Double? = null,
    @SerializedName("feels_like") val feelsLike: Double? = null,
    val humidity: Int? = null
)

data class WeatherCondition(
    val main: String? = null,
    val description: String? = null
)

data class WindInfo(
    val speed: Double? = null
)

data class OpenMeteoResponse(
    val current: OpenMeteoCurrent? = null
)

data class OpenMeteoCurrent(
    @SerializedName("temperature_2m") val temperature: Double? = null,
    @SerializedName("relative_humidity_2m") val humidity: Int? = null,
    @SerializedName("wind_speed_10m") val windSpeed: Double? = null,
    @SerializedName("weather_code") val weatherCode: Int? = null
)
