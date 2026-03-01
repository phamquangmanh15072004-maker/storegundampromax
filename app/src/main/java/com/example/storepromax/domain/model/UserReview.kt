package com.example.storepromax.domain.model

import com.google.firebase.firestore.Exclude

data class UserReview(
    var id: String = "",
    val productId: String = "",
    val userId: String = "",
    val userName: String = "",
    val avatarUrl: String = "",
    val rating: Int = 0,
    val content: String = "",
    val timestamp: Long = 0L,
    val parentId: String? = null,
    @get:Exclude
    var replies: List<UserReview> = emptyList()
)