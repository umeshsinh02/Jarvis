package com.jarvis.assistant.data.remote

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.jarvis.assistant.domain.model.ChatMessage
import com.jarvis.assistant.domain.model.MessageSender
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiClient @Inject constructor() {

    companion object {
        private const val MODEL_NAME = "gemini-1.5-flash"
    }

    /**
     * Streams Gemini responses chunk-by-chunk using the active systemPrompt
     * and the last 10 messages as conversation memory.
     */
    fun streamReply(
        apiKey: String,
        systemPrompt: String,
        userMessage: String,
        history: List<ChatMessage>
    ): Flow<Result<String>> = flow {
        if (apiKey.isBlank()) {
            emit(
                Result.failure(
                    IllegalStateException("Gemini API key missing hai, Sir. Settings mein jaake key add kijiye.")
                )
            )
            return@flow
        }

        val effectivePrompt = systemPrompt.trim().ifBlank { PromptConfig.DEFAULT_SYSTEM_PROMPT }

        val model = GenerativeModel(
            modelName = MODEL_NAME,
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.75f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 320
            },
            systemInstruction = content { text(effectivePrompt) }
        )

        // Convert the last 10 persisted messages into Gemini Content history
        val chatHistory: List<Content> = history
            .takeLast(10)
            .map { msg ->
                val role = if (msg.sender == MessageSender.USER) "user" else "model"
                content(role = role) { text(msg.content) }
            }

        val chat = model.startChat(history = chatHistory)
        chat.sendMessageStream(userMessage).collect { response ->
            val chunk = response.text
            if (!chunk.isNullOrEmpty()) {
                emit(Result.success(chunk))
            }
        }
    }.catch { throwable ->
        val friendlyMessage = when {
            throwable.message?.contains("API_KEY_INVALID", ignoreCase = true) == true ->
                "Sir, Gemini API key invalid lag rahi hai. Settings mein check kijiye."
            throwable.message?.contains("QUOTA", ignoreCase = true) == true ||
                throwable.message?.contains("429") == true ->
                "API quota temporarily exhaust ho gaya hai, Boss. Thodi der baad try karein."
            throwable is java.net.UnknownHostException ->
                "Internet connection offline hai, Sir. Network check kijiye."
            else ->
                "Neural link mein glitch aaya hai: ${throwable.localizedMessage ?: "Unknown error"}"
        }
        emit(Result.failure(Exception(friendlyMessage, throwable)))
    }
}
