package com.example.storepromax.domain.model

data class Post(
    val id: String = "",

    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "",

    val title: String = "",
    val content: String = "",
    val price: Long = 0,
    val images: List<String> = emptyList(),

    val condition: String = "USED",
    val grade: String = "HG",

    val likeCount: Int = 0,
    val commentCount: Int = 0,

    val status: String = "PENDING",

    val rejectionReason: String? = null,
    val searchKeywords: List<String> = emptyList(),

    val createdAt: Long = System.currentTimeMillis(),
    val processedAt: Long = 0,
    val likedByUsers: List<String> = emptyList()
)