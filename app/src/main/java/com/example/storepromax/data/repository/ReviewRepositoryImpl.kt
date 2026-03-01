package com.example.storepromax.data.repository

import com.example.storepromax.domain.model.UserReview
import com.example.storepromax.domain.repository.ReviewRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ReviewRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ReviewRepository {

    override suspend fun getReviews(productId: String): List<UserReview> {
        return try {
            val snapshot = firestore.collection("products").document(productId)
                .collection("reviews")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            val allReviews = snapshot.documents.mapNotNull { doc ->
                doc.toObject(UserReview::class.java)?.copy(id = doc.id)
            }
            val topLevelComments = allReviews.filter { it.parentId == null }
            val replies = allReviews.filter { it.parentId != null }
            topLevelComments.map { parent ->
                val parentReplies = replies
                    .filter { it.parentId == parent.id }
                    .sortedBy { it.timestamp }

                parent.copy(replies = parentReplies)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun submitComment(
        productId: String,
        content: String,
        parentId: String?,
        rating: Int?
    ) {
        val currentUser = auth.currentUser ?: throw Exception("Bạn cần đăng nhập để bình luận!")
        val userId = currentUser.uid
        val reviewRef = firestore.collection("products").document(productId)
            .collection("reviews").document()

        val newReview = mutableMapOf<String, Any?>()
        newReview["id"] = reviewRef.id
        newReview["productId"] = productId
        newReview["userId"] = userId
        newReview["userName"] = currentUser.displayName ?: "Anonymous"
        newReview["avatarUrl"] = currentUser.photoUrl?.toString() ?: ""
        newReview["content"] = content
        newReview["timestamp"] = System.currentTimeMillis() // 🔥 Đồng nhất dùng kiểu Long
        newReview["parentId"] = parentId
        newReview["rating"] = rating ?: 0

        reviewRef.set(newReview).await()
        updateProductAverageRating(productId)
    }

    override suspend fun submitRating(productId: String, rating: Int) {
        val currentUser = auth.currentUser ?: throw Exception("Chưa đăng nhập!")
        val userId = currentUser.uid

        val reviewRef = firestore.collection("products").document(productId)
            .collection("reviews").document(userId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(reviewRef)
            if (snapshot.exists()) {
                transaction.update(reviewRef, "rating", rating)
            } else {
                val newReview = hashMapOf(
                    "userId" to userId,
                    "userName" to (currentUser.displayName ?: "Anonymous"),
                    "avatarUrl" to (currentUser.photoUrl?.toString() ?: ""),
                    "rating" to rating,
                    "content" to "",
                    "timestamp" to System.currentTimeMillis() // 🔥 Đổi Date() thành Long
                )
                transaction.set(reviewRef, newReview)
            }
        }.await()
        updateProductAverageRating(productId)
    }

    override suspend fun deleteReview(productId: String, reviewId: String) {
        firestore.collection("products").document(productId)
            .collection("reviews").document(reviewId)
            .delete()
            .await()
        updateProductAverageRating(productId)
    }

    override suspend fun updateReview(productId: String, reviewId: String, newContent: String) {
        firestore.collection("products").document(productId)
            .collection("reviews").document(reviewId)
            .update("content", newContent)
            .await()
        updateProductAverageRating(productId)
    }
    private suspend fun updateProductAverageRating(productId: String) {
        try {
            val snapshot = firestore.collection("products").document(productId)
                .collection("reviews")
                .get()
                .await()

            val allRatings = snapshot.documents.mapNotNull {
                it.getLong("rating")?.toInt()
            }.filter { it > 0 }

            val avgRating = if (allRatings.isNotEmpty()) {
                allRatings.average()
            } else {
                0.0
            }
            firestore.collection("products").document(productId)
                .update(
                    mapOf(
                        "rating" to avgRating,
                    )
                ).await()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}