package com.example.storepromax.domain.model

import com.google.firebase.firestore.PropertyName

data class ChatChannel(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "",

    val receiverId: String = "",
    val receiverName: String = "",

    val productId: String = "",
    val productName: String = "",
    val productImage: String = "",
    val lastMessage: String = "",
    val lastUpdated: Long = System.currentTimeMillis(),
    val status: String = "PENDING",
    val type: String = "SUPPORT",
    val blockedBy: List<String> = emptyList(),
    val receiverAvatar: String = "",
)
data class ChatMessage(
    val id: String = "",
    val channelId: String = "",
    val senderId: String = "",
    val content: String = "",
    val timestamp: Long = 0,
    val isAdmin: Boolean = false,
    val type: String = "TEXT",
    val mediaUrl: String = "",
    val replyToId: String? = null,
    val deletedBy: List<String> = emptyList()
)