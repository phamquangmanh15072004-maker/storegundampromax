package com.example.storepromax.domain.model

data class UserReview(
    val id: String,
    val productId: String,
    val userId: String,
    val userName: String,
    val avatarUrl: String,
    val rating: Int?,
    val content: String,
    val timestamp: Long,
    val parentId: String? = null,
    val replies: List<UserReview> = emptyList()
)