package com.example.storepromax.domain.model

data class FcmRequest(
    val targetToken: String? = null,
    val topic: String? = null,
    val title: String,
    val body: String,
    val type: String,
    val orderId: String? = null,
    val action: String? = null,
    val postId: String? = null,
    val channelId: String? = null
)