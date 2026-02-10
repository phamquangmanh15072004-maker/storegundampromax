package com.example.storepromax.domain.repository

import com.example.storepromax.domain.model.ChatChannel
import com.example.storepromax.domain.model.ChatMessage
import com.example.storepromax.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun getOrCreateSupportChannel(): Result<String>
    fun getSupportChannels(): Flow<List<ChatChannel>>
    suspend fun updateChannelStatus(channelId: String, newStatus: String): Result<Boolean>
    fun getUserChannels(): Flow<List<ChatChannel>>
    suspend fun createTradeChannel(sellerId: String, product: Product): Result<String>
    fun getMessages(channelId: String): Flow<List<ChatMessage>>
    suspend fun sendMessage(
        channelId: String,
        content: String,
        type: String = "TEXT",
        mediaUrl: String = "",
        isAdmin: Boolean
    ): Result<Boolean>

    suspend fun getOrCreateUserChat(targetUserId: String, targetUserName: String, initialContent: String): Result<String>
}