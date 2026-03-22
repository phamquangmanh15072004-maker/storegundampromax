package com.example.storepromax.domain.model

data class ProductReview(
    val id: String,
    val userId: String,
    val userName: String,
    val rating: Int,
    val comment: String,
    val timestamp: Long
)