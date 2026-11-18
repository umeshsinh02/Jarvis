package com.jarvis.assistant.domain.usecase

import com.jarvis.assistant.domain.model.ChatMessage
import com.jarvis.assistant.domain.model.MessageSender
import com.jarvis.assistant.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend fun persistUserMessage(text: String) {
        chatRepository.saveMessage(
            ChatMessage(sender = MessageSender.USER, content = text)
        )
    }

    suspend fun persistJarvisMessage(
        text: String,
        isCommandExecution: Boolean = false,
        commandType: String? = null
    ) {
        chatRepository.saveMessage(
            ChatMessage(
                sender = MessageSender.JARVIS,
                content = text,
                isCommandExecution = isCommandExecution,
                commandType = commandType
            )
        )
    }

    suspend fun streamAiReply(
        apiKey: String,
        systemPrompt: String,
        userPrompt: String
    ): Flow<Result<String>> {
        val recentHistory = chatRepository.getRecentContext()
        return chatRepository.streamGeminiReply(
            apiKey = apiKey,
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            history = recentHistory
        )
    }
}
