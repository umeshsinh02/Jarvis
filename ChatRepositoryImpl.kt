package com.jarvis.assistant.data.repository

import com.jarvis.assistant.data.local.ChatDao
import com.jarvis.assistant.data.local.ChatMessageEntity
import com.jarvis.assistant.data.remote.GeminiClient
import com.jarvis.assistant.domain.model.ChatMessage
import com.jarvis.assistant.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatDao: ChatDao,
    private val geminiClient: GeminiClient
) : ChatRepository {

    override fun observeMessages(): Flow<List<ChatMessage>> =
        chatDao.observeAllMessages().map { list -> list.map { it.toDomain() } }

    override suspend fun getRecentContext(): List<ChatMessage> =
        chatDao.getLastTenMessagesDesc().reversed().map { it.toDomain() }

    override suspend fun saveMessage(message: ChatMessage): Long =
        chatDao.insertMessage(ChatMessageEntity.fromDomain(message))

    override suspend fun clearHistory() {
        chatDao.clearAll()
    }

    override fun streamGeminiReply(
        apiKey: String,
        systemPrompt: String,
        userPrompt: String,
        history: List<ChatMessage>
    ): Flow<Result<String>> =
        geminiClient.streamReply(
            apiKey = apiKey,
            systemPrompt = systemPrompt,
            userMessage = userPrompt,
            history = history
        )
}
