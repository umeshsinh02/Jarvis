package com.jarvis.assistant.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.assistant.data.local.SettingsDataStore
import com.jarvis.assistant.domain.model.HudThemeStyle
import com.jarvis.assistant.domain.model.JarvisVoiceEngine
import com.jarvis.assistant.domain.model.UserSettings
import com.jarvis.assistant.domain.repository.ChatRepository
import com.jarvis.assistant.service.WakeWordService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val chatRepository: ChatRepository
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    fun saveApiKeys(geminiKey: String, picovoiceKey: String, weatherKey: String) {
        viewModelScope.launch {
            settingsDataStore.updateGeminiApiKey(geminiKey)
            settingsDataStore.updatePicovoiceKey(picovoiceKey)
            settingsDataStore.updateWeatherApiKey(weatherKey)
        }
    }

    fun updateSystemPrompt(prompt: String) {
        viewModelScope.launch { settingsDataStore.updateSystemPrompt(prompt) }
    }

    fun resetSystemPrompt() {
        viewModelScope.launch { settingsDataStore.resetSystemPrompt() }
    }

    fun setVoiceEngine(engine: JarvisVoiceEngine) {
        viewModelScope.launch { settingsDataStore.setVoiceEngine(engine) }
    }

    fun toggleHudChimes(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setHudChimesEnabled(enabled) }
    }

    fun updateVoiceSpeed(speed: Float) {
        viewModelScope.launch { settingsDataStore.updateVoiceSpeed(speed) }
    }

    fun updateVoicePitch(pitch: Float) {
        viewModelScope.launch { settingsDataStore.updateVoicePitch(pitch) }
    }

    fun setThemeStyle(style: HudThemeStyle) {
        viewModelScope.launch { settingsDataStore.setThemeStyle(style) }
    }

    fun toggleWakeWord(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setWakeWordEnabled(enabled)
            val current = settingsDataStore.settingsFlow.first()
            val intent = Intent(context, WakeWordService::class.java)
            if (enabled && current.picovoiceAccessKey.isNotBlank()) {
                intent.putExtra(WakeWordService.EXTRA_ACCESS_KEY, current.picovoiceAccessKey)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } else {
                intent.action = WakeWordService.ACTION_STOP
                context.startService(intent)
            }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch { chatRepository.clearHistory() }
    }
}
