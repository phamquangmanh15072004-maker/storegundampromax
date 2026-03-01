package com.example.storepromax.domain.repository

import com.example.storepromax.domain.model.UserReview

interface ReviewRepository {
    suspend fun getReviews(productId: String): List<UserReview>
    suspend fun submitRating(productId: String, rating: Int)
    suspend fun submitComment(productId: String, content: String, parentId: String?, rating: Int?)
    suspend fun deleteReview(productId: String, reviewId: String)
    suspend fun updateReview(productId: String, reviewId: String, newContent: String)
}
