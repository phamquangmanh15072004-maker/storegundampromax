package com.example.storepromax.domain.model

data class Comment(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "",
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val parentId: String = "",
    val replyingToName: String = ""
)