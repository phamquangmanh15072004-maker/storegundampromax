package com.example.storepromax.domain.model

import java.util.UUID

data class ChatMessageAI(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val attachedProducts: List<Product> = emptyList(),
    val cartItems: List<Product> = emptyList(),
    val hasGoToCartButton: Boolean = false,
    val userImageUrl: String? = null,
    val attachedPosts: List<Post> = emptyList(),
    @Transient val localBitmap: android.graphics.Bitmap? = null
)