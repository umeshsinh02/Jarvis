package com.jarvis.assistant.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun observeAllMessages(): Flow<List<ChatMessageEntity>>

    /**
     * Fetches the most recent 10 messages in descending order;
     * repository reverses them into chronological order for Gemini context memory.
     */
    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT 10")
    suspend fun getLastTenMessagesDesc(): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()
}
