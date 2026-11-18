package com.jarvis.assistant.domain.model

enum class MessageSender {
    USER,
    JARVIS
}

data class ChatMessage(
    val id: Long = 0L,
    val sender: MessageSender,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isCommandExecution: Boolean = false,
    val commandType: String? = null
)
