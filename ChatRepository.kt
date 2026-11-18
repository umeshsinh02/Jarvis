package com.jarvis.assistant.domain.repository

import com.jarvis.assistant.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeMessages(): Flow<List<ChatMessage>>
    suspend fun getRecentContext(): List<ChatMessage>
    suspend fun saveMessage(message: ChatMessage): Long
    suspend fun clearHistory()
    fun streamGeminiReply(
        apiKey: String,
        systemPrompt: String,
        userPrompt: String,
        history: List<ChatMessage>
    ): Flow<Result<String>>
}
