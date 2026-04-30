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
        rating: Int,
        mediaUrls: List<String>,
        orderId: String?
    ) {
        val currentUser = auth.currentUser ?: throw Exception("Bạn cần đăng nhập để bình luận!")
        val userId = currentUser.uid
        if (orderId != null && parentId == null) {
            val orderRef = firestore.collection("orders").document(orderId)
            val orderSnap = orderRef.get().await()
            if (!orderSnap.exists()) throw Exception("Không tìm thấy thông tin đơn hàng!")
            val status = orderSnap.getString("status") ?: ""
            if (status != "COMPLETED" && status != "RETURN_REJECTED") {
                throw Exception("Đơn hàng đang xử lý khiếu nại hoặc đã hoàn tiền. Bạn không thể đánh giá!")
            }
            val reviewedProducts = orderSnap.get("reviewedProducts") as? List<String> ?: emptyList()
            if (reviewedProducts.contains(productId)) {
                throw Exception("Bạn đã đánh giá sản phẩm này rồi!")
            }
            val newReviewedList = reviewedProducts + productId
            orderRef.update("reviewedProducts", newReviewedList).await()
        }
        val reviewRef = firestore.collection("products").document(productId)
            .collection("reviews").document()

        val newReview = hashMapOf<String, Any>(
            "id" to reviewRef.id,
            "productId" to productId,
            "userId" to userId,
            "userName" to (currentUser.displayName ?: "Anonymous"),
            "avatarUrl" to (currentUser.photoUrl?.toString() ?: ""),
            "content" to content,
            "timestamp" to System.currentTimeMillis(),
            "rating" to rating,
            "mediaUrls" to mediaUrls
        )

        if (parentId != null) {
            newReview["parentId"] = parentId
        }

        if (orderId != null) {
            newReview["orderId"] = orderId
        }

        reviewRef.set(newReview).await()

        if (rating > 0) {
            updateProductAverageRating(productId)
        }
    }

    override suspend fun submitRating(productId: String, rating: Int) {
        val currentUser = auth.currentUser ?: throw Exception("Chưa đăng nhập!")
        val userId = currentUser.uid

        val reviewRef = firestore.collection("products").document(productId)
            .collection("reviews").document()

        firestore.runTransaction { transaction ->
            val newReview = hashMapOf<String, Any>(
                "id" to reviewRef.id,
                "productId" to productId,
                "userId" to userId,
                "userName" to (currentUser.displayName ?: "Anonymous"),
                "avatarUrl" to (currentUser.photoUrl?.toString() ?: ""),
                "rating" to rating,
                "content" to "",
                "timestamp" to System.currentTimeMillis(),
                "mediaUrls" to emptyList<String>()
            )
            transaction.set(reviewRef, newReview)
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
                Math.round(allRatings.average() * 10.0) / 10.0
            } else {
                0.0
            }
            firestore.collection("products").document(productId)
                .update(
                    mapOf("rating" to avgRating)
                ).await()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}