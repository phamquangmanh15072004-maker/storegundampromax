package com.example.storepromax.data.repository

import com.example.storepromax.domain.model.UserReview
import kotlinx.coroutines.delay
interface ReviewRepository {
    suspend fun getReviews(productId: String): List<UserReview>
    suspend fun submitRating(productId: String, rating: Int): Boolean
    suspend fun submitComment(productId: String, content: String, parentId: String?, rating: Int?): UserReview
}
class FakeReviewRepository : ReviewRepository {

    companion object {
        private val MEMORY_DATABASE = mutableListOf(
            UserReview("1","sp1", "u1", "Phạm Hùng", "", 5, "Mô hình đẹp, khớp nối chắc chắn!", System.currentTimeMillis()),
            UserReview("2","sp2","u2", "Minh Tuấn", "", 4, "Giao hàng hơi chậm xíu.", System.currentTimeMillis())
        )
    }

    override suspend fun getReviews(productId: String): List<UserReview> {
        delay(500)
        android.util.Log.d("DEBUG_REVIEW", "Đang tìm review cho ID: $productId")
        val productReviews = MEMORY_DATABASE.filter { it.productId == productId }
        android.util.Log.d("DEBUG_REVIEW", "Tìm thấy: ${productReviews.size} kết quả")
        return productReviews.filter { it.parentId == null }.map { parent ->
            parent.copy(replies = productReviews.filter { it.parentId == parent.id })
        }
    }

    override suspend fun submitRating(productId: String, rating: Int): Boolean {
        delay(300)
        val existingIndex = MEMORY_DATABASE.indexOfFirst {
            it.userId == "me" && it.productId == productId && it.rating != null
        }

        if (existingIndex != -1) {
            val old = MEMORY_DATABASE[existingIndex]
            MEMORY_DATABASE[existingIndex] = old.copy(rating = rating)
        }else {
            val newReview = UserReview(
                id = System.currentTimeMillis().toString(),
                productId = productId,
                userId = "me",
                userName = "Tôi",
                avatarUrl = "",
                rating = rating,
                content = "",
                timestamp = System.currentTimeMillis(),
                parentId = null
            )
            MEMORY_DATABASE.add(newReview)
        }
        return true
    }
    override suspend fun submitComment(productId: String, content: String, parentId: String?, rating: Int?): UserReview {
        delay(300)
        val newReview = UserReview(
            id = System.currentTimeMillis().toString(),
            productId = productId,
            userId = "me",
            userName = "Tôi (Sinh viên)",
            avatarUrl = "",
            rating = if (parentId == null) rating else null,
            content = content,
            timestamp = System.currentTimeMillis(),
            parentId = parentId
        )
        MEMORY_DATABASE.add(newReview)
        return newReview
    }
    fun deleteReview(reviewId: String) {
        MEMORY_DATABASE.removeAll { it.id == reviewId }
        MEMORY_DATABASE.removeAll { it.parentId == reviewId }
    }

    fun updateReview(reviewId: String, newContent: String) {
        val index = MEMORY_DATABASE.indexOfFirst { it.id == reviewId }
        if (index != -1) {
            val old = MEMORY_DATABASE[index]
            MEMORY_DATABASE[index] = old.copy(content = newContent)
        }
    }
}