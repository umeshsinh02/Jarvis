package com.jarvis.assistant.domain.model

enum class AssistantStatus(val displayText: String) {
    IDLE("Standby — Tap Orb or say "Hey JARVIS""),
    LISTENING("Listening..."),
    THINKING("Thinking..."),
    SPEAKING("Speaking...")
}

data class JarvisUiState(
    val status: AssistantStatus = AssistantStatus.IDLE,
    val messages: List<ChatMessage> = emptyList(),
    val partialTranscript: String = "",
    val streamingReply: String = "",
    val audioAmplitude: Float = 0f,
    val isContinuousMode: Boolean = false,
    val isWakeWordActive: Boolean = false,
    val errorMessage: String? = null
)
