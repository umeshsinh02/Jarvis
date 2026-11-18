package com.jarvis.assistant.data.repository

import com.jarvis.assistant.data.remote.WeatherApi
import com.jarvis.assistant.domain.repository.WeatherRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class WeatherRepositoryImpl @Inject constructor(
    private val weatherApi: WeatherApi
) : WeatherRepository {

    override suspend fun fetchWeatherSummary(city: String, apiKey: String): Result<String> {
        return runCatching {
            if (apiKey.isNotBlank()) {
                val res = weatherApi.getCurrentWeather(city = city, apiKey = apiKey)
                val temp = res.main?.temp?.roundToInt() ?: 0
                val feels = res.main?.feelsLike?.roundToInt() ?: temp
                val desc = res.weather?.firstOrNull()?.description ?: "clear skies"
                val humidity = res.main?.humidity ?: 0
                val cityName = res.name ?: city
                "Sir, $cityName mein abhi $temp°C temperature hai aur $desc chal raha hai. " +
                    "Feels like $feels°C with $humidity% humidity."
            } else {
                val meteo = weatherApi.getOpenMeteoWeather()
                val current = meteo.current
                val temp = current?.temperature?.roundToInt() ?: 28
                val humidity = current?.humidity ?: 60
                val wind = current?.windSpeed?.roundToInt() ?: 12
                "Sir, current telemetry ke hisaab se temperature $temp°C hai, " +
                    "humidity $humidity% aur hawa $wind km/h ki speed se chal rahi hai."
            }
        }
    }
}
