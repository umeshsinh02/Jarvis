package com.jarvis.assistant.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.assistant.data.local.SettingsDataStore
import com.jarvis.assistant.domain.model.AssistantStatus
import com.jarvis.assistant.domain.model.JarvisUiState
import com.jarvis.assistant.domain.model.UserSettings
import com.jarvis.assistant.domain.repository.ChatRepository
import com.jarvis.assistant.domain.usecase.CommandOutcome
import com.jarvis.assistant.domain.usecase.ProcessVoiceCommandUseCase
import com.jarvis.assistant.domain.usecase.SendMessageUseCase
import com.jarvis.assistant.service.VoiceService
import com.jarvis.assistant.util.AudioHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JarvisViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatRepository: ChatRepository,
    private val settingsDataStore: SettingsDataStore,
    private val processVoiceCommand: ProcessVoiceCommandUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val audioHelper: AudioHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(JarvisUiState())
    val uiState: StateFlow<JarvisUiState> = _uiState.asStateFlow()

    private val _screenshotRequestFlow = MutableSharedFlow<Unit>()
    val screenshotRequestFlow: SharedFlow<Unit> = _screenshotRequestFlow.asSharedFlow()

    private var currentSettings = UserSettings()
    private var activeReplyJob: Job? = null

    init {
        observeMessages()
        observeSettings()
        observeAudioState()

        audioHelper.setOnAllSpeechDoneListener {
            _uiState.update { state ->
                if (state.status == AssistantStatus.SPEAKING) {
                    state.copy(status = AssistantStatus.IDLE)
                } else {
                    state
                }
            }
            if (currentSettings.continuousListening) {
                startListening()
            }
        }
    }

    private fun observeMessages() {
        viewModelScope.launch {
            chatRepository.observeMessages().collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsDataStore.settingsFlow.collect { settings ->
                currentSettings = settings
                audioHelper.updateVoiceParams(settings.voiceSpeed, settings.voicePitch)
                _uiState.update {
                    it.copy(
                        isContinuousMode = settings.continuousListening,
                        isWakeWordActive = settings.wakeWordEnabled
                    )
                }
            }
        }
    }

    private fun observeAudioState() {
        viewModelScope.launch {
            audioHelper.amplitudeFlow.collect { amp ->
                _uiState.update { it.copy(audioAmplitude = amp) }
            }
        }
        viewModelScope.launch {
            audioHelper.isSpeakingFlow.collect { speaking ->
                if (speaking) {
                    _uiState.update { it.copy(status = AssistantStatus.SPEAKING) }
                }
            }
        }
    }

    fun onOrbTapped() {
        when (_uiState.value.status) {
            AssistantStatus.LISTENING -> stopListening()
            AssistantStatus.SPEAKING -> {
                audioHelper.stopSpeaking()
                _uiState.update { it.copy(status = AssistantStatus.IDLE) }
            }
            AssistantStatus.THINKING -> {
                activeReplyJob?.cancel()
                _uiState.update { it.copy(status = AssistantStatus.IDLE, streamingReply = "") }
            }
            AssistantStatus.IDLE -> startListening()
        }
    }

    fun startListening() {
        if (currentSettings.hudChimesEnabled) {
            audioHelper.playHudChime(isListeningCue = true)
        }
        _uiState.update {
            it.copy(status = AssistantStatus.LISTENING, partialTranscript = "", errorMessage = null)
        }
        audioHelper.startListening(
            onPartial = { partial ->
                _uiState.update { it.copy(partialTranscript = partial) }
            },
            onFinal = { finalTranscript ->
                _uiState.update { it.copy(partialTranscript = "") }
                submitUserPrompt(finalTranscript)
            },
            onError = { err ->
                _uiState.update {
                    it.copy(
                        status = AssistantStatus.IDLE,
                        partialTranscript = "",
                        errorMessage = err
                    )
                }
            }
        )
    }

    fun stopListening() {
        audioHelper.stopListening()
        _uiState.update { it.copy(status = AssistantStatus.IDLE, partialTranscript = "") }
    }

    fun toggleContinuousListening(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setContinuousListening(enabled)
            val serviceIntent = Intent(context, VoiceService::class.java)
            if (enabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                startListening()
            } else {
                serviceIntent.action = VoiceService.ACTION_STOP
                context.startService(serviceIntent)
                stopListening()
            }
        }
    }

    fun submitUserPrompt(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty()) return

        activeReplyJob?.cancel()
        activeReplyJob = viewModelScope.launch {
            _uiState.update {
                it.copy(status = AssistantStatus.THINKING, streamingReply = "", errorMessage = null)
            }
            sendMessageUseCase.persistUserMessage(text)

            when (val outcome = processVoiceCommand(text, currentSettings.weatherApiKey)) {
                is CommandOutcome.HandledLocally -> {
                    sendMessageUseCase.persistJarvisMessage(
                        text = outcome.responseText,
                        isCommandExecution = true,
                        commandType = outcome.commandLabel
                    )
                    _uiState.update { it.copy(status = AssistantStatus.SPEAKING) }
                    audioHelper.speakSentence(
                        sentence = outcome.responseText,
                        flushQueue = true,
                        apiKey = currentSettings.geminiApiKey,
                        voiceEngine = currentSettings.voiceEngine
                    )
                }

                is CommandOutcome.RequestScreenshotPermission -> {
                    _screenshotRequestFlow.emit(Unit)
                }

                is CommandOutcome.DelegateToGemini -> {
                    streamFromGeminiAndSpeak(text)
                }
            }
        }
    }

    /**
     * Collects Gemini 1.5 Flash token stream, buffers into complete sentences,
     * and dispatches each sentence immediately to British TTS for zero-lag voice output.
     */
    private suspend fun streamFromGeminiAndSpeak(prompt: String) {
        val fullBuffer = StringBuilder()
        val sentenceBuffer = StringBuilder()
        var isFirstSentence = true
        var hadError = false

        sendMessageUseCase.streamAiReply(
            apiKey = currentSettings.geminiApiKey,
            systemPrompt = currentSettings.systemPrompt,
            userPrompt = prompt
        ).collect { result ->
            result.onSuccess { chunk ->
                fullBuffer.append(chunk)
                sentenceBuffer.append(chunk)
                _uiState.update { it.copy(streamingReply = fullBuffer.toString()) }

                // Flush completed sentences to TTS immediately
                val buffered = sentenceBuffer.toString()
                val boundaryIdx = buffered.lastIndexOfAny(charArrayOf('.', '!', '?', '\n'))
                if (boundaryIdx >= 0) {
                    val completeSentence = buffered.substring(0, boundaryIdx + 1).trim()
                    val remainder = buffered.substring(boundaryIdx + 1)
                    sentenceBuffer.clear()
                    sentenceBuffer.append(remainder)

                    if (completeSentence.isNotEmpty()) {
                        audioHelper.speakSentence(
                            sentence = completeSentence,
                            flushQueue = isFirstSentence,
                            apiKey = currentSettings.geminiApiKey,
                            voiceEngine = currentSettings.voiceEngine
                        )
                        isFirstSentence = false
                    }
                }
            }.onFailure { err ->
                hadError = true
                val errText = err.message ?: "Gemini connection error, Sir."
                _uiState.update {
                    it.copy(
                        status = AssistantStatus.IDLE,
                        streamingReply = "",
                        errorMessage = errText
                    )
                }
                audioHelper.speakSentence(
                    sentence = errText,
                    flushQueue = true,
                    apiKey = currentSettings.geminiApiKey,
                    voiceEngine = currentSettings.voiceEngine
                )
            }
        }

        if (!hadError) {
            val remaining = sentenceBuffer.toString().trim()
            if (remaining.isNotEmpty()) {
                audioHelper.speakSentence(
                    sentence = remaining,
                    flushQueue = isFirstSentence,
                    apiKey = currentSettings.geminiApiKey,
                    voiceEngine = currentSettings.voiceEngine
                )
            }
            val finalReply = fullBuffer.toString().trim()
            if (finalReply.isNotEmpty()) {
                sendMessageUseCase.persistJarvisMessage(finalReply)
            }
            _uiState.update { it.copy(streamingReply = "") }
        }
    }

    fun onScreenshotStarted() {
        viewModelScope.launch {
            val msg = "Screen capture saved to Pictures/JARVIS, Sir."
            sendMessageUseCase.persistJarvisMessage(msg, isCommandExecution = true, commandType = "SCREENSHOT")
            audioHelper.speakSentence(
                sentence = msg,
                flushQueue = true,
                apiKey = currentSettings.geminiApiKey,
                voiceEngine = currentSettings.voiceEngine
            )
        }
    }

    fun onScreenshotDenied() {
        viewModelScope.launch {
            val msg = "Screenshot permission cancel kar di gayi hai, Sir."
            sendMessageUseCase.persistJarvisMessage(msg, isCommandExecution = true, commandType = "SCREENSHOT")
            _uiState.update { it.copy(status = AssistantStatus.IDLE) }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        audioHelper.release()
    }
}
