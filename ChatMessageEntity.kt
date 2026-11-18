package com.jarvis.assistant.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jarvis.assistant.domain.model.ChatMessage
import com.jarvis.assistant.domain.model.MessageSender

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val sender: String,
    val content: String,
    val timestamp: Long,
    val isCommandExecution: Boolean = false,
    val commandType: String? = null
) {
    fun toDomain(): ChatMessage = ChatMessage(
        id = id,
        sender = if (sender == MessageSender.USER.name) MessageSender.USER else MessageSender.JARVIS,
        content = content,
        timestamp = timestamp,
        isCommandExecution = isCommandExecution,
        commandType = commandType
    )

    companion object {
        fun fromDomain(message: ChatMessage): ChatMessageEntity = ChatMessageEntity(
            id = message.id,
            sender = message.sender.name,
            content = message.content,
            timestamp = message.timestamp,
            isCommandExecution = message.isCommandExecution,
            commandType = message.commandType
        )
    }
}
