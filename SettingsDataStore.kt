package com.jarvis.assistant.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jarvis.assistant.data.remote.PromptConfig
import com.jarvis.assistant.domain.model.HudThemeStyle
import com.jarvis.assistant.domain.model.JarvisVoiceEngine
import com.jarvis.assistant.domain.model.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.jarvisDataStore: DataStore<Preferences> by preferencesDataStore(name = "jarvis_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val PICOVOICE_KEY = stringPreferencesKey("picovoice_access_key")
        val WEATHER_API_KEY = stringPreferencesKey("weather_api_key")
        val SYSTEM_PROMPT = stringPreferencesKey("gemini_system_prompt")
        val VOICE_ENGINE = stringPreferencesKey("voice_engine")
        val HUD_CHIMES_ENABLED = booleanPreferencesKey("hud_chimes_enabled")
        val VOICE_SPEED = floatPreferencesKey("voice_speed")
        val VOICE_PITCH = floatPreferencesKey("voice_pitch")
        val CONTINUOUS_LISTENING = booleanPreferencesKey("continuous_listening")
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val THEME_STYLE = stringPreferencesKey("hud_theme_style")
    }

    val settingsFlow: Flow<UserSettings> = context.jarvisDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val themeName = prefs[Keys.THEME_STYLE] ?: HudThemeStyle.ARC_CYAN.name
            val themeStyle = runCatching { HudThemeStyle.valueOf(themeName) }
                .getOrDefault(HudThemeStyle.ARC_CYAN)

            val engineName = prefs[Keys.VOICE_ENGINE] ?: JarvisVoiceEngine.CINEMATIC_NEURAL.name
            val voiceEngine = runCatching { JarvisVoiceEngine.valueOf(engineName) }
                .getOrDefault(JarvisVoiceEngine.CINEMATIC_NEURAL)

            UserSettings(
                geminiApiKey = prefs[Keys.GEMINI_API_KEY].orEmpty(),
                picovoiceAccessKey = prefs[Keys.PICOVOICE_KEY].orEmpty(),
                weatherApiKey = prefs[Keys.WEATHER_API_KEY].orEmpty(),
                systemPrompt = prefs[Keys.SYSTEM_PROMPT]?.takeIf { it.isNotBlank() }
                    ?: PromptConfig.DEFAULT_SYSTEM_PROMPT,
                voiceEngine = voiceEngine,
                hudChimesEnabled = prefs[Keys.HUD_CHIMES_ENABLED] ?: true,
                voiceSpeed = prefs[Keys.VOICE_SPEED] ?: 1.0f,
                voicePitch = prefs[Keys.VOICE_PITCH] ?: 0.86f,
                continuousListening = prefs[Keys.CONTINUOUS_LISTENING] ?: false,
                wakeWordEnabled = prefs[Keys.WAKE_WORD_ENABLED] ?: false,
                themeStyle = themeStyle
            )
        }

    suspend fun updateGeminiApiKey(apiKey: String) {
        context.jarvisDataStore.edit { it[Keys.GEMINI_API_KEY] = apiKey.trim() }
    }

    suspend fun updatePicovoiceKey(key: String) {
        context.jarvisDataStore.edit { it[Keys.PICOVOICE_KEY] = key.trim() }
    }

    suspend fun updateWeatherApiKey(key: String) {
        context.jarvisDataStore.edit { it[Keys.WEATHER_API_KEY] = key.trim() }
    }

    suspend fun updateSystemPrompt(prompt: String) {
        context.jarvisDataStore.edit {
            it[Keys.SYSTEM_PROMPT] = prompt.trim().ifBlank { PromptConfig.DEFAULT_SYSTEM_PROMPT }
        }
    }

    suspend fun resetSystemPrompt() {
        context.jarvisDataStore.edit {
            it[Keys.SYSTEM_PROMPT] = PromptConfig.DEFAULT_SYSTEM_PROMPT
        }
    }

    suspend fun updateVoiceSpeed(speed: Float) {
        context.jarvisDataStore.edit { it[Keys.VOICE_SPEED] = speed.coerceIn(0.6f, 1.8f) }
    }

    suspend fun updateVoicePitch(pitch: Float) {
        context.jarvisDataStore.edit { it[Keys.VOICE_PITCH] = pitch.coerceIn(0.6f, 1.4f) }
    }

    suspend fun setContinuousListening(enabled: Boolean) {
        context.jarvisDataStore.edit { it[Keys.CONTINUOUS_LISTENING] = enabled }
    }

    suspend fun setWakeWordEnabled(enabled: Boolean) {
        context.jarvisDataStore.edit { it[Keys.WAKE_WORD_ENABLED] = enabled }
    }

    suspend fun setVoiceEngine(engine: JarvisVoiceEngine) {
        context.jarvisDataStore.edit { it[Keys.VOICE_ENGINE] = engine.name }
    }

    suspend fun setHudChimesEnabled(enabled: Boolean) {
        context.jarvisDataStore.edit { it[Keys.HUD_CHIMES_ENABLED] = enabled }
    }

    suspend fun setThemeStyle(style: HudThemeStyle) {
        context.jarvisDataStore.edit { it[Keys.THEME_STYLE] = style.name }
    }
}
